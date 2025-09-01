package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import com.minkang.ultimate.managers.ShopManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * /상점 명령 개선:
 * - /상점 → 깔끔한 세로형 도움말
 * - /상점 추가 <이름> <구매|판매> [가격] [슬롯]
 * - /상점 열기 <이름>
 * - /상점 연동 <이름>  (시선 방향의 Citizens NPC에 연동)
 * - /상점 연동해제     (바라보는 NPC 연동 해제)
 * - /상점 연동목록     (연동된 NPC → 상점 목록)
 *
 * Citizens 2.0.27-b2218, Spigot 1.16.5, Java 8 호환.
 */
public class ShopCommand implements CommandExecutor {

    private boolean hasPerm(CommandSender s, String p) {
        if (s.hasPermission(p)) return true;
        s.sendMessage("§c권한이 없습니다: " + p);
        return false;
    }

    private void sendShopHelp(CommandSender s) {
        s.sendMessage(ChatColor.DARK_GRAY + "====================");
        s.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "상점 명령어");
        s.sendMessage(ChatColor.GRAY + "  /상점 추가 <이름> <구매|판매> [가격] [슬롯]
        s.sendMessage(ChatColor.GRAY + "  /상점 열기 <이름>");
        s.sendMessage(ChatColor.GRAY + "  /상점 생성 <이름>");
        s.sendMessage(ChatColor.GRAY + "  /상점 연동 <이름>   " + ChatColor.DARK_GRAY + "← NPC 바라보고 실행");
        s.sendMessage(ChatColor.GRAY + "  /상점 연동해제      " + ChatColor.DARK_GRAY + "← NPC 바라보고 실행");
        s.sendMessage(ChatColor.GRAY + "  /상점 연동목록");
        s.sendMessage(ChatColor.DARK_GRAY + "--------------------");
        s.sendMessage(ChatColor.GRAY + "TIP: NPC를 향해 보고 명령을 입력하세요.");
        s.sendMessage(ChatColor.DARK_GRAY + "====================");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendShopHelp(sender);
            return true;
        }

        final Main pl = Main.getPlugin(Main.class);
        final ShopManager shops = pl.shop();

        final String sub = args[0];

        // /상점 열기 <이름>
        if ("열기".equalsIgnoreCase(sub)) {
            if (!(sender instanceof Player)) { sender.sendMessage("§c플레이어만 사용 가능합니다."); return true; }
            if (args.length < 2) { sender.sendMessage("§c사용법: /상점 열기 <이름>"); return true; }
            String key = args[1];
            shops.open((Player) sender, key);
            return true;
        }

        // /상점 추가 <이름> <구매|판매> [가격] [슬롯]
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

            shops.createOrUpdate(name, buy, sell, price);
            if (slot != null && sender instanceof Player){
                boolean ok = shops.addItemToSlot((Player)sender, name, slot, buy, sell, price);
                sender.sendMessage(ok? "§a아이템 등록됨: §f" + name + " §7슬롯 " + slot : "§c아이템 등록 실패: 손에 든 아이템 확인");
            } else {
                sender.sendMessage("§7아이템 슬롯 미지정: 메타만 갱신됨.");
            }
            sender.sendMessage("§a상점 등록/갱신: §f"+name+" §7("+mode+(price!=null? " 가격="+price:"")+")");
            return true;
        }
            String name = args[1];
            String mode = args[2];
            Double price = null;
            if (args.length >= 4) {
                try { price = Double.parseDouble(args[3]); } catch (Exception ignored) {}
            }
            boolean buy = "구매".equalsIgnoreCase(mode);
            boolean sell = "판매".equalsIgnoreCase(mode);
            if (!buy && !sell) {
                sender.sendMessage("§c모드는 '구매' 또는 '판매'만 가능합니다.");
                return true;
            }
            shops.createOrUpdate(name, buy, sell, price);
            sender.sendMessage("§a상점 등록/갱신: §f" + name + " §7(" + mode + (price != null ? " 가격=" + price : "") + ")");
            return true;
        }

        
        // /상점 생성 <이름>
        if ("생성".equalsIgnoreCase(sub)) {
            if (args.length < 2) { sender.sendMessage("§c사용법: /상점 생성 <이름>"); return true; }
            String name = args[1];
            shops.createEmpty(name);
            sender.sendMessage("§a상점 생성됨: §f" + name + " §7(기본 6행)");
            return true;
        }
// Citizens 연동 관련: 연동/연동해제/연동목록
        if ("연동".equalsIgnoreCase(sub)) {
            if (!(sender instanceof Player)) { sender.sendMessage("§c플레이어만 사용 가능합니다."); return true; }
            if (args.length < 2) { sender.sendMessage("§c사용법: /상점 연동 <상점이름>"); return true; }
            Player p = (Player) sender;
            NPC npc = findTargetNPC(p, 6.0);
            if (npc == null) {
                sender.sendMessage("§cNPC를 정확히 바라보고 다시 시도하세요. (거리 6블록 이내)");
                return true;
            }
            String shopKey = args[1];
            shops.setNpcBinding(npc.getId(), shopKey);
            sender.sendMessage("§a연동 완료: NPC §f#" + npc.getId() + " §7→ 상점 §f" + shopKey);
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
            if (bindings.isEmpty()) {
                sender.sendMessage("§7연동된 NPC가 없습니다.");
                return true;
            }
            sender.sendMessage(ChatColor.DARK_GRAY + "==== " + ChatColor.YELLOW + "NPC 연동 목록" + ChatColor.DARK_GRAY + " ====");
            for (Map.Entry<Integer,String> e : bindings.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
                sender.sendMessage(" §f#" + e.getKey() + ChatColor.GRAY + " → " + ChatColor.AQUA + e.getValue());
            }
            sender.sendMessage(ChatColor.DARK_GRAY + "=========================");
            return true;
        }

        // 그 외 → 도움말
        sendShopHelp(sender);
        return true;
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
