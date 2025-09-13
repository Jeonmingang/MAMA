package com.minkang.ultimate.commands;
import com.minkang.ultimate.managers.ShopManager;

import com.minkang.ultimate.Main;
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
        s.sendMessage(ChatColor.GRAY + "  /상점 삭제 <이름>");
        s.sendMessage(ChatColor.GRAY + "  /상점 목록");
        s.sendMessage(ChatColor.GRAY + "  /상점 추가 <이름> <구매|판매> <구매|판매> <금액> <금액> <수량> <슬롯>");
        s.sendMessage(ChatColor.GRAY + "  /상점 추가삭제 <이름> <슬롯>");
        s.sendMessage(ChatColor.GRAY + "  /상점 열기 <이름>");
        s.sendMessage(ChatColor.GRAY + "  /상점 연동 <이름>   " + ChatColor.DARK_GRAY + "← NPC 바라보고 실행");
        s.sendMessage(ChatColor.GRAY + "  /상점 연동해제      " + ChatColor.DARK_GRAY + "← NPC 바라보고 실행");
        s.sendMessage(ChatColor.GRAY + "  /상점 연동목록");
        s.sendMessage(ChatColor.DARK_GRAY + "--------------------");
        s.sendMessage(ChatColor.GRAY + "좌클릭=구매, 우클릭=판매, 쉬프트=64개");
        s.sendMessage(ChatColor.DARK_GRAY + "====================");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
            org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
                if (!p.hasPermission("usp.shop.rightclick")) {
                    p.sendMessage("§c권한이 없습니다. (usp.shop.rightclick)");
                    return true;
                }
                String key = args[1];
                try { com.minkang.ultimate.managers.ShopManager.getInstance().open(p, key); } catch (Throwable t) { p.sendMessage("§c상점을 열 수 없습니다."); }
                return true;
            }
        // Reload entry point: either command label '상점리로드' or subcommand '리로드'
        if ("상점리로드".equalsIgnoreCase(label) || (args.length>0 && ("리로드".equalsIgnoreCase(args[0]) || "reload".equalsIgnoreCase(args[0])))){
            if (!sender.hasPermission("usp.shop.admin")) { sender.sendMessage("§c권한이 없습니다: usp.shop.admin"); return true; }
            try {
                com.minkang.ultimate.shop.ShopIntegrationManager integ = new com.minkang.ultimate.shop.ShopIntegrationManager(Main.getPlugin(Main.class));
                integ.reload(sender);
            } catch (Throwable t){ /* ignore */ }
            try { Main.getPlugin(Main.class).shop().reload(); } catch (Throwable t) { /* ignore */ }
            sender.sendMessage("§a상점 연동을 리로드했습니다.");
            return true;
        }

        if (args.length == 0){ sendHelp(sender); return true; }

        String sub = args[0];

        if ("목록".equalsIgnoreCase(sub)) {
            listShops(sender);
            return true;
        }

        if ("생성".equalsIgnoreCase(sub)) {
            if (!hasPerm(sender, "usp.shop.admin")) return true;
            if (args.length < 2) { sender.sendMessage("§c사용법: /상점 생성 <이름>"); return true; }
            String name = args[1];
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
            if (args.length < 3) { sendHelp(sender); return true; }
            String name = args[1];

            // 두 가지 포맷 지원
            // A) /상점 추가 <이름> <구매|판매> <구매|판매> <금액> <금액> <수량> <슬롯>
            // B) /상점 추가 <이름> <구매> <수량> <금액> <슬롯>
            FileConfiguration cfg = plugin.getConfig();
            String base = "shops."+name+".";
            if (!cfg.isConfigurationSection("shops."+name)) cfg.createSection("shops."+name);
            if (cfg.getConfigurationSection(base+"items") == null) cfg.createSection(base+"items");

            if (args.length >= 8) {
                // 포맷 A
                String m1 = args[2];
                String m2 = args[3];
                double price1, price2; int amount, slot;
                try {
                    price1 = Double.parseDouble(args[4]);
                    price2 = Double.parseDouble(args[5]);
                    amount = Integer.parseInt(args[6]);
                    slot = Integer.parseInt(args[7]);
                } catch (Exception e){ sender.sendMessage("§c숫자 인자(금액/수량/슬롯)를 확인하세요."); return true; }
                if (!(sender instanceof Player)){
                    sender.sendMessage("§c아이템 등록은 플레이어만 가능합니다."); return true;
                }
                Player p = (Player)sender;
                ItemStack inHand = p.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().isAir()){ sender.sendMessage("§c손에 든 아이템이 없습니다."); return true; }

                cfg.set(base+"items."+slot+".item", inHand.clone());
                cfg.set(base+"items."+slot+".amount", amount);
                // m1 → price1, m2 → price2
                boolean b1 = "구매".equalsIgnoreCase(m1); boolean s1 = "판매".equalsIgnoreCase(m1);
                boolean b2 = "구매".equalsIgnoreCase(m2); boolean s2 = "판매".equalsIgnoreCase(m2);
                cfg.set(base+"items."+slot+".buy.enabled", b1 || b2);
                cfg.set(base+"items."+slot+".sell.enabled", s1 || s2);
                if (b1) cfg.set(base+"items."+slot+".buy.price", price1);
                if (s1) cfg.set(base+"items."+slot+".sell.price", price1);
                if (b2) cfg.set(base+"items."+slot+".buy.price", price2);
                if (s2) cfg.set(base+"items."+slot+".sell.price", price2);

                plugin.saveConfig();
                sender.sendMessage("§a등록됨: §f" + name + " §7슬롯 " + slot + " §8(수량 " + amount + ", 구매:"+(cfg.getBoolean(base+"items."+slot+".buy.enabled")?cfg.getDouble(base+"items."+slot+".buy.price"):"X")
                        + ", 판매:"+(cfg.getBoolean(base+"items."+slot+".sell.enabled")?cfg.getDouble(base+"items."+slot+".sell.price"):"X")+")");
                return true;

            } else if (args.length == 6) {
                // 포맷 B
                String m = args[2];
                if (!"구매".equalsIgnoreCase(m) && !"판매".equalsIgnoreCase(m)){
                    sender.sendMessage("§c형식: /상점 추가 <이름> <구매> <수량> <금액> <슬롯>"); return true;
                }
                int amount, slot; double price;
                try{
                    amount = Integer.parseInt(args[3]);
                    price = Double.parseDouble(args[4]);
                    slot = Integer.parseInt(args[5]);
                }catch (Exception e){ sender.sendMessage("§c숫자 인자(금액/수량/슬롯)를 확인하세요."); return true; }
                if (!(sender instanceof Player)){
                    sender.sendMessage("§c아이템 등록은 플레이어만 가능합니다."); return true;
                }
                Player p = (Player)sender;
                ItemStack inHand = p.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().isAir()){ sender.sendMessage("§c손에 든 아이템이 없습니다."); return true; }

                cfg.set(base+"items."+slot+".item", inHand.clone());
                cfg.set(base+"items."+slot+".amount", amount);
                if ("구매".equalsIgnoreCase(m)){
                    cfg.set(base+"items."+slot+".buy.enabled", true);
                    cfg.set(base+"items."+slot+".buy.price", price);
                    cfg.set(base+"items."+slot+".sell.enabled", false);
                }else{
                    cfg.set(base+"items."+slot+".sell.enabled", true);
                    cfg.set(base+"items."+slot+".sell.price", price);
                    cfg.set(base+"items."+slot+".buy.enabled", false);
                }
                plugin.saveConfig();
                sender.sendMessage("§a등록됨: §f" + name + " §7슬롯 " + slot + " §8(수량 " + amount + ", " + m + ":" + price + ")");
                return true;
            } else {
                sendHelp(sender);
                return true;
            }
        }

        
        if ("삭제".equalsIgnoreCase(sub)) {
            if (!hasPerm(sender, "usp.shop.admin")) return true;
            if (args.length < 2) { sender.sendMessage("§c사용법: /상점 삭제 <상점이름>"); return true; }
            String name = args[1];
            FileConfiguration cfg = plugin.getConfig();
            if (!cfg.isConfigurationSection("shops."+name)) {
                sender.sendMessage("§c해당 상점이 없습니다: " + name);
                return true;
            }
            cfg.set("shops."+name, null);
            plugin.saveConfig();
            sender.sendMessage("§a상점 삭제 완료: §f" + name);
            return true;
        }

        if ("추가삭제".equalsIgnoreCase(sub)) {
            if (!hasPerm(sender, "usp.shop.admin")) return true;
            if (args.length < 3) { sender.sendMessage("§c사용법: /상점 추가삭제 <상점이름> <슬롯>"); return true; }
            String name = args[1];
            int slot;
            try { slot = Integer.parseInt(args[2]); } catch (Exception ex){ sender.sendMessage("§c슬롯은 숫자여야 합니다."); return true; }
            FileConfiguration cfg = plugin.getConfig();
            if (!cfg.isConfigurationSection("shops."+name)) {
                sender.sendMessage("§c해당 상점이 없습니다: " + name);
                return true;
            }
            if (cfg.get("shops."+name+".items."+slot) == null) {
                sender.sendMessage("§c해당 슬롯에 아이템이 없습니다.");
                return true;
            }
            cfg.set("shops."+name+".items."+slot, null);
            plugin.saveConfig();
            sender.sendMessage("§a삭제 완료: §f" + name + " §7슬롯 §f" + slot);
            return true;
        }
if ("연동".equalsIgnoreCase(sub)) {
            if (!(sender instanceof Player)) { sender.sendMessage("§c플레이어만 사용 가능합니다."); return true; }
            if (args.length < 2) { sender.sendMessage("§c사용법: /상점 연동 <상점이름>"); return true; }
            Player p = (Player) sender;
            NPC npc = findTargetNPC(p, 6.0);
            if (npc == null) { sender.sendMessage("§cNPC를 정확히 바라보고 다시 시도하세요. (거리 6블록 이내)"); return true; }
            // config 에 저장 (별도 매니저 없어도 동작)
            FileConfiguration cfg = plugin.getConfig();
            cfg.set("npcshop.bindings."+npc.getId(), args[1]);
            plugin.saveConfig();
            sender.sendMessage("§a연동 완료: NPC §f#" + npc.getId() + " §7→ 상점 §f" + args[1]);
            sender.sendMessage("§7이제 해당 NPC를 우클릭하면 상점이 열립니다.");
            return true;
        }

        if ("연동해제".equalsIgnoreCase(sub)) {
            if (!(sender instanceof Player)) { sender.sendMessage("§c플레이어만 사용 가능합니다."); return true; }
            Player p = (Player) sender;
            NPC npc = findTargetNPC(p, 6.0);
            if (npc == null) { sender.sendMessage("§cNPC를 정확히 바라보고 다시 시도하세요. (거리 6블록 이내)"); return true; }
            FileConfiguration cfg = plugin.getConfig();
            cfg.set("npcshop.bindings."+npc.getId(), null);
            plugin.saveConfig();
            sender.sendMessage("§7연동 해제: NPC §f#" + npc.getId());
            return true;
        }

        if ("연동목록".equalsIgnoreCase(sub)) {
            FileConfiguration cfg = plugin.getConfig();
            ConfigurationSection sec = cfg.getConfigurationSection("npcshop.bindings");
            if (sec == null || sec.getKeys(false).isEmpty()) { sender.sendMessage("§7연동된 NPC가 없습니다."); return true; }
            sender.sendMessage(ChatColor.DARK_GRAY + "==== " + ChatColor.YELLOW + "NPC 연동 목록" + ChatColor.DARK_GRAY + " ====");
            for (String k : sec.getKeys(false)){
                sender.sendMessage(" §f#" + k + ChatColor.GRAY + " → " + ChatColor.AQUA + sec.getString(k));
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

    private void openGui(Player player, String shopKey){        List<String> hintBuy = plugin.getConfig().getStringList("shop.item-lore.hints.buy");
        List<String> hintSell = plugin.getConfig().getStringList("shop.item-lore.hints.sell");

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
                    Integer amount = itemsSec.getInt(k + ".amount", 1);
                    Double buyPrice = itemsSec.getConfigurationSection(k + ".buy") != null ? itemsSec.getDouble(k + ".buy.price") : null;
                    Double sellPrice = itemsSec.getConfigurationSection(k + ".sell") != null ? itemsSec.getDouble(k + ".sell.price") : null;
                    boolean buyEnabled = itemsSec.getBoolean(k + ".buy.enabled", false);
                    boolean sellEnabled = itemsSec.getBoolean(k + ".sell.enabled", false);

                    ItemStack copy = is.clone();
                    ItemMeta meta = copy.getItemMeta();
                    if (meta != null){
                        java.util.List<String> tpl = plugin.getConfig().getStringList("shop.item-lore.default");


                        java.util.List<String> lore = new java.util.ArrayList<String>();

                        if (tpl == null || tpl.isEmpty()){
                            lore.add("§7모드: " + (buyEnabled? "구매 " : "") + (sellEnabled? "판매" : ""));
                            lore.add("§7단위수량: §b" + amount);
                            if (buyEnabled) lore.add("§7구매가: §e" + buyPrice);
                            if (sellEnabled) lore.add("§7판매가: §e" + sellPrice);
                        } else {
                            for (String line : tpl){
                                String t = line
                                        .replace("%mode%", (buyEnabled? "구매 " : "") + (sellEnabled? "판매" : ""))
                                        .replace("%unit%", String.valueOf(amount))
                                        .replace("%buy_price%", String.valueOf(buyPrice))
                                        .replace("%sell_price%", String.valueOf(sellPrice))
                                        .replace("%shop_name%", shopKey)
                                        .replace("%item_name%", (meta.hasDisplayName()? meta.getDisplayName() : copy.getType().name()));
                                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', t));
                            }
                        }
                        if (buyEnabled){
                            if (hintBuy == null || hintBuy.isEmpty()){
                                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8— &7좌클릭 &f= 구매"));
                                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8— &7쉬프트 좌클릭 &f= 64개 구매"));
                            } else {
                                for (String h : hintBuy) lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', h));
                            }
                        }
                        if (sellEnabled){
                            if (hintSell == null || hintSell.isEmpty()){
                                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8— &7우클릭 &f= 판매"));
                                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8— &7쉬프트 우클릭 &f= 64개 판매"));
                            } else {
                                for (String h : hintSell) lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', h));
                            }
                        }

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
        try{
            Class.forName("net.citizensnpcs.api.CitizensAPI");
        }catch(Throwable t){ return null; }
        Location eye = p.getEyeLocation();
        World w = p.getWorld();
        Vector dir = eye.getDirection();
        RayTraceResult r = w.rayTraceEntities(eye, dir, maxDist, 0.5, entity -> CitizensAPI.getNPCRegistry().isNPC(entity));
        if (r == null) return null;
        Entity hit = r.getHitEntity();
        if (hit == null) return null;
        return CitizensAPI.getNPCRegistry().getNPC(hit);
    }
}
