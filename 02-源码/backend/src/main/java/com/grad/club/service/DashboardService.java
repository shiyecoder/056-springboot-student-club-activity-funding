package com.grad.club.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.grad.club.entity.*;
import com.grad.club.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计看板：8 个核心指标 + 6 张图表（需求文档第 9 节）。
 * 统计口径与明细口径一致：图表点击后跳转的列表，总数必须与图表数值相同。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClubMapper clubMapper;
    private final ClubMemberMapper memberMapper;
    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper signupMapper;
    private final ExpenseMapper expenseMapper;
    private final FundFlowMapper flowMapper;
    private final FundDisclosureMapper disclosureMapper;
    private final FundObjectionMapper objectionMapper;
    private final FeasibilitySnapshotMapper snapshotMapper;

    private static final List<String> LEVELS = List.of("A", "B", "C", "D");

    public Map<String, Object> overview() {
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        long clubTotal = clubMapper.selectCount(new QueryWrapper<Club>().ne("status", "已注销"));
        long memberTotal = memberMapper.selectCount(new QueryWrapper<ClubMember>().eq("status", "在册"));
        long monthActs = activityMapper.selectCount(new QueryWrapper<Activity>()
                .ge("start_time", monthStart.toString().replace('T', ' ')));
        long pendingActs = activityMapper.selectCount(new QueryWrapper<Activity>().eq("status", "待审批"));

        List<Activity> finished = activityMapper.selectList(new QueryWrapper<Activity>()
                .in("status", List.of("已结束", "已结项")).gt("expect_count", 0));
        double attend = finished.isEmpty() ? 0 : Math.round(finished.stream()
                .mapToDouble(a -> Math.min(1.0, (a.getActualCount() == null ? 0 : a.getActualCount())
                        / (double) a.getExpectCount())).average().orElse(0) * 1000) / 10.0;

        BigDecimal limit = clubMapper.selectList(new QueryWrapper<Club>().ne("status", "已注销"))
                .stream().map(Club::getFundLimit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal spent = flowMapper.selectList(new QueryWrapper<FundFlow>().eq("direction", "支出"))
                .stream().map(FundFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        double execRate = limit.compareTo(BigDecimal.ZERO) == 0 ? 0
                : spent.multiply(new BigDecimal("100")).divide(limit, 1, RoundingMode.HALF_UP).doubleValue();

        BigDecimal pendingPay = expenseMapper.selectList(new QueryWrapper<Expense>()
                        .in("status", List.of("待初审", "待财务复核")))
                .stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long objections = objectionMapper.selectCount(new QueryWrapper<FundObjection>()
                .in("handle_status", List.of("待答复")));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clubTotal", clubTotal);
        m.put("memberTotal", memberTotal);
        m.put("monthActivity", monthActs);
        m.put("pendingActivity", pendingActs);
        m.put("attendRate", attend);
        m.put("fundExecRate", execRate);
        m.put("pendingExpense", pendingPay);
        m.put("objectionCount", objections);
        m.put("fundLimitTotal", limit);
        m.put("fundSpentTotal", spent);
        return m;
    }

    public Map<String, Object> charts() {
        Map<String, Object> m = new LinkedHashMap<>();

        // 1. 社团类别占比
        List<Club> clubs = clubMapper.selectList(new QueryWrapper<Club>().ne("status", "已注销"));
        m.put("clubType", clubs.stream().collect(Collectors.groupingBy(Club::getClubType, Collectors.counting()))
                .entrySet().stream().map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());

        // 2. 近 6 个月活动场次与参与人次
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDateTime s = ym.atDay(1).atStartOfDay();
            LocalDateTime e = ym.plusMonths(1).atDay(1).atStartOfDay();
            List<Activity> acts = activityMapper.selectList(new QueryWrapper<Activity>()
                    .ge("start_time", s.toString().replace('T', ' '))
                    .lt("start_time", e.toString().replace('T', ' ')));
            int people = acts.stream().mapToInt(a -> a.getActualCount() == null ? 0 : a.getActualCount()).sum();
            trend.add(Map.of("month", ym.getMonthValue() + "月", "activity", acts.size(), "people", people));
        }
        m.put("monthlyTrend", trend);

        // 3. 经费支出结构（按流水业务类型）
        List<FundFlow> outFlows = flowMapper.selectList(new QueryWrapper<FundFlow>().eq("direction", "支出"));
        m.put("expenseStructure", outFlows.stream()
                .collect(Collectors.groupingBy(FundFlow::getBizType,
                        Collectors.reducing(BigDecimal.ZERO, FundFlow::getAmount, BigDecimal::add)))
                .entrySet().stream().map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());

        // 4. 各社团预算执行率（前 8）
        List<Map<String, Object>> rates = new ArrayList<>();
        for (Club c : clubs) {
            BigDecimal limit = c.getFundLimit() == null ? BigDecimal.ZERO : c.getFundLimit();
            BigDecimal used = activityMapper.selectList(new QueryWrapper<Activity>()
                            .eq("club_id", c.getId())
                            .in("status", List.of("已立项", "进行中", "已结束", "已结项")))
                    .stream().map(Activity::getBudgetTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            double r = limit.compareTo(BigDecimal.ZERO) == 0 ? 0
                    : used.multiply(new BigDecimal("100")).divide(limit, 1, RoundingMode.HALF_UP).doubleValue();
            rates.add(Map.of("name", c.getClubName(), "value", r, "status", c.getStatus()));
        }
        rates.sort((a, b) -> Double.compare((double) b.get("value"), (double) a.get("value")));
        m.put("clubExecRate", rates.subList(0, Math.min(8, rates.size())));

        // 5. 报名到签到转化漏斗
        long signups = signupMapper.selectCount(new QueryWrapper<ActivitySignup>().eq("cancel_flag", 0));
        long checked = signupMapper.selectCount(new QueryWrapper<ActivitySignup>()
                .eq("cancel_flag", 0).in("checkin_status", List.of("已签到", "迟到")));
        long onTime = signupMapper.selectCount(new QueryWrapper<ActivitySignup>()
                .eq("cancel_flag", 0).eq("checkin_status", "已签到"));
        m.put("funnel", List.of(
                Map.of("name", "报名", "value", signups),
                Map.of("name", "签到", "value", checked),
                Map.of("name", "准时签到", "value", onTime)));

        // 6. 可行性等级分布
        List<FeasibilitySnapshot> snaps = snapshotMapper.selectList(null);
        m.put("feasibilityLevel", LEVELS.stream()
                .map(l -> Map.of("name", l, "value", snaps.stream().filter(s -> l.equals(s.getLevel())).count()))
                .toList());
        m.put("feasibilityTotal", snaps.size());
        return m;
    }

    /** 统计报表页数据 */
    public Map<String, Object> report(String startDate, String endDate) {
        String s = (startDate == null || startDate.isBlank())
                ? LocalDate.now().minusMonths(6).toString() : startDate;
        String e = (endDate == null || endDate.isBlank()) ? LocalDate.now().toString() : endDate;
        Map<String, Object> ov = overview();
        // 区间口径：「区间内活动」按用户选的日期区间统计，否则报表页的日期筛选就是假功能
        ov.put("monthActivity", activityMapper.selectCount(new QueryWrapper<Activity>()
                .ge("start_time", s + " 00:00:00").le("start_time", e + " 23:59:59")));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("overview", ov);
        m.put("charts", charts());
        m.put("clubStatus", clubMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(Club::getStatus, Collectors.counting()))
                .entrySet().stream().map(en -> Map.of("name", en.getKey(), "value", en.getValue())).toList());
        m.put("disclosureStatus", disclosureMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(FundDisclosure::getStatus, Collectors.counting()))
                .entrySet().stream().map(en -> Map.of("name", en.getKey(), "value", en.getValue())).toList());
        Map<String, Object> caliber = new LinkedHashMap<>();
        caliber.put("dateRange", s + " ~ " + e);
        caliber.put("attendRate", "已结束/已结项活动的实际到场人数 ÷ 预计人数，按算术平均");
        caliber.put("fundExecRate", "经费流水支出合计 ÷ 未注销社团年度经费上限合计");
        m.put("caliber", caliber);
        return m;
    }
}
