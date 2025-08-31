package com.minkang.ultimate.commands;

import com.minkang.ultimate.managers.LockManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LockCommand implements CommandExecutor {
    private final LockManager lm;
    public LockCommand(LockManager lm){ this.lm = lm; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)) { s.sendMessage("플레이어만 사용"); return true; }
        Player p = (Player) s;

        // "/상자 잠금 ..." 지원
        if (a.length >= 1 && "잠금".equalsIgnoreCase(a[0])) {
            a = Arrays.copyOfRange(a, 1, a.length);
        }

        if (a.length == 0) {
            p.sendMessage("§7사용법: §f/잠금 시간 <기간> | /잠금 영구 | /잠금 추가 <닉> | /잠금 제거 <닉> | /잠금 해제 | /잠금 정보");
            return true;
        }

        String sub = a[0];

        if ("시간".equalsIgnoreCase(sub)) {
            if (a.length < 2) { p.sendMessage("§c기간: 90m / 2h / 1d2h30m / 120(분)"); return true; }
            long minutes = parseDurationToMinutes(a[1]);
            if (minutes <= 0) { p.sendMessage("§c유효하지 않은 기간"); return true; }
            if (!lm.consumeTempTicket(p)) { p.sendMessage("§c손에 '시간잠금권'을 들고 사용하세요."); return true; }
            Block target = getTargetContainerSmart(p);
            if (target == null) { p.sendMessage("§c가까운 상자를 바라보세요(최대 6블럭)."); return true; }
            lm.lockTimed(p, target, minutes);
            return true;
        }

        if ("영구".equalsIgnoreCase(sub)) {
            if (!lm.consumePermTicket(p)) { p.sendMessage("§c손에 '영구잠금권'을 들고 사용하세요."); return true; }
            Block target = getTargetContainerSmart(p);
            if (target == null) { p.sendMessage("§c가까운 상자를 바라보세요(최대 6블럭)."); return true; }
            lm.lockPermanent(p, target);
            return true;
        }

        if ("추가".equalsIgnoreCase(sub)) {
            if (a.length < 2) { p.sendMessage("§c닉네임을 입력하세요."); return true; }
            Block target = getTargetContainerSmart(p);
            if (target == null) { p.sendMessage("§c대상 상자를 바라보세요."); return true; }
            String name = a[1];
            OfflinePlayer op = (Bukkit.getPlayerExact(name) != null) ? Bukkit.getPlayerExact(name) : Bukkit.getOfflinePlayer(name);
            UUID u = op.getUniqueId();
            boolean ok = lm.addAllowed(p, target, u, name);
            if (!ok) p.sendMessage("§c추가 실패. 소유자/잠금 상태 확인.");
            return true;
        }

        if ("제거".equalsIgnoreCase(sub)) {
            if (a.length < 2) { p.sendMessage("§c닉네임을 입력하세요."); return true; }
            Block target = getTargetContainerSmart(p);
            if (target == null) { p.sendMessage("§c대상 상자를 바라보세요."); return true; }
            String name = a[1];
            OfflinePlayer op = (Bukkit.getPlayerExact(name) != null) ? Bukkit.getPlayerExact(name) : Bukkit.getOfflinePlayer(name);
            UUID u = op.getUniqueId();
            boolean ok = lm.removeAllowed(p, target, u, name);
            if (!ok) p.sendMessage("§c제거 실패. 소유자/잠금 상태 확인.");
            return true;
        }

        if ("해제".equalsIgnoreCase(sub)) {
            Block target = getTargetContainerSmart(p);
            if (target == null) { p.sendMessage("§c대상 상자를 바라보세요."); return true; }
            lm.unlock(p, target);
            return true;
        }

        if ("정보".equalsIgnoreCase(sub)) {
            Block target = getTargetContainerSmart(p);
            if (target == null) { p.sendMessage("§c대상 상자를 바라보세요."); return true; }
            lm.info(p, target);
            return true;
        }

        p.sendMessage("§7사용법: §f/잠금 시간 <기간> | /잠금 영구 | /잠금 추가 <닉> | /잠금 제거 <닉> | /잠금 해제 | /잠금 정보");
        return true;
    }

    private static final Pattern DUR = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?|(\\d+)");

    private long parseDurationToMinutes(String s) {
        s = s.toLowerCase().trim();
        Matcher m = DUR.matcher(s);
        if (!m.matches()) return -1;
        long days = parseNum(m.group(1));
        long hours = parseNum(m.group(2));
        long mins = parseNum(m.group(3));
        long just = parseNum(m.group(4));
        if (just > 0) return just; // 숫자만 → 분
        return days*24*60 + hours*60 + mins;
    }
    private long parseNum(String g) { return (g==null||g.isEmpty())?0:Long.parseLong(g); }

    private Block getTargetContainerSmart(Player p) {
        Block b = p.getTargetBlockExact(6);
        if (b != null && isContainer(b)) return b;
        org.bukkit.util.BlockIterator it = new org.bukkit.util.BlockIterator(p, 6);
        while (it.hasNext()) {
            Block n = it.next();
            if (isContainer(n)) return n;
        }
        return null;
    }

    private boolean isContainer(Block b) {
        switch (b.getType()) {
            case CHEST: case TRAPPED_CHEST: case BARREL: return true;
            default: return false;
        }
    }
}
