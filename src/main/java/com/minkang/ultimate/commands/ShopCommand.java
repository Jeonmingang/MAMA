package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import com.minkang.ultimate.managers.ShopManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShopCommand implements CommandExecutor {

    private final Main plugin;
    public ShopCommand(Main plugin){ this.plugin = plugin; }

    private boolean hasPerm(CommandSender s, String p){
        if (s.hasPermission(p)) return true;
        s.sendMessage("§c권한이 없습니다: " + p);
        return false;
    }

    private void sendHelp(CommandSender s){
        s.sendMessage(ChatColor.DARK_GRAY + "====================");
        s.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "상점 명령어");
        s.sendMessage(ChatColor.GRAY + "  /상점 생성 <이름>");
        s.sendMessage(ChatColor.GRAY + "  /상점 목록");
        s.sendMessage(ChatColor.GRAY + "  /상점 추가 <이름> <구매|판매> [가격] [슬롯]");
        s.sendMessage(ChatColor.GRAY + "  /상점 열기 <이름>");
        s.sendMessage(ChatColor.GRAY + "  /상점 연동 <이름>   " + ChatColor.DARK_GRAY + "← NPC 바라보고 실행");
        s.sendMessage(ChatColor.GRAY + "  /상점 연동해제      " + ChatColor.DARK_GRAY + "← NPC 바라보고 실행");
        s.sendMessage(ChatColor.GRAY + "  /상점 연동목록");
        s.sendMessage(ChatColor.DARK_GRAY + "--------------------");
        s.sendMessage(ChatColor.GRAY + "TIP: NPC를 향해 보고 명령을 입력하세요.");
        s.sendMessage(ChatColor.DARK_GRAY + "====================");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final ShopManager shops = plugin.shop();

        if (args.length == 0){
            sendHelp(sender);
            return true;
        }

        String sub = args[0];

        if ("목록".equalsIgnoreCase(sub)) {
            listShops(sender);
            return true;
        }

        if ("생성".equalsIgnoreCase(sub)) {
            if (!hasPerm(sender, "usp.shop.admin")) return true;
            if (args.length < 2) { sender.sendMessage("§c사용법: /상점 생성 <이름>"); return true; }
            String name = args[1];
            // 기본 구조 생성
            FileConfiguration cfg = plugin.getConfig();
            String base = "shops."+name+".";
            if (!cfg.isConfigurationSection("shops."+name)) cfg.createSection("shops."+name);
            if (cfg.getInt(base+"rows", 0) <= 0) cfg.set(base+"rows", 6);
            if (cfg.getConfigurationSection(base+"items") == null) cfg.createSection(base+"items");
            plugin.saveConfig();
            sender.sendMessage("§a상점 생성됨: §f" + name + " §7(기본 6행)");
            return true;
        }

        if ("열기".equalsIgnoreCase(sub)) {
            if (!(sender instanceof Player)) { sender.sendMessage("§c플레이어만 사용 가능합니다."); return true; }
            if (args.length < 2) { sender.sendMessage("§c사용법: /상점 열기 <이름>"); return true; }
            openGui((Player)sender, args[1]);
            return true;
        }

        if ("추가".equalsIgnoreCase(sub)) {
            if (!hasPerm(sender, "usp.shop.admin")) return true;
            if (args.length < 3) {
                sender.sendMessage("§c사용법: /상점 추가 <이름> <구매|판매> [가격] [슬롯]");
                return true;
            }
            String name = args[1];
            String mode = args[2];
            Double price = null;
            Integer slot = null;
            if (args.length >= 4) { try { price = Double.parseDouble(args[3]); } catch (Exception ignored){} }
            if (args.length >= 5) { try { slot = Integer.parseInt(args[4]); } catch (Exception ignored){} }

            boolean buy = "구매".equalsIgnoreCase(mode);
            boolean sell = "판매".equalsIgnoreCase(mode);
            if (!buy && !sell) { sender.sendMessage("§c모드는 '구매' 또는 '판매'만 가능합니다."); return true; }

            // 메타 저장
            FileConfiguration cfg = plugin.getConfig();
            String base = "shops."+name+".";
            if (!cfg.isConfigurationSection("shops."+name)) cfg.createSection("shops."+name);
            cfg.set(base+"allowBuy", buy);
            cfg.set(base+"allowSell", sell);
            if (price != null) cfg.set(base+"price", price);
            if (slot != null && sender instanceof Player){
                Player p = (Player)sender;
                ItemStack inHand = p.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().isAir()) {
                    sender.sendMessage("§c아이템 등록 실패: 손에 든 아이템이 없습니다.");
                } else {
                    cfg.set(base+"items."+slot+".item", inHand.clone());
                    cfg.set(base+"items."+slot+".allowBuy", buy);
                    cfg.set(base+"items."+slot+".allowSell", sell);
                    if (price != null) cfg.set(base+"items."+slot+".price", price);
                    sender.sendMessage("§a아이템 등록됨: §f" + name + " §7슬롯 " + slot);
                }
            } else {
                sender.sendMessage("§7아이템 슬롯 미지정: 메타만 갱신됨.");
            }
            plugin.saveConfig();
            sender.sendMessage("§a상점 등록/갱신: §f"+name+" §7("+mode+(price!=null? " 가격="+price:"")+")");
            return true;
        }

        if ("연동".equalsIgnoreCase(sub)) {
            if (!(sender instanceof Player)) { sender.sendMessage("§c플레이어만 사용 가능합니다."); return true; }
            if (args.length < 2) { sender.sendMessage("§c사용법: /상점 연동 <상점이름>"); return true; }
            Player p = (Player) sender;
            NPC npc = findTargetNPC(p, 6.0);
            if (npc == null) { sender.sendMessage("§cNPC를 정확히 바라보고 다시 시도하세요. (거리 6블록 이내)"); return true; }
            shops.setNpcBinding(npc.getId(), args[1]);
            sender.sendMessage("§a연동 완료: NPC §f#" + npc.getId() + " §7→ 상점 §f" + args[1]);
            sender.sendMessage("§7이제 해당 NPC를 우클릭하면 상점이 열립니다.");
            return true;
        }

        if ("연동해제".equalsIgnoreCase(sub)) {
            if (!(sender instanceof Player)) { sender.sendMessage("§c플레이어만 사용 가능합니다."); return true; }
            Player p = (Player) sender;
            NPC npc = findTargetNPC(p, 6.0);
            if (npc == null) { sender.sendMessage("§cNPC를 정확히 바라보고 다시 시도하세요. (거리 6블록 이내)"); return true; }
            shops.setNpcBinding(npc.getId(), null);
            sender.sendMessage("§7연동 해제: NPC §f#" + npc.getId());
            return true;
        }

        if ("연동목록".equalsIgnoreCase(sub)) {
            Map<Integer, String> bindings = shops.getAllBindings();
            if (bindings == null || bindings.isEmpty()) { sender.sendMessage("§7연동된 NPC가 없습니다."); return true; }
            sender.sendMessage(ChatColor.DARK_GRAY + "==== " + ChatColor.YELLOW + "NPC 연동 목록" + ChatColor.DARK_GRAY + " ====");
            for (Map.Entry<Integer,String> e : bindings.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
                sender.sendMessage(" §f#" + e.getKey() + ChatColor.GRAY + " → " + ChatColor.AQUA + e.getValue());
            }
            sender.sendMessage(ChatColor.DARK_GRAY + "=========================");
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void listShops(CommandSender s){
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection shops = cfg.getConfigurationSection("shops");
        if (shops == null || shops.getKeys(false).isEmpty()){
            s.sendMessage("§7등록된 상점이 없습니다.");
            return;
        }
        List<String> names = new ArrayList<>(shops.getKeys(false));
        names.sort(Comparator.naturalOrder());
        s.sendMessage(ChatColor.DARK_GRAY + "==== " + ChatColor.YELLOW + "상점 목록" + ChatColor.DARK_GRAY + " ====");
        for (String n : names) s.sendMessage(" §f- " + n);
        s.sendMessage(ChatColor.DARK_GRAY + "==================");
    }

    private static class ShopInventoryHolder implements InventoryHolder {
        private final String key;
        ShopInventoryHolder(String key){ this.key = key; }
        @Override public Inventory getInventory(){ return null; }
        public String getKey(){ return key; }
    }

    private void openGui(Player player, String shopKey){
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection shopSec = cfg.getConfigurationSection("shops."+shopKey);
        if (shopSec == null){
            player.sendMessage("§c상점이 존재하지 않습니다: " + shopKey);
            return;
        }
        int rows = shopSec.getInt("rows", 6);
        if (rows < 1) rows = 1; if (rows > 6) rows = 6;
        int size = rows * 9;
        Inventory inv = Bukkit.createInventory(new ShopInventoryHolder(shopKey), size, "상점: " + shopKey);

        ConfigurationSection itemsSec = shopSec.getConfigurationSection("items");
        if (itemsSec != null){
            for (String k : itemsSec.getKeys(false)){
                int slot = -1;
                try{ slot = Integer.parseInt(k); }catch(Exception ignored){}
                if (slot < 0 || slot >= size) continue;
                ItemStack is = itemsSec.getItemStack(k + ".item");
                if (is != null){
                    Double price = null;
                    try { price = itemsSec.getDouble(k + ".price"); } catch (Throwable ignored){}
                    boolean buy = itemsSec.getBoolean(k + ".allowBuy", false);
                    boolean sell = itemsSec.getBoolean(k + ".allowSell", false);
                    ItemStack copy = is.clone();
                    ItemMeta meta = copy.getItemMeta();
                    if (meta != null){
                        List<String> lore = meta.hasLore()? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                        lore.add("§7모드: " + (buy? "구매 " : "") + (sell? "판매" : ""));
                        if (price != null) lore.add("§7가격: §e" + price);
                        meta.setLore(lore);
                        copy.setItemMeta(meta);
                    }
                    inv.setItem(slot, copy);
                }
            }
        }
        player.openInventory(inv);
    }

    /** 플레이어 시야 방향으로 Citizens NPC 탐색 */
    private NPC findTargetNPC(Player p, double maxDist) {
        if (!isCitizensPresent()) return null;
        Location eye = p.getEyeLocation();
        World w = p.getWorld();
        Vector dir = eye.getDirection();
        RayTraceResult r = w.rayTraceEntities(eye, dir, maxDist, 0.5, entity -> CitizensAPI.getNPCRegistry().isNPC(entity));
        if (r == null) return null;
        Entity hit = r.getHitEntity();
        if (hit == null) return null;
        return CitizensAPI.getNPCRegistry().getNPC(hit);
    }

    private boolean isCitizensPresent() {
        try {
            Class.forName("net.citizensnpcs.api.CitizensAPI");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
