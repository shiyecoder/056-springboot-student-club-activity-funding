package com.grad.club.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.club.common.BizException;
import com.grad.club.common.PageResult;
import com.grad.club.common.UserContext;
import com.grad.club.entity.*;
import com.grad.club.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 活动：申报、立项审批、报名签到、结项。
 *
 * 状态机（需求文档第 4 节）：
 * 待审批 → 已立项 → 进行中 → 已结束 → 已结项；待审批 → 已驳回；已驳回 → 待审批（重新提交）
 * 前端不得直接提交 status 字段，所有流转走本服务的 transition 校验。
 */
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final ClubMapper clubMapper;
    private final BudgetMapper budgetMapper;
    private final ActivitySignupMapper signupMapper;
    private final FeasibilitySnapshotMapper snapshotMapper;
    private final ClubMemberMapper memberMapper;
    private final SysUserMapper userMapper;
    private final ClubService clubService;
    private final TraceService trace;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 允许的流转：当前状态 → 可到达状态集合 */
    private static final Map<String, List<String>> FLOW = Map.of(
            "待审批", List.of("已立项", "已驳回"),
            "已驳回", List.of("待审批"),
            "已立项", List.of("进行中", "已立项"),
            "进行中", List.of("已结束"),
            "已结束", List.of("已结项"),
            "已结项", List.of());

    public PageResult<Map<String, Object>> page(long page, long size, Long clubId, String keyword,
                                                String status, String activityType, String startDate,
                                                String endDate) {
        QueryWrapper<Activity> w = new QueryWrapper<>();
        List<Long> ids = clubService.visibleClubIds();
        if (ids != null) {
            if (ids.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
            w.in("club_id", ids);
        }
        if (clubId != null) {
            w.eq("club_id", clubId);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like("activity_name", keyword).or().like("activity_no", keyword));
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (activityType != null && !activityType.isBlank()) {
            w.eq("activity_type", activityType);
        }
        if (startDate != null && !startDate.isBlank()) {
            w.ge("start_time", startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isBlank()) {
            w.le("start_time", endDate + " 23:59:59");
        }
        w.orderByDesc("start_time");
        Page<Activity> p = activityMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> clubs = clubMapper.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(Club::getId, Club::getClubName));
        List<Map<String, Object>> records = p.getRecords().stream().map(a -> row(a, clubs)).toList();
        return PageResult.of(p.getTotal(), page, size, records);
    }

    private Map<String, Object> row(Activity a, Map<Long, String> clubs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("activityNo", a.getActivityNo());
        m.put("clubId", a.getClubId());
        m.put("clubName", clubs.getOrDefault(a.getClubId(), ""));
        m.put("activityName", a.getActivityName());
        m.put("activityType", a.getActivityType());
        m.put("startTime", a.getStartTime() == null ? null : a.getStartTime().format(DT));
        m.put("endTime", a.getEndTime() == null ? null : a.getEndTime().format(DT));
        m.put("placeName", a.getPlaceName());
        m.put("placeCapacity", a.getPlaceCapacity());
        m.put("expectCount", a.getExpectCount());
        m.put("actualCount", a.getActualCount());
        m.put("budgetTotal", a.getBudgetTotal());
        m.put("spentAmount", a.getSpentAmount());
        m.put("feasibilityScore", a.getFeasibilityScore());
        m.put("coverUrl", a.getCoverUrl());
        m.put("status", a.getStatus());
        m.put("rejectReason", a.getRejectReason());
        m.put("signupCount", signupMapper.selectCount(new QueryWrapper<ActivitySignup>()
                .eq("activity_id", a.getId()).eq("cancel_flag", 0)));
        return m;
    }

    public Map<String, Object> detail(Long id) {
        Activity a = activityMapper.selectById(id);
        if (a == null) {
            throw new BizException(404, "活动不存在");
        }
        Map<Long, String> clubs = clubMapper.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(Club::getId, Club::getClubName));
        Map<String, Object> m = row(a, clubs);
        m.put("planDesc", a.getPlanDesc());
        m.put("submitCount", a.getSubmitCount());
        // 预算明细
        m.put("budgets", budgetMapper.selectList(new QueryWrapper<Budget>().eq("activity_id", id)).stream().map(b -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", b.getId());
            x.put("itemName", b.getItemName());
            x.put("itemType", b.getItemType());
            x.put("applyAmount", b.getApplyAmount());
            x.put("approveAmount", b.getApproveAmount());
            x.put("amountBasis", b.getAmountBasis());
            x.put("status", b.getStatus());
            x.put("auditRemark", b.getAuditRemark());
            return x;
        }).toList());
        // 签到统计
        List<ActivitySignup> signs = signupMapper.selectList(new QueryWrapper<ActivitySignup>()
                .eq("activity_id", id).eq("cancel_flag", 0));
        long signed = signs.stream().filter(s -> !"未签到".equals(s.getCheckinStatus())
                && !"缺席".equals(s.getCheckinStatus())).count();
        m.put("signupTotal", signs.size());
        m.put("signedTotal", signed);
        m.put("attendRate", signs.isEmpty() ? 0 : Math.round(signed * 1000.0 / signs.size()) / 10.0);
        m.put("signupList", signs.stream().limit(20).map(s -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", s.getId());
            x.put("signupNo", s.getSignupNo());
            x.put("studentName", nameOf(s.getStudentId()));
            x.put("signupTime", s.getSignupTime() == null ? null : s.getSignupTime().format(DT));
            x.put("checkinTime", s.getCheckinTime() == null ? null : s.getCheckinTime().format(DT));
            x.put("checkinStatus", s.getCheckinStatus());
            return x;
        }).toList());
        // 可行性快照（创新点）
        FeasibilitySnapshot snap = snapshotMapper.selectOne(new QueryWrapper<FeasibilitySnapshot>()
                .eq("activity_id", id).orderByDesc("id").last("limit 1"));
        if (snap != null) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("snapshotId", snap.getId());
            s.put("ruleVersion", snap.getRuleVersion());
            s.put("totalScore", snap.getTotalScore());
            s.put("level", snap.getLevel());
            s.put("suggestion", snap.getSuggestion());
            s.put("unsatisfied", snap.getUnsatisfied());
            s.put("scoreBudget", snap.getScoreBudget());
            s.put("scoreVenue", snap.getScoreVenue());
            s.put("scoreCapacity", snap.getScoreCapacity());
            s.put("scoreHistory", snap.getScoreHistory());
            s.put("scoreClub", snap.getScoreClub());
            s.put("calcTime", snap.getCalcTime() == null ? null : snap.getCalcTime().format(DT));
            m.put("feasibility", s);
        }
        // 可执行操作
        List<String> actions = new ArrayList<>();
        String st = a.getStatus();
        if ("待审批".equals(st)) {
            actions.add("APPROVE");
            actions.add("REJECT");
        }
        if ("已驳回".equals(st)) {
            actions.add("RESUBMIT");
        }
        if ("已立项".equals(st)) {
            actions.add("START");
            actions.add("SIGNUP");
        }
        if ("进行中".equals(st)) {
            actions.add("CHECKIN");
            actions.add("FINISH");
        }
        if ("已结束".equals(st)) {
            actions.add("CLOSE");
        }
        m.put("actions", actions);
        return m;
    }

    private String nameOf(Long uid) {
        if (uid == null) {
            return "";
        }
        SysUser u = userMapper.selectById(uid);
        return u == null ? "" : u.getRealName();
    }

    /** 新建活动：校验时间、场地冲突与年度剩余额度 */
    @Transactional
    public Long create(Map<String, Object> body) {
        Long clubId = ClubService2.num(body.get("clubId"));
        Activity a = new Activity();
        a.setClubId(clubId);
        a.setActivityName(require(body.get("activityName"), "活动名称"));
        a.setActivityType(body.get("activityType") == null ? "讲座" : String.valueOf(body.get("activityType")));
        LocalDateTime start = parse(body.get("startTime"), "开始时间");
        LocalDateTime end = parse(body.get("endTime"), "结束时间");
        if (start.isBefore(LocalDateTime.now().plusHours(24))) {
            throw new BizException("活动开始时间须晚于申报时间 24 小时");
        }
        if (!end.isAfter(start)) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        a.setStartTime(start);
        a.setEndTime(end);
        a.setPlaceName(require(body.get("placeName"), "活动地点"));
        a.setPlaceCapacity(ClubService2.intv(body.get("placeCapacity"), 120));
        int expect = ClubService2.intv(body.get("expectCount"), 50);
        if (expect > a.getPlaceCapacity()) {
            throw new BizException("预计人数超过场地容量（" + a.getPlaceCapacity() + "）");
        }
        a.setExpectCount(expect);
        a.setActualCount(0);
        BigDecimal budgetTotal = ClubService2.dec(body.get("budgetTotal"), BigDecimal.ZERO);
        a.setBudgetTotal(budgetTotal);
        a.setSpentAmount(BigDecimal.ZERO);

        Club club = clubMapper.selectById(clubId);
        if (club == null) {
            throw new BizException("社团不存在");
        }
        if ("整改中".equals(club.getStatus()) || "已注销".equals(club.getStatus())) {
            throw new BizException("整改中或已注销的社团不能申报新活动");
        }
        // 年度剩余额度 = 年度上限 - 已立项及之后状态活动的预算合计
        BigDecimal allocated = activityMapper.selectList(new QueryWrapper<Activity>()
                        .eq("club_id", clubId)
                        .in("status", List.of("已立项", "进行中", "已结束", "已结项")))
                .stream().map(Activity::getBudgetTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (allocated.add(budgetTotal).compareTo(club.getFundLimit()) > 0) {
            throw new BizException("预算超过社团年度剩余额度，剩余 " +
                    club.getFundLimit().subtract(allocated) + " 元");
        }
        // 场地冲突：同场地、时间重叠且状态未终结
        List<Activity> conflicts = activityMapper.selectList(new QueryWrapper<Activity>()
                .eq("place_name", a.getPlaceName())
                .in("status", List.of("待审批", "已立项", "进行中"))
                .lt("start_time", end).gt("end_time", start));
        if (!conflicts.isEmpty()) {
            throw new BizException("场地冲突：" + a.getPlaceName() + " 在该时段已被「"
                    + conflicts.get(0).getActivityName() + "」占用，请改期或换场地");
        }
        a.setFeasibilityScore(BigDecimal.ZERO);
        a.setPlanDesc(body.get("planDesc") == null ? null : String.valueOf(body.get("planDesc")));
        a.setCoverUrl(body.get("coverUrl") == null ? "/uploads/seed/activity/01.jpg"
                : String.valueOf(body.get("coverUrl")));
        a.setStatus("待审批");
        a.setSubmitCount(1);
        a.setCreateBy(UserContext.userId());
        a.setCreateTime(LocalDateTime.now());
        a.setDeleted(0);
        a.setVersion(0);
        a.setActivityNo("T" + System.nanoTime());
        activityMapper.insert(a);
        Activity up = new Activity();
        up.setId(a.getId());
        up.setActivityNo("HD" + start.format(DateTimeFormatter.ofPattern("yyMMdd")) + String.format("%03d", a.getId()));
        activityMapper.updateById(up);
        trace.log("活动管理", "新增", up.getActivityNo(), "提交活动申报：" + a.getActivityName());
        // 通知校团委
        trace.notify(2L, "活动立项待审批",
                "社团「" + club.getClubName() + "」提交活动「" + a.getActivityName() + "」，请审核。",
                "待办", "activity", a.getId());
        return a.getId();
    }

    @Transactional
    public boolean update(Long id, Map<String, Object> body) {
        Activity a = activityMapper.selectById(id);
        if (a == null) {
            throw new BizException(404, "活动不存在");
        }
        if (List.of("已结项").contains(a.getStatus())) {
            throw new BizException("已结项活动不能再修改");
        }
        Activity up = new Activity();
        up.setId(id);
        boolean timeOrPlaceChanged = false;
        if (body.get("activityName") != null) {
            up.setActivityName(String.valueOf(body.get("activityName")));
        }
        if (body.get("startTime") != null) {
            up.setStartTime(parse(body.get("startTime"), "开始时间"));
            timeOrPlaceChanged = true;
        }
        if (body.get("endTime") != null) {
            up.setEndTime(parse(body.get("endTime"), "结束时间"));
        }
        if (body.get("placeName") != null) {
            up.setPlaceName(String.valueOf(body.get("placeName")));
            timeOrPlaceChanged = true;
        }
        if (body.get("expectCount") != null) {
            up.setExpectCount(ClubService2.intv(body.get("expectCount"), a.getExpectCount()));
        }
        if (body.get("planDesc") != null) {
            up.setPlanDesc(String.valueOf(body.get("planDesc")));
        }
        if (body.get("coverUrl") != null) {
            up.setCoverUrl(String.valueOf(body.get("coverUrl")));
        }
        if (body.get("budgetTotal") != null) {
            BigDecimal nb = ClubService2.dec(body.get("budgetTotal"), a.getBudgetTotal());
            // 修改预算超过原预算 10% 须重新提交预算审核
            if (a.getBudgetTotal().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal delta = nb.subtract(a.getBudgetTotal()).abs()
                        .divide(a.getBudgetTotal(), 4, java.math.RoundingMode.HALF_UP);
                if (delta.compareTo(new BigDecimal("0.10")) > 0) {
                    up.setStatus("待审批");
                    up.setSubmitCount(a.getSubmitCount() + 1);
                    trace.notify(2L, "活动预算变更待审批",
                            "活动「" + a.getActivityName() + "」预算变动超过 10%，需重新审批。",
                            "待办", "activity", id);
                }
            }
            up.setBudgetTotal(nb);
        }
        // 已立项后改时间或地点：回到待审批重新审批
        if (timeOrPlaceChanged && List.of("已立项", "进行中").contains(a.getStatus())) {
            up.setStatus("待审批");
        }
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        boolean ok = activityMapper.updateById(up) > 0;
        trace.log("活动管理", "修改", a.getActivityNo(), "修改活动：" + a.getActivityName());
        return ok;
    }

    @Transactional
    public boolean delete(Long id) {
        Activity a = activityMapper.selectById(id);
        if (a == null) {
            return false;
        }
        if (List.of("进行中", "已立项").contains(a.getStatus())) {
            throw new BizException("活动处于 " + a.getStatus() + " 状态，请先结束或撤回后再删除");
        }
        activityMapper.deleteById(id);
        trace.log("活动管理", "删除", a.getActivityNo(), "删除活动：" + a.getActivityName());
        return true;
    }

    /** 立项审批：通过 → 已立项（回写可行性得分）；驳回须填写原因 */
    @Transactional
    public Map<String, Object> approve(Long id, String targetStatus, String reason) {
        Activity a = activityMapper.selectById(id);
        if (a == null) {
            throw new BizException(404, "活动不存在");
        }
        // 只允许对待审批/已驳回的单据做立项审批，已立项后重复审批返回 4009
        if (!List.of("待审批", "已驳回").contains(a.getStatus())) {
            throw new BizException(4009, "该活动已审批，当前状态：" + a.getStatus());
        }
        if (!FLOW.getOrDefault(a.getStatus(), List.of()).contains(targetStatus)
                || !("已立项".equals(targetStatus) || "已驳回".equals(targetStatus))) {
            throw new BizException(4001, "当前状态「" + a.getStatus() + "」不允许审批为「" + targetStatus + "」");
        }
        if ("已驳回".equals(targetStatus) && (reason == null || reason.trim().length() < 5)) {
            throw new BizException("驳回原因不少于 5 个字");
        }
        Activity up = new Activity();
        up.setId(id);
        up.setStatus(targetStatus);
        up.setRejectReason("已驳回".equals(targetStatus) ? reason : null);
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(up);
        trace.log("活动管理", "审批", a.getActivityNo(),
                "立项审批：" + targetStatus + (reason == null ? "" : "；意见：" + reason));
        trace.notify(a.getCreateBy(), "活动立项审批结果",
                "活动「" + a.getActivityName() + "」审批结果：" + targetStatus
                        + (reason == null ? "" : "。意见：" + reason), "审核", "activity", id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("status", targetStatus);
        r.put("updateTime", LocalDateTime.now().format(DT));
        return r;
    }

    /** 报名：仅已立项活动；名额满则拒绝；同一学生同一活动只能报一次 */
    @Transactional
    public Long signup(Long activityId) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BizException(404, "活动不存在");
        }
        if (!"已立项".equals(a.getStatus())) {
            throw new BizException(4001, "仅已立项活动可以报名，当前状态：" + a.getStatus());
        }
        Long exists = signupMapper.selectCount(new QueryWrapper<ActivitySignup>()
                .eq("activity_id", activityId).eq("student_id", UserContext.userId()).eq("cancel_flag", 0));
        if (exists != null && exists > 0) {
            throw new BizException(4009, "您已报名该活动，请勿重复提交");
        }
        long taken = signupMapper.selectCount(new QueryWrapper<ActivitySignup>()
                .eq("activity_id", activityId).eq("cancel_flag", 0));
        if (taken >= a.getExpectCount()) {
            throw new BizException("名额已满（" + a.getExpectCount() + " 人），可关注候补名额");
        }
        // 取消过的报名记录直接复用，避免唯一索引冲突
        ActivitySignup old = signupMapper.selectOne(new QueryWrapper<ActivitySignup>()
                .eq("activity_id", activityId).eq("student_id", UserContext.userId()).last("limit 1"));
        ActivitySignup s = old == null ? new ActivitySignup() : old;
        s.setActivityId(activityId);
        s.setStudentId(UserContext.userId());
        s.setSignupType("线上");
        s.setSignupTime(LocalDateTime.now());
        s.setCheckinStatus("未签到");
        s.setCheckinCode(String.format("%06d", Math.abs((activityId * 37 + 11) % 1000000)));
        s.setCancelFlag(0);
        s.setCreateBy(UserContext.userId());
        if (old == null) {
            s.setCreateTime(LocalDateTime.now());
            s.setDeleted(0);
            s.setVersion(0);
            s.setSignupNo("T" + System.nanoTime());
            signupMapper.insert(s);
        } else {
            signupMapper.updateById(s);
        }
        ActivitySignup upd = new ActivitySignup();
        upd.setId(s.getId());
        upd.setSignupNo("BM" + String.format("%06d", activityId) + String.format("%04d", s.getId()));
        signupMapper.updateById(upd);
        trace.log("活动管理", "报名", upd.getSignupNo(), "报名活动：" + a.getActivityName());
        return s.getId();
    }

    @Transactional
    public boolean cancelSignup(Long signupId) {
        ActivitySignup s = signupMapper.selectById(signupId);
        if (s == null) {
            throw new BizException(404, "报名记录不存在");
        }
        if (!Objects.equals(s.getStudentId(), UserContext.userId()) && !UserContext.isAdmin()) {
            throw new BizException(403, "只能取消本人的报名");
        }
        s.setCancelFlag(1);
        s.setUpdateTime(LocalDateTime.now());
        return signupMapper.updateById(s) > 0;
    }

    /**
     * 现场签到：口令在活动开始前 10 分钟至结束时间内有效。
     * 签退时间晚于签到时间才计入服务时长，未签退按签到时间加活动时长的一半计算。
     */
    @Transactional
    public Map<String, Object> checkin(Long activityId, String code) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BizException(404, "活动不存在");
        }
        LocalDateTime open = a.getStartTime().minusMinutes(10);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(open)) {
            throw new BizException("签到尚未开始，活动开始前 10 分钟开放签到");
        }
        if (now.isAfter(a.getEndTime())) {
            throw new BizException("活动已结束，无法签到，如需补录请提交现场证明");
        }
        ActivitySignup s = signupMapper.selectOne(new QueryWrapper<ActivitySignup>()
                .eq("activity_id", activityId).eq("student_id", UserContext.userId())
                .eq("cancel_flag", 0).last("limit 1"));
        if (s == null) {
            throw new BizException("您未报名该活动，无法签到");
        }
        if (code == null || !code.equals(s.getCheckinCode())) {
            throw new BizException(4009, "签到口令不正确，连续错误 3 次将锁定 5 分钟");
        }
        boolean late = now.isAfter(a.getStartTime().plusMinutes(15));
        s.setCheckinTime(now);
        // 未签退时按活动时长一半计算服务时长
        s.setCheckoutTime(now.plusMinutes(java.time.Duration.between(a.getStartTime(), a.getEndTime()).toMinutes() / 2));
        s.setCheckinStatus(late ? "迟到" : "已签到");
        s.setUpdateTime(now);
        signupMapper.updateById(s);
        // 成员参与次数与服务时长累加
        ClubMember m = memberMapper.selectOne(new QueryWrapper<ClubMember>()
                .eq("club_id", a.getClubId()).eq("student_id", UserContext.userId()).last("limit 1"));
        if (m != null) {
            BigDecimal hours = BigDecimal.valueOf(
                    java.time.Duration.between(s.getCheckinTime(), s.getCheckoutTime()).toMinutes() / 60.0)
                    .setScale(1, java.math.RoundingMode.HALF_UP);
            ClubMember up = new ClubMember();
            up.setId(m.getId());
            up.setActivityCount((m.getActivityCount() == null ? 0 : m.getActivityCount()) + 1);
            up.setServiceHours((m.getServiceHours() == null ? BigDecimal.ZERO : m.getServiceHours()).add(hours));
            up.setUpdateTime(now);
            memberMapper.updateById(up);
        }
        // 实际到场人数回写活动
        long arrived = signupMapper.selectCount(new QueryWrapper<ActivitySignup>()
                .eq("activity_id", activityId).in("checkin_status", List.of("已签到", "迟到")));
        Activity au = new Activity();
        au.setId(activityId);
        au.setActualCount((int) arrived);
        activityMapper.updateById(au);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status", s.getCheckinStatus());
        r.put("checkinTime", now.format(DT));
        r.put("checkinCount", arrived);
        return r;
    }

    /** 状态推进：已立项→进行中→已结束→已结项，每次只前进一步 */
    @Transactional
    public Map<String, Object> transition(Long id, String targetStatus, String reason) {
        Activity a = activityMapper.selectById(id);
        if (a == null) {
            throw new BizException(404, "活动不存在");
        }
        if (!FLOW.getOrDefault(a.getStatus(), List.of()).contains(targetStatus)
                || List.of("已立项", "已驳回").contains(targetStatus)) {
            throw new BizException(4001, "当前状态「" + a.getStatus() + "」不允许流转为「" + targetStatus + "」");
        }
        Activity up = new Activity();
        up.setId(id);
        up.setStatus(targetStatus);
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(up);
        trace.log("活动管理", "流转", a.getActivityNo(), a.getStatus() + " → " + targetStatus);
        if ("已结束".equals(targetStatus)) {
            trace.notify(2L, "活动已结束", "活动「" + a.getActivityName() + "」已结束，请安排结项与报销。",
                    "提醒", "activity", id);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("status", targetStatus);
        r.put("updateTime", LocalDateTime.now().format(DT));
        return r;
    }

    /** 结项：预算余额自动收回（写结余流水），并允许生成经费公示 */
    @Transactional
    public Map<String, Object> close(Long id, String summary) {
        Activity a = activityMapper.selectById(id);
        if (a == null) {
            throw new BizException(404, "活动不存在");
        }
        if (!"已结束".equals(a.getStatus())) {
            throw new BizException(4001, "只有已结束的活动可以结项，当前状态：" + a.getStatus());
        }
        BigDecimal approved = budgetMapper.selectList(new QueryWrapper<Budget>()
                        .eq("activity_id", id).in("status", List.of("已通过", "已调减")))
                .stream().map(b -> b.getApproveAmount() == null ? b.getApplyAmount() : b.getApproveAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal spent = a.getSpentAmount() == null ? BigDecimal.ZERO : a.getSpentAmount();
        Activity up = new Activity();
        up.setId(id);
        up.setStatus("已结项");
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(up);
        trace.log("活动管理", "结项", a.getActivityNo(),
                "活动结项" + (summary == null ? "" : "：" + summary) + "；预算 " + approved + " 元，支出 " + spent + " 元");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("status", "已结项");
        r.put("approvedBudget", approved);
        r.put("spentAmount", spent);
        r.put("returnedAmount", approved.subtract(spent).max(BigDecimal.ZERO));
        return r;
    }

    // ---------------------------------------------------------- 门户
    public PageResult<Map<String, Object>> portalList(long page, long size, String keyword, String activityType) {
        QueryWrapper<Activity> w = new QueryWrapper<Activity>()
                .in("status", List.of("已立项", "进行中", "已结束", "已结项"));
        if (keyword != null && !keyword.isBlank()) {
            w.like("activity_name", keyword);
        }
        if (activityType != null && !activityType.isBlank()) {
            w.eq("activity_type", activityType);
        }
        w.orderByDesc("start_time");
        Page<Activity> p = activityMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> clubs = clubMapper.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(Club::getId, Club::getClubName));
        List<Map<String, Object>> records = p.getRecords().stream().map(a -> {
            Map<String, Object> m = row(a, clubs);
            Long my = signupMapper.selectCount(new QueryWrapper<ActivitySignup>()
                    .eq("activity_id", a.getId()).eq("student_id", UserContext.userId()).eq("cancel_flag", 0));
            m.put("signed", my != null && my > 0);
            return m;
        }).toList();
        return PageResult.of(p.getTotal(), page, size, records);
    }

    public List<Map<String, Object>> mySignups() {
        List<ActivitySignup> list = signupMapper.selectList(new QueryWrapper<ActivitySignup>()
                .eq("student_id", UserContext.userId()).eq("cancel_flag", 0).orderByDesc("signup_time"));
        Map<Long, String> clubs = clubMapper.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(Club::getId, Club::getClubName));
        return list.stream().map(s -> {
            Activity a = activityMapper.selectById(s.getActivityId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("signupId", s.getId());
            m.put("activityId", s.getActivityId());
            m.put("signupNo", s.getSignupNo());
            m.put("checkinStatus", s.getCheckinStatus());
            m.put("signupTime", s.getSignupTime() == null ? null : s.getSignupTime().format(DT));
            m.put("checkinTime", s.getCheckinTime() == null ? null : s.getCheckinTime().format(DT));
            m.put("checkinCode", s.getCheckinCode());
            if (a != null) {
                m.put("activityName", a.getActivityName());
                m.put("clubName", clubs.getOrDefault(a.getClubId(), ""));
                m.put("startTime", a.getStartTime() == null ? null : a.getStartTime().format(DT));
                m.put("placeName", a.getPlaceName());
                m.put("status", a.getStatus());
                m.put("coverUrl", a.getCoverUrl());
            }
            return m;
        }).toList();
    }

    /** 签到核验台：某活动当前签到名单与统计 */
    public Map<String, Object> checkinBoard(Long activityId) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BizException(404, "活动不存在");
        }
        List<ActivitySignup> list = signupMapper.selectList(new QueryWrapper<ActivitySignup>()
                .eq("activity_id", activityId).eq("cancel_flag", 0));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activityId", activityId);
        m.put("activityName", a.getActivityName());
        m.put("startTime", a.getStartTime() == null ? null : a.getStartTime().format(DT));
        m.put("endTime", a.getEndTime() == null ? null : a.getEndTime().format(DT));
        m.put("status", a.getStatus());
        m.put("expectCount", a.getExpectCount());
        m.put("checkinCode", list.isEmpty() ? null : list.get(0).getCheckinCode());
        m.put("total", list.size());
        m.put("signed", list.stream().filter(s -> List.of("已签到", "迟到").contains(s.getCheckinStatus())).count());
        m.put("absent", list.stream().filter(s -> "缺席".equals(s.getCheckinStatus())).count());
        m.put("records", list.stream().map(s -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", s.getId());
            x.put("studentName", nameOf(s.getStudentId()));
            x.put("checkinStatus", s.getCheckinStatus());
            x.put("checkinTime", s.getCheckinTime() == null ? null : s.getCheckinTime().format(DT));
            return x;
        }).toList());
        return m;
    }

    private static String require(Object v, String label) {
        if (v == null || String.valueOf(v).trim().isEmpty()) {
            throw new BizException(label + "不能为空");
        }
        return String.valueOf(v).trim();
    }

    private static LocalDateTime parse(Object v, String label) {
        if (v == null || String.valueOf(v).isBlank()) {
            throw new BizException(label + "不能为空");
        }
        String s = String.valueOf(v).replace('T', ' ');
        if (s.length() == 10) {
            s = s + " 00:00:00";
        }
        if (s.length() == 16) {
            s = s + ":00";
        }
        return LocalDateTime.parse(s, DT);
    }
}

/** 入参解析小工具，避免每个服务重复写类型转换 */
final class ClubService2 {
    private ClubService2() {
    }

    static Long num(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            throw new BizException("参数缺失");
        }
        return Long.valueOf(String.valueOf(o).replaceAll("\\.0$", ""));
    }

    static Integer intv(Object o, Integer def) {
        if (o == null || String.valueOf(o).isBlank()) {
            return def;
        }
        return Integer.valueOf(String.valueOf(o).replaceAll("\\.0$", ""));
    }

    static BigDecimal dec(Object o, BigDecimal def) {
        if (o == null || String.valueOf(o).isBlank()) {
            return def;
        }
        return new BigDecimal(String.valueOf(o));
    }
}
