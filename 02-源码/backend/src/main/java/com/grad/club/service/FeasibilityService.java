package com.grad.club.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.grad.club.common.BizException;
import com.grad.club.common.UserContext;
import com.grad.club.entity.*;
import com.grad.club.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 创新点：活动可行性评分。
 *
 * 五项因素加权（需求文档第 8 节）：预算合理性 25%、场地与时段冲突 20%、
 * 预计报名与场地容量匹配 20%、历史同类活动效果 20%、社团年审状态与经费余额 15%。
 * 每次计算保存输入快照、规则版本与各因素得分，结果可追溯；人工修正留原始值不覆盖。
 */
@Service
@RequiredArgsConstructor
public class FeasibilityService {

    private final FeasibilityRuleMapper ruleMapper;
    private final FeasibilitySnapshotMapper snapshotMapper;
    private final FeasibilityCorrectionMapper correctionMapper;
    private final ActivityMapper activityMapper;
    private final ClubMapper clubMapper;
    private final BudgetMapper budgetMapper;
    private final ActivitySignupMapper signupMapper;
    private final FundFlowMapper flowMapper;
    private final TraceService trace;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FeasibilityRule currentRule() {
        FeasibilityRule r = ruleMapper.selectOne(new QueryWrapper<FeasibilityRule>()
                .eq("status", "生效").orderByDesc("id").last("limit 1"));
        if (r == null) {
            r = ruleMapper.selectOne(new QueryWrapper<FeasibilityRule>().orderByDesc("id").last("limit 1"));
        }
        if (r == null) {
            throw new BizException("尚未配置可行性评分规则");
        }
        return r;
    }

    public List<Map<String, Object>> rules() {
        return ruleMapper.selectList(new QueryWrapper<FeasibilityRule>().orderByDesc("id")).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("ruleName", r.getRuleName());
            m.put("ruleVersion", r.getRuleVersion());
            m.put("weightBudget", r.getWeightBudget());
            m.put("weightVenue", r.getWeightVenue());
            m.put("weightCapacity", r.getWeightCapacity());
            m.put("weightHistory", r.getWeightHistory());
            m.put("weightClub", r.getWeightClub());
            m.put("levelA", r.getLevelA());
            m.put("levelB", r.getLevelB());
            m.put("levelC", r.getLevelC());
            m.put("status", r.getStatus());
            m.put("effectiveTime", r.getEffectiveTime() == null ? null : r.getEffectiveTime().format(DT));
            m.put("remark", r.getRemark());
            return m;
        }).toList();
    }

    /** 规则配置：权重合计必须等于 100%，旧版本置为历史，新版本立即生效 */
    @Transactional
    public Long saveRule(Map<String, Object> body) {
        BigDecimal w1 = dec(body.get("weightBudget"), new BigDecimal("25"));
        BigDecimal w2 = dec(body.get("weightVenue"), new BigDecimal("20"));
        BigDecimal w3 = dec(body.get("weightCapacity"), new BigDecimal("20"));
        BigDecimal w4 = dec(body.get("weightHistory"), new BigDecimal("20"));
        BigDecimal w5 = dec(body.get("weightClub"), new BigDecimal("15"));
        BigDecimal sum = w1.add(w2).add(w3).add(w4).add(w5);
        if (sum.compareTo(new BigDecimal("100")) != 0) {
            throw new BizException("权重合计必须等于 100%，当前为 " + sum.stripTrailingZeros().toPlainString() + "%");
        }
        FeasibilityRule old = ruleMapper.selectOne(new QueryWrapper<FeasibilityRule>()
                .eq("status", "生效").orderByDesc("id").last("limit 1"));
        if (old != null) {
            FeasibilityRule his = new FeasibilityRule();
            his.setId(old.getId());
            his.setStatus("历史");
            ruleMapper.updateById(his);
        }
        FeasibilityRule r = new FeasibilityRule();
        r.setRuleName(body.get("ruleName") == null ? "活动可行性评分规则" : String.valueOf(body.get("ruleName")));
        r.setRuleVersion(body.get("ruleVersion") == null ? "V" + (new Random().nextInt(900) + 100)
                : String.valueOf(body.get("ruleVersion")));
        r.setWeightBudget(w1);
        r.setWeightVenue(w2);
        r.setWeightCapacity(w3);
        r.setWeightHistory(w4);
        r.setWeightClub(w5);
        r.setLevelA(dec(body.get("levelA"), new BigDecimal("85")));
        r.setLevelB(dec(body.get("levelB"), new BigDecimal("70")));
        r.setLevelC(dec(body.get("levelC"), new BigDecimal("55")));
        r.setStatus("生效");
        r.setEffectiveTime(LocalDateTime.now());
        r.setRemark(body.get("remark") == null ? "调整权重配置" : String.valueOf(body.get("remark")));
        r.setCreateBy(UserContext.userId());
        r.setCreateTime(LocalDateTime.now());
        r.setDeleted(0);
        r.setVersion(0);
        ruleMapper.insert(r);
        trace.log("统计分析", "修改", r.getRuleVersion(),
                "调整可行性评分权重：" + w1 + "/" + w2 + "/" + w3 + "/" + w4 + "/" + w5);
        return r.getId();
    }

    /** 计算并保存快照（相同输入 + 相同规则版本 → 相同结果，可用重算验证） */
    @Transactional
    public Map<String, Object> evaluate(Long activityId) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BizException(404, "活动不存在");
        }
        FeasibilityRule rule = currentRule();
        Club club = clubMapper.selectById(a.getClubId());

        // 1. 预算合理性与单位人次成本：与同类型已结项活动的单位成本对比
        List<Activity> sameType = activityMapper.selectList(new QueryWrapper<Activity>()
                .eq("activity_type", a.getActivityType()).eq("status", "已结项")
                .gt("expect_count", 0).ne("id", activityId).last("limit 30"));
        double unitCost = a.getExpectCount() == null || a.getExpectCount() == 0 ? 0
                : a.getBudgetTotal().doubleValue() / a.getExpectCount();
        double sBudget;
        String budgetNote;
        if (sameType.isEmpty()) {
            sBudget = 70;  // 缺失处理：无同类历史按 70 分基准
            budgetNote = "无同类历史活动，预算合理性按 70 分基准计";
        } else {
            double avg = sameType.stream().mapToDouble(x -> x.getBudgetTotal().doubleValue()
                    / Math.max(1, x.getExpectCount())).average().orElse(unitCost);
            double ratio = avg == 0 ? 1 : unitCost / avg;
            sBudget = clamp(100 - Math.abs(ratio - 1) * 120);
            budgetNote = "单位人次成本 " + String.format("%.1f", unitCost) + " 元，同类均值 "
                    + String.format("%.1f", avg) + " 元";
        }

        // 2. 场地与时段冲突：同场地同日时段重叠、容量
        List<Activity> conflicts = activityMapper.selectList(new QueryWrapper<Activity>()
                .eq("place_name", a.getPlaceName())
                .in("status", List.of("待审批", "已立项", "进行中"))
                .lt("start_time", a.getEndTime()).gt("end_time", a.getStartTime()).ne("id", activityId));
        double sVenue;
        String venueNote;
        if (a.getPlaceName() == null || a.getPlaceName().isBlank()) {
            sVenue = 0;  // 场地未确定记 0 分
            venueNote = "场地未确定，请先办理场地借用";
        } else if (!conflicts.isEmpty()) {
            sVenue = 30;
            venueNote = "同场地时段与「" + conflicts.get(0).getActivityName() + "」重叠";
        } else {
            sVenue = 100;
            venueNote = "场地时段无冲突，容量 " + a.getPlaceCapacity() + " 人";
        }

        // 3. 预计人数与容量匹配 + 近 3 次同类活动报名率
        double rate = a.getPlaceCapacity() == null || a.getPlaceCapacity() == 0 ? 0.7
                : Math.min(1.0, a.getExpectCount() / (double) a.getPlaceCapacity());
        double sCapacity = clamp(rate <= 0.95 ? 60 + rate * 40 : 100 - (rate - 0.95) * 200);
        String capacityNote = "预计 " + a.getExpectCount() + " 人 / 容量 " + a.getPlaceCapacity()
                + " 人，占用率 " + Math.round(rate * 100) + "%";

        // 4. 历史同类活动效果：到场率
        List<Activity> history = activityMapper.selectList(new QueryWrapper<Activity>()
                .eq("activity_type", a.getActivityType()).in("status", List.of("已结束", "已结项"))
                .gt("expect_count", 0).last("limit 10"));
        double sHistory;
        String historyNote;
        if (history.isEmpty()) {
            sHistory = 60;  // 新社团无历史按 60 分基准
            historyNote = "无历史同类活动，按 60 分基准计";
        } else {
            double attend = history.stream().mapToDouble(x -> x.getActualCount() == null ? 0
                    : Math.min(1.0, x.getActualCount() / (double) Math.max(1, x.getExpectCount())))
                    .average().orElse(0.8);
            sHistory = clamp(attend * 100);
            historyNote = "近 " + history.size() + " 场同类活动平均到场率 " + Math.round(attend * 100) + "%";
        }

        // 5. 年审状态与经费余额
        double sClub;
        String clubNote;
        if (club == null) {
            sClub = 0;
            clubNote = "社团信息缺失";
        } else if ("整改中".equals(club.getStatus())) {
            sClub = 0;
            clubNote = "社团处于整改中，不能申报新活动";
        } else if ("已注销".equals(club.getStatus())) {
            sClub = 0;
            clubNote = "社团已注销";
        } else {
            BigDecimal income = flowMapper.selectList(new QueryWrapper<FundFlow>().eq("club_id", club.getId()))
                    .stream().filter(f -> "收入".equals(f.getDirection()))
                    .map(FundFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal out = flowMapper.selectList(new QueryWrapper<FundFlow>().eq("club_id", club.getId()))
                    .stream().filter(f -> "支出".equals(f.getDirection()))
                    .map(FundFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal balance = income.subtract(out);
            BigDecimal need = a.getBudgetTotal() == null ? BigDecimal.ZERO : a.getBudgetTotal();
            if (balance.compareTo(need) >= 0) {
                sClub = 100;
                clubNote = "社团余额 " + balance + " 元，可覆盖本次活动预算";
            } else {
                sClub = 30;
                clubNote = "社团余额 " + balance + " 元，不足以覆盖预算 " + need + " 元";
            }
        }

        double total = sBudget * rule.getWeightBudget().doubleValue() / 100
                + sVenue * rule.getWeightVenue().doubleValue() / 100
                + sCapacity * rule.getWeightCapacity().doubleValue() / 100
                + sHistory * rule.getWeightHistory().doubleValue() / 100
                + sClub * rule.getWeightClub().doubleValue() / 100;
        total = Math.round(total * 10) / 10.0;
        String level = total >= rule.getLevelA().doubleValue() ? "A"
                : total >= rule.getLevelB().doubleValue() ? "B"
                : total >= rule.getLevelC().doubleValue() ? "C" : "D";

        List<String> suggestions = new ArrayList<>();
        if (sBudget < 75) {
            suggestions.add("压缩预算或调整测算依据，使单位人次成本接近同类活动均值");
        }
        if (sVenue < 80) {
            suggestions.add("改期或更换场地，先完成场地借用手续");
        }
        if (sCapacity < 75) {
            suggestions.add("调整预计人数或更换容量匹配的场地");
        }
        if (sHistory < 75) {
            suggestions.add("提前一周开放报名并加强宣传，提高到场率");
        }
        if (sClub < 60) {
            suggestions.add("先完成整改或补充经费来源说明");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("材料齐全，可按申报金额立项");
        }
        List<String> unsatisfied = new ArrayList<>();
        if (!conflicts.isEmpty()) {
            unsatisfied.add("同场地时段存在重叠");
        }
        if (sClub < 60) {
            unsatisfied.add("社团年审状态或经费余额不满足要求");
        }
        if (a.getExpectCount() != null && a.getPlaceCapacity() != null
                && a.getExpectCount() > a.getPlaceCapacity()) {
            unsatisfied.add("预计人数超过场地容量");
        }

        FeasibilitySnapshot s = new FeasibilitySnapshot();
        s.setActivityId(activityId);
        s.setRuleId(rule.getId());
        s.setRuleVersion(rule.getRuleVersion());
        s.setScoreBudget(bd(sBudget));
        s.setScoreVenue(bd(sVenue));
        s.setScoreCapacity(bd(sCapacity));
        s.setScoreHistory(bd(sHistory));
        s.setScoreClub(bd(sClub));
        s.setTotalScore(bd(total));
        s.setLevel(level);
        s.setSuggestion(String.join("；", suggestions));
        s.setUnsatisfied(unsatisfied.isEmpty() ? null : String.join("；", unsatisfied));
        s.setInputSnapshot("{\"unitCost\":\"" + budgetNote + "\",\"venue\":\"" + venueNote
                + "\",\"capacity\":\"" + capacityNote + "\",\"history\":\"" + historyNote
                + "\",\"club\":\"" + clubNote + "\"}");
        s.setCalcTime(LocalDateTime.now());
        s.setCreateBy(UserContext.userId());
        s.setCreateTime(LocalDateTime.now());
        s.setDeleted(0);
        s.setVersion(0);
        snapshotMapper.insert(s);

        // 得分回写活动，供列表与审批台展示
        Activity up = new Activity();
        up.setId(activityId);
        up.setFeasibilityScore(bd(total));
        activityMapper.updateById(up);
        trace.log("统计分析", "计算", a.getActivityNo(), "可行性评分 " + total + " 分（" + level + " 级）");

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("snapshotId", s.getId());
        r.put("activityId", activityId);
        r.put("ruleVersion", rule.getRuleVersion());
        r.put("totalScore", bd(total));
        r.put("level", level);
        r.put("levelText", levelText(level));
        r.put("suggestion", s.getSuggestion());
        r.put("unsatisfied", s.getUnsatisfied());
        r.put("factors", List.of(
                Map.of("name", "预算合理性", "weight", rule.getWeightBudget(), "score", bd(sBudget),
                        "note", budgetNote),
                Map.of("name", "场地与时段冲突", "weight", rule.getWeightVenue(), "score", bd(sVenue),
                        "note", venueNote),
                Map.of("name", "人数与容量匹配", "weight", rule.getWeightCapacity(), "score", bd(sCapacity),
                        "note", capacityNote),
                Map.of("name", "历史同类活动效果", "weight", rule.getWeightHistory(), "score", bd(sHistory),
                        "note", historyNote),
                Map.of("name", "年审状态与经费余额", "weight", rule.getWeightClub(), "score", bd(sClub),
                        "note", clubNote)));
        return r;
    }

    public Map<String, Object> latest(Long activityId) {
        FeasibilitySnapshot s = snapshotMapper.selectOne(new QueryWrapper<FeasibilitySnapshot>()
                .eq("activity_id", activityId).orderByDesc("id").last("limit 1"));
        if (s == null) {
            return evaluate(activityId);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("snapshotId", s.getId());
        r.put("activityId", s.getActivityId());
        r.put("ruleVersion", s.getRuleVersion());
        r.put("totalScore", s.getTotalScore());
        r.put("level", s.getLevel());
        r.put("levelText", levelText(s.getLevel()));
        r.put("suggestion", s.getSuggestion());
        r.put("unsatisfied", s.getUnsatisfied());
        r.put("inputSnapshot", s.getInputSnapshot());
        r.put("calcTime", s.getCalcTime() == null ? null : s.getCalcTime().format(DT));
        r.put("factors", List.of(
                Map.of("name", "预算合理性", "score", s.getScoreBudget()),
                Map.of("name", "场地与时段冲突", "score", s.getScoreVenue()),
                Map.of("name", "人数与容量匹配", "score", s.getScoreCapacity()),
                Map.of("name", "历史同类活动效果", "score", s.getScoreHistory()),
                Map.of("name", "年审状态与经费余额", "score", s.getScoreClub())));
        List<FeasibilityCorrection> cs = correctionMapper.selectList(new QueryWrapper<FeasibilityCorrection>()
                .eq("snapshot_id", s.getId()).orderByDesc("id"));
        if (!cs.isEmpty()) {
            FeasibilityCorrection c = cs.get(0);
            r.put("correction", Map.of("newLevel", c.getNewLevel(), "originLevel", c.getOriginLevel(),
                    "originScore", c.getOriginScore(), "reason", c.getReason(),
                    "operTime", c.getOperTime() == null ? "" : c.getOperTime().format(DT)));
        }
        return r;
    }

    /** 人工修正：原因不少于 5 字，原始结果保留 */
    @Transactional
    public boolean correct(Long activityId, String snapshotId, String newLevel, String reason) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("人工修正原因不少于 5 个字");
        }
        FeasibilitySnapshot s = snapshotId == null
                ? snapshotMapper.selectOne(new QueryWrapper<FeasibilitySnapshot>()
                .eq("activity_id", activityId).orderByDesc("id").last("limit 1"))
                : snapshotMapper.selectById(Long.valueOf(snapshotId));
        if (s == null) {
            throw new BizException("尚无可行性计算结果，请先计算");
        }
        FeasibilityCorrection c = new FeasibilityCorrection();
        c.setSnapshotId(s.getId());
        c.setActivityId(s.getActivityId());
        c.setOriginScore(s.getTotalScore());
        c.setOriginLevel(s.getLevel());
        c.setNewLevel(newLevel);
        c.setReason(reason);
        c.setOperatorId(UserContext.userId());
        c.setOperTime(LocalDateTime.now());
        c.setCreateBy(UserContext.userId());
        c.setCreateTime(LocalDateTime.now());
        c.setDeleted(0);
        c.setVersion(0);
        correctionMapper.insert(c);
        trace.log("统计分析", "修正", String.valueOf(s.getActivityId()),
                "可行性等级由 " + s.getLevel() + " 人工修正为 " + newLevel + "；原因：" + reason);
        return true;
    }

    /** 看板用：推荐等级分布、人工修正率 */
    public Map<String, Object> stat() {
        List<FeasibilitySnapshot> all = snapshotMapper.selectList(null);
        Map<String, Long> levelDist = new LinkedHashMap<>();
        for (String l : List.of("A", "B", "C", "D")) {
            levelDist.put(l, all.stream().filter(s -> l.equals(s.getLevel())).count());
        }
        long corrected = correctionMapper.selectCount(null);
        long total = all.size();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("levelDistribution", levelDist.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());
        m.put("corrected", corrected);
        m.put("correctionRate", total == 0 ? 0 : Math.round(corrected * 1000.0 / total) / 10.0);
        m.put("ruleVersion", currentRule().getRuleVersion());
        return m;
    }

    private static String levelText(String level) {
        return switch (level) {
            case "A" -> "材料齐全，可直接立项";
            case "B" -> "可立项，需补充预算测算或调整时段";
            case "C" -> "暂缓立项，建议压缩预算 20% 或改期";
            default -> "不建议立项，需重做方案并说明经费来源";
        };
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal dec(Object o, BigDecimal def) {
        if (o == null || String.valueOf(o).isBlank()) {
            return def;
        }
        return new BigDecimal(String.valueOf(o));
    }
}
