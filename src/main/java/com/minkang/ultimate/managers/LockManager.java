package com.minkang.ultimate.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LockManager implements Listener {

    public static class LockData {
        public UUID owner;
        public boolean permanent;
        public long expiresAt; // epoch millis (permanent=false일 때만 의미)
        public Set<UUID> allowed = new HashSet<UUID>();
        public org.bukkit.Location signLoc; // 서있는/벽 표지판 위치

        boolean isExpired() {
            return !permanent && System.currentTimeMillis() > expiresAt;
        }
    }

    private final JavaPlugin plugin;
    private final Map<String, LockData> locks = new HashMap<String, LockData>();
    private File file;
    private YamlConfiguration conf;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final NamespacedKey KEY_TEMP;
    private final NamespacedKey KEY_PERM;

    public LockManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.KEY_TEMP = new NamespacedKey(plugin, "lock_ticket_temp");
        this.KEY_PERM = new NamespacedKey(plugin, "lock_ticket_perm");
        load();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // 주기 태스크 없음: 접속/상호작용 시에만 만료 정리(서버 부담 0)
    }

    /* ================== PUBLIC API ================== */

    public boolean lockTimed(Player p, Block target, long durationMinutes) {
        if (!isContainer(target)) { p.sendMessage("§c상자를 보고 /잠금을 사용하세요."); return false; }
        String key = key(target);
        LockData ld = new LockData();
        ld.owner = p.getUniqueId();
        ld.permanent = false;
        ld.expiresAt = System.currentTimeMillis() + durationMinutes * 60_000L;
        locks.put(key, ld);
        placeOrUpdateSign(target, ld, p, p.getName());
        save();
        p.sendMessage("§a잠금됨: §f"+durationMinutes+"분 동안");
        return true;
    }

    public boolean lockPermanent(Player p, Block target) {
        if (!isContainer(target)) { p.sendMessage("§c상자를 보고 /잠금을 사용하세요."); return false; }
        String key = key(target);
        LockData ld = new LockData();
        ld.owner = p.getUniqueId();
        ld.permanent = true;
        ld.expiresAt = 0L;
        locks.put(key, ld);
        placeOrUpdateSign(target, ld, p, p.getName());
        save();
        p.sendMessage("§a잠금됨: §f영구");
        return true;
    }

    public boolean addAllowed(Player p, Block target, UUID who, String name) {
        LockData ld = get(target);
        if (ld == null) { p.sendMessage("§c해당 상자는 잠금 상태가 아닙니다."); return false; }
        if (!isOwner(p, ld)) { p.sendMessage("§c소유자만 추가할 수 있습니다."); return false; }
        boolean changed = ld.allowed.add(who);
        if (changed) {
            placeOrUpdateSign(target, ld, p, Bukkit.getOfflinePlayer(ld.owner).getName());
            save();
            p.sendMessage("§a추가됨: §f" + name);
        } else {
            p.sendMessage("§7이미 허용 목록에 있습니다: §f" + name);
        }
        return true;
    }

    public boolean removeAllowed(Player p, Block target, UUID who, String name) {
        LockData ld = get(target);
        if (ld == null) { p.sendMessage("§c해당 상자는 잠금 상태가 아닙니다."); return false; }
        if (!isOwner(p, ld)) { p.sendMessage("§c소유자만 제거할 수 있습니다."); return false; }
        boolean changed = ld.allowed.remove(who);
        if (changed) {
            placeOrUpdateSign(target, ld, p, Bukkit.getOfflinePlayer(ld.owner).getName());
            save();
            p.sendMessage("§7제거됨: §f" + name);
        } else {
            p.sendMessage("§7허용 목록에 없습니다: §f" + name);
        }
        return true;
    }

    public boolean unlock(Player p, Block target) {
        LockData ld = get(target);
        if (ld == null) { p.sendMessage("§7이미 잠금이 없습니다."); return false; }
        if (!isOwner(p, ld)) { p.sendMessage("§c소유자만 해제할 수 있습니다."); return false; }
        removeSign(ld);
        locks.remove(key(target));
        save();
        p.sendMessage("§a해제 완료");
        return true;
    }

    public void info(Player p, Block target) {
        LockData ld = get(target);
        if (ld == null) { p.sendMessage("§7잠금 없음"); return; }
        String owner = ownerName(ld.owner);
        p.sendMessage("§e[잠금 정보]");
        p.sendMessage("§7소유자: §f" + owner);
        if (ld.permanent) p.sendMessage("§7유형: §f영구");
        else p.sendMessage("§7만료: §f" + FMT.format(Instant.ofEpochMilli(ld.expiresAt)));
        if (!ld.allowed.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (UUID u : ld.allowed) {
                if (shown > 0) sb.append(", ");
                String n = ownerName(u);
                sb.append(n);
                shown++;
                if (shown >= 5) { // 채팅만 5명까지
                    int rest = ld.allowed.size() - shown;
                    if (rest > 0) sb.append(" +").append(rest);
                    break;
                }
            }
            p.sendMessage("§7허용: §f" + sb.toString());
        }
    }

    /* ================== 이벤트: 열기/인터랙트 차단 ================== */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        if (e.getInventory().getType() == InventoryType.CHEST || e.getInventory().getType() == InventoryType.BARREL) {
            if (e.getInventory().getLocation() == null) return;
            Block b = e.getInventory().getLocation().getBlock();
            LockData ld = get(b);
            if (ld == null) return;
            if (ld.isExpired()) { unlockSilently(b, ld); return; }
            if (!canOpen(((Player)e.getPlayer()).getUniqueId(), ld)) {
                ((Player)e.getPlayer()).sendMessage("§c잠금된 상자입니다.");
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null || !isContainer(b)) return;
        LockData ld = get(b);
        if (ld == null) return;
        if (ld.isExpired()) { unlockSilently(b, ld); return; }
        if (!canOpen(e.getPlayer().getUniqueId(), ld)) {
            e.getPlayer().sendMessage("§c잠금된 상자입니다.");
            e.setCancelled(true);
        }
    }

    /* ================== 내부 유틸 ================== */

    private boolean isContainer(Block b) {
        Material t = b.getType();
        return t == Material.CHEST || t == Material.TRAPPED_CHEST || t == Material.BARREL;
    }

    private String key(Block b) {
        org.bukkit.Location l = b.getLocation();
        return l.getWorld().getName()+","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
    }

    private LockData get(Block b) { return locks.get(key(b)); }

    private boolean isOwner(Player p, LockData ld) { return ld.owner.equals(p.getUniqueId()); }

    private boolean canOpen(UUID u, LockData ld) { return u.equals(ld.owner) || ld.allowed.contains(u); }

    private String ownerName(UUID u){
        OfflinePlayer op = Bukkit.getOfflinePlayer(u);
        return op!=null && op.getName()!=null ? op.getName() : u.toString().substring(0,8);
    }

    private void placeOrUpdateSign(Block chest, LockData ld, Player p, String ownerName) {
        Material t = chest.getType();
        Block frontBlock = null;
        org.bukkit.block.BlockFace front = null;

        if (t == Material.CHEST || t == Material.TRAPPED_CHEST) {
            Chest data = (Chest) chest.getBlockData();
            front = data.getFacing();
        } else if (t == Material.BARREL) {
            Directional d = (Directional) chest.getBlockData();
            org.bukkit.block.BlockFace f = d.getFacing();
            if (f == org.bukkit.block.BlockFace.UP || f == org.bukkit.block.BlockFace.DOWN) f = p.getFacing();
            front = f;
        } else {
            return;
        }

        frontBlock = chest.getRelative(front);

        // 이미 벽표지판이면 갱신
        if (frontBlock.getState() instanceof Sign) {
            Sign sign = (Sign) frontBlock.getState();
            writeSignLegacy(sign, ld, ownerName);
            sign.update(true, false);
            ld.signLoc = frontBlock.getLocation();
            return;
        }

        if (!frontBlock.getType().isAir()) return; // 막혀있으면 설치 안 함(정면 고정)

        // 새로 설치
        frontBlock.setType(Material.OAK_WALL_SIGN, false);
        org.bukkit.block.data.BlockData bd = frontBlock.getBlockData();
        if (bd instanceof WallSign) {
            WallSign ws = (WallSign) bd;
            ws.setFacing(front);
            frontBlock.setBlockData(ws, false);
            Sign sign = (Sign) frontBlock.getState();
            writeSignLegacy(sign, ld, ownerName);
            sign.update(true, false);
            ld.signLoc = frontBlock.getLocation();
        } else {
            frontBlock.setType(Material.AIR, false);
        }
    }

    private void writeSignLegacy(Sign sign, LockData ld, String ownerName) {
        sign.setLine(0, "§0[§2§l잠금§0]");
        sign.setLine(1, "§7소유자: §f" + (ownerName != null ? ownerName : "알수없음"));
        if (ld.permanent) {
            sign.setLine(2, "§b영구");
        } else {
            String when = FMT.format(Instant.ofEpochMilli(ld.expiresAt));
            sign.setLine(2, "§e만료: §f" + when);
        }
        // 허용 3명까지만
        if (!ld.allowed.isEmpty()) {
            int shown = 0;
            StringBuilder sb = new StringBuilder();
            for (UUID u : ld.allowed) {
                if (shown > 0) sb.append(", ");
                String n = ownerName(u);
                sb.append(n);
                shown++;
                if (shown >= 3) {
                    int rest = ld.allowed.size() - shown;
                    if (rest > 0) sb.append(" +").append(rest);
                    break;
                }
            }
            sign.setLine(3, "§7허용: §f" + sb.toString());
        } else {
            sign.setLine(3, "");
        }
    }

    private void removeSign(LockData ld) {
        if (ld.signLoc == null) return;
        Block b = ld.signLoc.getBlock();
        if (b.getState() instanceof Sign) {
            b.setType(Material.AIR, false);
        }
        ld.signLoc = null;
    }

    private void unlockSilently(Block b, LockData ld) {
        removeSign(ld);
        locks.remove(key(b));
        save();
    }

    /* ================== 저장/로드 ================== */

    private void load() {
        file = new File(plugin.getDataFolder(), "locks.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            conf = new YamlConfiguration();
            return;
        }
        conf = YamlConfiguration.loadConfiguration(file);
        for (String k : conf.getKeys(false)) {
            LockData ld = new LockData();
            ld.owner = UUID.fromString(conf.getString(k+".owner"));
            ld.permanent = conf.getBoolean(k+".permanent");
            ld.expiresAt = conf.getLong(k+".expiresAt");
            List<String> allow = conf.getStringList(k+".allowed");
            for (String s : allow) ld.allowed.add(UUID.fromString(s));
            if (conf.isConfigurationSection(k+".sign")) {
                World w = Bukkit.getWorld(conf.getString(k+".sign.world"));
                int x = conf.getInt(k+".sign.x"), y = conf.getInt(k+".sign.y"), z = conf.getInt(k+".sign.z");
                if (w != null) ld.signLoc = new org.bukkit.Location(w,x,y,z);
            }
            locks.put(k, ld);
        }
    }

    public void save() {
        conf = new YamlConfiguration();
        for (Map.Entry<String, LockData> e : locks.entrySet()) {
            String k = e.getKey();
            LockData ld = e.getValue();
            conf.set(k+".owner", ld.owner.toString());
            conf.set(k+".permanent", ld.permanent);
            conf.set(k+".expiresAt", ld.expiresAt);
            List<String> allow = new ArrayList<String>();
            for (UUID u : ld.allowed) allow.add(u.toString());
            conf.set(k+".allowed", allow);
            if (ld.signLoc != null) {
                conf.set(k+".sign.world", ld.signLoc.getWorld().getName());
                conf.set(k+".sign.x", ld.signLoc.getBlockX());
                conf.set(k+".sign.y", ld.signLoc.getBlockY());
                conf.set(k+".sign.z", ld.signLoc.getBlockZ());
            }
        }
        try { conf.save(file); } catch (IOException ignored) {}
    }

    /* ================== 티켓 판별/소모 ================== */

    public boolean consumeTempTicket(Player p) {
        ItemStack in = p.getInventory().getItemInMainHand();
        if (!isTicket(in, KEY_TEMP, "시간잠금권")) return false;
        decrement(in, p);
        return true;
    }
    public boolean consumePermTicket(Player p) {
        ItemStack in = p.getInventory().getItemInMainHand();
        if (!isTicket(in, KEY_PERM, "영구잠금권")) return false;
        decrement(in, p);
        return true;
    }

    private boolean isTicket(ItemStack it, NamespacedKey key, String nameContains) {
        if (it == null || it.getType().isAir()) return false;
        ItemMeta meta = it.getItemMeta(); if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte b = pdc.get(key, PersistentDataType.BYTE);
        if (b != null && b == (byte)1) return true;
        String dn = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : "";
        return dn.contains(nameContains);
    }

    private void decrement(ItemStack in, Player p) {
        if (in.getAmount() <= 1) p.getInventory().setItemInMainHand(null);
        else in.setAmount(in.getAmount()-1);
    }
}
