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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 社团与成员：注册、年审、名册、干部任免。
 *
 * 数据范围：
 * - ADMIN / LEAGUE：全部社团
 * - LEADER：本人担任社长的社团
 * - MEMBER：本人加入的社团（只读）
 */
@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubMapper clubMapper;
    private final ClubMemberMapper memberMapper;
    private final ActivityMapper activityMapper;
    private final FundDisclosureMapper disclosureMapper;
    private final FundFlowMapper flowMapper;
    private final SysUserMapper userMapper;
    private final TraceService trace;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 当前登录人可见的社团 id；null 表示全部 */
    public List<Long> visibleClubIds() {
        if (UserContext.scopeAll()) {
            return null;
        }
        if (UserContext.isLeader()) {
            return clubMapper.selectList(new QueryWrapper<Club>().eq("president_id", UserContext.userId()))
                    .stream().map(Club::getId).toList();
        }
        // 成员：本人加入的社团
        return memberMapper.selectList(new QueryWrapper<ClubMember>()
                        .eq("student_id", UserContext.userId()).eq("status", "在册"))
                .stream().map(ClubMember::getClubId).distinct().toList();
    }

    // ------------------------------------------------------------ 社团
    public PageResult<Map<String, Object>> pageClub(long page, long size, String keyword, String status,
                                                    String clubType) {
        QueryWrapper<Club> w = new QueryWrapper<>();
        List<Long> ids = visibleClubIds();
        if (ids != null) {
            if (ids.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
            w.in("id", ids);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like("club_name", keyword).or().like("club_no", keyword));
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (clubType != null && !clubType.isBlank()) {
            w.eq("club_type", clubType);
        }
        w.orderByDesc("star_level").orderByDesc("create_time");
        Page<Club> p = clubMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> records = p.getRecords().stream().map(this::clubRow).toList();
        return PageResult.of(p.getTotal(), page, size, records);
    }

    private Map<String, Object> clubRow(Club c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("clubNo", c.getClubNo());
        m.put("clubName", c.getClubName());
        m.put("clubType", c.getClubType());
        m.put("foundedDate", c.getFoundedDate() == null ? null : c.getFoundedDate().toString());
        m.put("memberCount", c.getMemberCount());
        m.put("fundLimit", c.getFundLimit());
        m.put("starLevel", c.getStarLevel());
        m.put("status", c.getStatus());
        m.put("reviewYear", c.getReviewYear());
        m.put("reviewRemark", c.getReviewRemark());
        m.put("coverUrl", c.getCoverUrl());
        m.put("advisorName", nameOf(c.getAdvisorId()));
        m.put("presidentName", nameOf(c.getPresidentId()));
        m.put("balance", balanceOf(c.getId()));
        return m;
    }

    /** 社团余额 = 流水收入合计 - 流水支出合计 */
    public BigDecimal balanceOf(Long clubId) {
        List<FundFlow> flows = flowMapper.selectList(new QueryWrapper<FundFlow>().eq("club_id", clubId));
        BigDecimal in = flows.stream().filter(f -> "收入".equals(f.getDirection()))
                .map(FundFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal out = flows.stream().filter(f -> "支出".equals(f.getDirection()))
                .map(FundFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return in.subtract(out).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public String nameOf(Long userId) {
        if (userId == null) {
            return "";
        }
        SysUser u = userMapper.selectById(userId);
        return u == null ? "" : u.getRealName();
    }

    public Map<String, Object> clubDetail(Long id) {
        Club c = clubMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "社团不存在");
        }
        Map<String, Object> m = clubRow(c);
        m.put("advisorId", c.getAdvisorId());
        m.put("presidentId", c.getPresidentId());
        List<ClubMember> ms = memberMapper.selectList(new QueryWrapper<ClubMember>()
                .eq("club_id", id).eq("status", "在册"));
        m.put("activeMemberCount", ms.size());
        Map<String, Long> byRole = ms.stream().collect(Collectors.groupingBy(ClubMember::getMemberRole, Collectors.counting()));
        m.put("roleDistribution", byRole);
        Map<String, Long> byDept = ms.stream().collect(Collectors.groupingBy(ClubMember::getDeptName, Collectors.counting()));
        m.put("deptDistribution", byDept);
        m.put("recentActivities", activityMapper.selectList(new QueryWrapper<Activity>()
                        .eq("club_id", id).orderByDesc("start_time").last("limit 5"))
                .stream().map(a -> {
                    Map<String, Object> x = new LinkedHashMap<>();
                    x.put("id", a.getId());
                    x.put("activityName", a.getActivityName());
                    x.put("startTime", a.getStartTime() == null ? null : a.getStartTime().format(DT));
                    x.put("status", a.getStatus());
                    x.put("feasibilityScore", a.getFeasibilityScore());
                    return x;
                }).toList());
        m.put("disclosures", disclosureMapper.selectList(new QueryWrapper<FundDisclosure>()
                        .eq("club_id", id).orderByDesc("publish_start").last("limit 3"))
                .stream().map(d -> {
                    Map<String, Object> x = new LinkedHashMap<>();
                    x.put("id", d.getId());
                    x.put("period", d.getPeriod());
                    x.put("incomeAmount", d.getIncomeAmount());
                    x.put("expenseAmount", d.getExpenseAmount());
                    x.put("balanceAmount", d.getBalanceAmount());
                    x.put("status", d.getStatus());
                    return x;
                }).toList());
        return m;
    }

    @Transactional
    public Long createClub(Map<String, Object> body) {
        String name = str(body.get("clubName"));
        if (name == null || name.length() < 2) {
            throw new BizException("社团名称不少于 2 个字");
        }
        Long dup = clubMapper.selectCount(new QueryWrapper<Club>().eq("club_name", name));
        if (dup != null && dup > 0) {
            throw new BizException("社团名称已存在，请更换名称");
        }
        Club c = new Club();
        c.setClubName(name);
        c.setClubType(str(body.get("clubType")) == null ? "学术科技" : str(body.get("clubType")));
        c.setFoundedDate(parseDate(body.get("foundedDate")));
        c.setAdvisorId(num(body.get("advisorId")));
        c.setPresidentId(num(body.get("presidentId")));
        c.setMemberCount(0);
        c.setFundLimit(dec(body.get("fundLimit"), new BigDecimal("20000")));
        c.setStarLevel(intv(body.get("starLevel"), 3));
        c.setReviewYear(intv(body.get("reviewYear"), LocalDate.now().getYear()));
        c.setStatus("待注册");
        c.setCoverUrl(str(body.get("coverUrl")) == null ? "/uploads/seed/club/01.jpg" : str(body.get("coverUrl")));
        c.setCreateBy(UserContext.userId());
        c.setCreateTime(LocalDateTime.now());
        c.setDeleted(0);
        c.setVersion(0);
        c.setClubNo("T" + System.nanoTime());
        clubMapper.insert(c);
        // 业务编号：ST + 2 位类别码 + 3 位序号
        Map<String, String> code = Map.of("学术科技", "01", "文艺体育", "02", "公益实践", "03", "创新创业", "04");
        Club up = new Club();
        up.setId(c.getId());
        up.setClubNo("ST" + code.getOrDefault(c.getClubType(), "01") + String.format("%03d", c.getId()));
        clubMapper.updateById(up);
        trace.log("社团管理", "新增", up.getClubNo(), "提交社团注册：" + name);
        return c.getId();
    }

    @Transactional
    public boolean updateClub(Long id, Map<String, Object> body) {
        Club c = clubMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "社团不存在");
        }
        if (UserContext.isLeader() && !Objects.equals(c.getPresidentId(), UserContext.userId())) {
            throw new BizException(403, "只能维护本人担任社长的社团");
        }
        Club up = new Club();
        up.setId(id);
        if (body.get("clubName") != null) {
            up.setClubName(str(body.get("clubName")));
        }
        if (body.get("clubType") != null) {
            up.setClubType(str(body.get("clubType")));
        }
        if (body.get("fundLimit") != null) {
            up.setFundLimit(dec(body.get("fundLimit"), c.getFundLimit()));
        }
        if (body.get("starLevel") != null) {
            up.setStarLevel(intv(body.get("starLevel"), c.getStarLevel()));
        }
        if (body.get("coverUrl") != null) {
            up.setCoverUrl(str(body.get("coverUrl")));
        }
        if (body.get("advisorId") != null) {
            up.setAdvisorId(num(body.get("advisorId")));
        }
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        boolean ok = clubMapper.updateById(up) > 0;
        trace.log("社团管理", "修改", c.getClubNo(), "更新社团资料：" + c.getClubName());
        return ok;
    }

    @Transactional
    public boolean deleteClub(Long id) {
        Club c = clubMapper.selectById(id);
        if (c == null) {
            return false;
        }
        Long running = activityMapper.selectCount(new QueryWrapper<Activity>()
                .eq("club_id", id).in("status", List.of("已立项", "进行中")));
        if (running != null && running > 0) {
            throw new BizException("该社团存在进行中的活动，请先结项后再删除");
        }
        clubMapper.deleteById(id);
        trace.log("社团管理", "删除", c.getClubNo(), "删除社团：" + c.getClubName());
        return true;
    }

    /**
     * 校团委年审：通过 → 正常；整改 → 整改中；注销 → 已注销。
     * 通过时星级与经费上限同步更新；整改须填写期限。
     */
    @Transactional
    public Map<String, Object> annualReview(Long id, String conclusion, String remark,
                                            Integer starLevel, BigDecimal fundLimit) {
        Club c = clubMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "社团不存在");
        }
        if (!List.of("通过", "整改", "注销").contains(conclusion)) {
            throw new BizException("年审结论只能是 通过 / 整改 / 注销");
        }
        if (!"通过".equals(conclusion) && (remark == null || remark.trim().length() < 5)) {
            throw new BizException("整改或注销必须填写不少于 5 个字的年审意见");
        }
        // 材料校验：在册成员不少于 10 人、有指导教师
        long active = memberMapper.selectCount(new QueryWrapper<ClubMember>()
                .eq("club_id", id).eq("status", "在册"));
        if ("通过".equals(conclusion) && active < 10) {
            throw new BizException("在册成员不足 10 人（当前 " + active + " 人），不能通过年审");
        }
        if ("通过".equals(conclusion) && c.getAdvisorId() == null) {
            throw new BizException("缺少指导教师，不能通过年审");
        }
        Club up = new Club();
        up.setId(id);
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        if ("通过".equals(conclusion)) {
            up.setStatus("正常");
            up.setReviewRemark("年审通过：" + (remark == null ? "材料齐全" : remark));
            if (starLevel != null) {
                up.setStarLevel(starLevel);
            }
            if (fundLimit != null) {
                up.setFundLimit(fundLimit);
            }
        } else if ("整改".equals(conclusion)) {
            up.setStatus("整改中");
            up.setReviewRemark("整改要求：" + remark);
        } else {
            up.setStatus("已注销");
            up.setReviewRemark("注销原因：" + remark);
        }
        clubMapper.updateById(up);
        trace.log("社团管理", "年审", c.getClubNo(), "年审结论：" + conclusion + "；" + remark);
        trace.notify(c.getPresidentId(), "社团年审结果",
                "社团「" + c.getClubName() + "」年审结论：" + conclusion + "。" + (remark == null ? "" : remark),
                "提醒", "club", id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("status", up.getStatus());
        return r;
    }

    public List<Map<String, Object>> clubOptions() {
        List<Long> ids = visibleClubIds();
        QueryWrapper<Club> w = new QueryWrapper<>();
        if (ids != null) {
            if (ids.isEmpty()) {
                return List.of();
            }
            w.in("id", ids);
        }
        return clubMapper.selectList(w.orderByAsc("club_name")).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("clubName", c.getClubName());
            m.put("clubType", c.getClubType());
            return m;
        }).toList();
    }

    // ------------------------------------------------------------ 成员
    public PageResult<Map<String, Object>> pageMember(long page, long size, Long clubId, String keyword,
                                                      String status, String memberRole, String deptName) {
        QueryWrapper<ClubMember> w = new QueryWrapper<>();
        List<Long> ids = visibleClubIds();
        if (ids != null) {
            if (ids.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
            w.in("club_id", ids);
        }
        if (clubId != null) {
            w.eq("club_id", clubId);
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (memberRole != null && !memberRole.isBlank()) {
            w.eq("member_role", memberRole);
        }
        if (deptName != null && !deptName.isBlank()) {
            w.like("dept_name", deptName);
        }
        w.orderByDesc("create_time");
        Page<ClubMember> p = memberMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> clubNames = clubMapper.selectList(null).stream()
                .collect(Collectors.toMap(Club::getId, Club::getClubName));
        List<Map<String, Object>> records = p.getRecords().stream().map(m -> {
            if (keyword != null && !keyword.isBlank()) {
                String real = nameOf(m.getStudentId());
                if (!real.contains(keyword) && !m.getMemberNo().contains(keyword)) {
                    return null;
                }
            }
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", m.getId());
            x.put("memberNo", m.getMemberNo());
            x.put("clubId", m.getClubId());
            x.put("clubName", clubNames.getOrDefault(m.getClubId(), ""));
            x.put("studentId", m.getStudentId());
            x.put("studentName", nameOf(m.getStudentId()));
            x.put("deptName", m.getDeptName());
            x.put("joinDate", m.getJoinDate() == null ? null : m.getJoinDate().toString());
            x.put("memberRole", m.getMemberRole());
            x.put("activityCount", m.getActivityCount());
            x.put("serviceHours", m.getServiceHours());
            x.put("status", m.getStatus());
            x.put("quitReason", m.getQuitReason());
            x.put("avatarUrl", m.getAvatarUrl());
            return x;
        }).filter(Objects::nonNull).toList();
        return PageResult.of(p.getTotal(), page, size, records);
    }

    /**
     * 干部任免：社长与财务干事不能为同一人；目标成员须在册且无未结清报销单。
     */
    @Transactional
    public boolean appoint(Long memberId, String memberRole, String remark) {
        ClubMember m = memberMapper.selectById(memberId);
        if (m == null) {
            throw new BizException(404, "成员不存在");
        }
        if (!"在册".equals(m.getStatus())) {
            throw new BizException("只有状态为在册的成员可以任免干部");
        }
        if (!List.of("成员", "干事", "部长", "副社长", "社长").contains(memberRole)) {
            throw new BizException("职务取值不合法");
        }
        // 社长独占：同社团已有社长时先卸任
        if ("社长".equals(memberRole)) {
            List<ClubMember> others = memberMapper.selectList(new QueryWrapper<ClubMember>()
                    .eq("club_id", m.getClubId()).eq("member_role", "社长").ne("id", memberId));
            for (ClubMember o : others) {
                ClubMember down = new ClubMember();
                down.setId(o.getId());
                down.setMemberRole("成员");
                down.setUpdateBy(UserContext.userId());
                down.setUpdateTime(LocalDateTime.now());
                memberMapper.updateById(down);
            }
            Club cu = new Club();
            cu.setId(m.getClubId());
            cu.setPresidentId(m.getStudentId());
            cu.setUpdateTime(LocalDateTime.now());
            clubMapper.updateById(cu);
        }
        ClubMember up = new ClubMember();
        up.setId(memberId);
        up.setMemberRole(memberRole);
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        boolean ok = memberMapper.updateById(up) > 0;
        trace.log("成员管理", "任免", m.getMemberNo(), "任命为 " + memberRole + (remark == null ? "" : "；" + remark));
        trace.notify(m.getStudentId(), "干部任免通知",
                "您在社团中的职务已调整为「" + memberRole + "」", "提醒", "member", m.getClubId());
        return ok;
    }

    /** 成员退出：社长确认后置为已退出，保留历史活动记录 */
    @Transactional
    public boolean quit(Long memberId, String reason) {
        ClubMember m = memberMapper.selectById(memberId);
        if (m == null) {
            throw new BizException(404, "成员不存在");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("退出原因不少于 5 个字");
        }
        ClubMember up = new ClubMember();
        up.setId(memberId);
        up.setStatus("已退出");
        up.setQuitReason(reason);
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        boolean ok = memberMapper.updateById(up) > 0;
        // 在册人数回写社团
        long active = memberMapper.selectCount(new QueryWrapper<ClubMember>()
                .eq("club_id", m.getClubId()).eq("status", "在册"));
        Club cu = new Club();
        cu.setId(m.getClubId());
        cu.setMemberCount((int) active);
        cu.setUpdateTime(LocalDateTime.now());
        clubMapper.updateById(cu);
        trace.log("成员管理", "退出", m.getMemberNo(), "成员退出：" + reason);
        return ok;
    }

    @Transactional
    public boolean auditMember(Long memberId, String status) {
        ClubMember m = memberMapper.selectById(memberId);
        if (m == null) {
            throw new BizException(404, "成员不存在");
        }
        ClubMember up = new ClubMember();
        up.setId(memberId);
        up.setStatus(status);
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        boolean ok = memberMapper.updateById(up) > 0;
        if ("在册".equals(status)) {
            long active = memberMapper.selectCount(new QueryWrapper<ClubMember>()
                    .eq("club_id", m.getClubId()).eq("status", "在册"));
            Club cu = new Club();
            cu.setId(m.getClubId());
            cu.setMemberCount((int) active);
            clubMapper.updateById(cu);
        }
        return ok;
    }

    /** 成员结构报表：院系占比 + 干部梯队 */
    public Map<String, Object> memberReport(Long clubId) {
        QueryWrapper<ClubMember> w = new QueryWrapper<ClubMember>().eq("status", "在册");
        if (clubId != null) {
            w.eq("club_id", clubId);
        }
        List<ClubMember> list = memberMapper.selectList(w);
        Map<String, Long> dept = list.stream().collect(Collectors.groupingBy(ClubMember::getDeptName, Collectors.counting()));
        Map<String, Long> role = list.stream().collect(Collectors.groupingBy(ClubMember::getMemberRole, Collectors.counting()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", list.size());
        m.put("deptDistribution", dept.entrySet().stream().map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());
        m.put("roleDistribution", role.entrySet().stream().map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());
        return m;
    }

    // ------------------------------------------------------------ 小工具
    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long num(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            return null;
        }
        return Long.valueOf(String.valueOf(o).replaceAll("\\.0$", ""));
    }

    private static Integer intv(Object o, Integer def) {
        if (o == null || String.valueOf(o).isBlank()) {
            return def;
        }
        return Integer.valueOf(String.valueOf(o).replaceAll("\\.0$", ""));
    }

    private static BigDecimal dec(Object o, BigDecimal def) {
        if (o == null || String.valueOf(o).isBlank()) {
            return def;
        }
        return new BigDecimal(String.valueOf(o));
    }

    private static LocalDate parseDate(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            return LocalDate.now();
        }
        String s = String.valueOf(o);
        return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
    }
}
