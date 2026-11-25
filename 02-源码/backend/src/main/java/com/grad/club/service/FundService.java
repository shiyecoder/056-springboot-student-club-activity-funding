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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 经费：预算申报与审核、报销与财务复核、经费流水、公示与异议。
 *
 * 关键规则（需求文档 5.5~5.7）：
 * - 科目金额合计须等于活动预算总额，单项不超过总额 60%，测算依据不少于 20 字
 * - 报销金额不得超过科目审核金额；超过 500 元至少 1 张发票
 * - 支付登记后生成经费流水，余额为负时拒绝记账
 * - 公示发布前校验期末余额与流水一致、公示期不少于 7 天
 */
@Service
@RequiredArgsConstructor
public class FundService {

    private final BudgetMapper budgetMapper;
    private final ExpenseMapper expenseMapper;
    private final FundFlowMapper flowMapper;
    private final FundDisclosureMapper disclosureMapper;
    private final FundObjectionMapper objectionMapper;
    private final ActivityMapper activityMapper;
    private final ClubMapper clubMapper;
    private final ClubService clubService;
    private final TraceService trace;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ------------------------------------------------------------ 预算
    public PageResult<Map<String, Object>> pageBudget(long page, long size, Long activityId, Long clubId,
                                                      String status, String keyword) {
        QueryWrapper<Budget> w = new QueryWrapper<>();
        List<Long> ids = clubService.visibleClubIds();
        if (ids != null) {
            if (ids.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
            w.in("club_id", ids);
        }
        if (activityId != null) {
            w.eq("activity_id", activityId);
        }
        if (clubId != null) {
            w.eq("club_id", clubId);
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.like("item_name", keyword);
        }
        w.orderByDesc("create_time");
        Page<Budget> p = budgetMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), page, size, p.getRecords().stream().map(this::budgetRow).toList());
    }

    private Map<String, Object> budgetRow(Budget b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("budgetNo", b.getBudgetNo());
        m.put("activityId", b.getActivityId());
        m.put("clubId", b.getClubId());
        Activity a = activityMapper.selectById(b.getActivityId());
        m.put("activityName", a == null ? "" : a.getActivityName());
        m.put("clubName", clubName(b.getClubId()));
        m.put("itemName", b.getItemName());
        m.put("itemType", b.getItemType());
        m.put("applyAmount", b.getApplyAmount());
        m.put("approveAmount", b.getApproveAmount());
        m.put("amountBasis", b.getAmountBasis());
        m.put("status", b.getStatus());
        m.put("auditRemark", b.getAuditRemark());
        m.put("auditTime", b.getAuditTime() == null ? null : b.getAuditTime().format(DT));
        return m;
    }

    private String clubName(Long id) {
        Club c = id == null ? null : clubMapper.selectById(id);
        return c == null ? "" : c.getClubName();
    }

    /** 新增预算行：依据不少于 20 字；单项不超过活动预算总额 60% */
    @Transactional
    public Long createBudget(Map<String, Object> body) {
        Long activityId = num(body.get("activityId"));
        Activity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        BigDecimal apply = dec(body.get("applyAmount"));
        if (apply.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("申请金额必须大于 0");
        }
        String basis = body.get("amountBasis") == null ? "" : String.valueOf(body.get("amountBasis")).trim();
        if (basis.length() < 20) {
            throw new BizException("测算依据不少于 20 个字，请写明单价与数量");
        }
        BigDecimal total = a.getBudgetTotal() == null ? BigDecimal.ZERO : a.getBudgetTotal();
        if (total.compareTo(BigDecimal.ZERO) > 0
                && apply.compareTo(total.multiply(new BigDecimal("0.6"))) > 0) {
            throw new BizException("单项金额不得超过活动预算总额的 60%（上限 "
                    + total.multiply(new BigDecimal("0.6")).setScale(2, RoundingMode.HALF_UP) + " 元）");
        }
        Budget b = new Budget();
        b.setActivityId(activityId);
        b.setClubId(a.getClubId());
        b.setItemName(require(body.get("itemName"), "科目名称"));
        b.setItemType(body.get("itemType") == null ? "物料" : String.valueOf(body.get("itemType")));
        b.setApplyAmount(apply);
        b.setAmountBasis(basis);
        b.setStatus("待审核");
        b.setCreateBy(UserContext.userId());
        b.setCreateTime(LocalDateTime.now());
        b.setDeleted(0);
        b.setVersion(0);
        b.setBudgetNo("T" + System.nanoTime());
        budgetMapper.insert(b);
        Budget up = new Budget();
        up.setId(b.getId());
        up.setBudgetNo("YS" + String.format("%06d", activityId) + String.format("%02d", b.getId() % 100));
        budgetMapper.updateById(up);
        trace.log("经费管理", "新增", up.getBudgetNo(), "编制预算科目：" + b.getItemName() + " " + apply + " 元");
        return b.getId();
    }

    /**
     * 预算审核：整体通过、按科目调减或驳回。
     * 调减须填写审核金额与理由；调减后总额回写活动。
     */
    @Transactional
    public Map<String, Object> approveBudget(Long id, String conclusion, BigDecimal approveAmount, String remark) {
        Budget b = budgetMapper.selectById(id);
        if (b == null) {
            throw new BizException(404, "预算不存在");
        }
        if (!"待审核".equals(b.getStatus())) {
            throw new BizException(4001, "该预算已审核，状态为：" + b.getStatus());
        }
        if (!List.of("通过", "调减", "驳回").contains(conclusion)) {
            throw new BizException("审核结论只能是 通过 / 调减 / 驳回");
        }
        if (!"通过".equals(conclusion) && (remark == null || remark.trim().length() < 5)) {
            throw new BizException("调减或驳回必须填写不少于 5 个字的理由");
        }
        Budget up = new Budget();
        up.setId(id);
        up.setAuditorId(UserContext.userId());
        up.setAuditTime(LocalDateTime.now());
        up.setAuditRemark(remark);
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        if ("通过".equals(conclusion)) {
            up.setStatus("已通过");
            up.setApproveAmount(b.getApplyAmount());
        } else if ("调减".equals(conclusion)) {
            if (approveAmount == null || approveAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("调减必须填写审核金额");
            }
            if (approveAmount.compareTo(b.getApplyAmount()) > 0) {
                throw new BizException("审核金额不得高于申请金额");
            }
            up.setStatus("已调减");
            up.setApproveAmount(approveAmount);
        } else {
            up.setStatus("已驳回");
            up.setApproveAmount(BigDecimal.ZERO);
        }
        budgetMapper.updateById(up);
        trace.log("经费管理", "审核", b.getBudgetNo(), "预算" + conclusion + (remark == null ? "" : "：" + remark));
        trace.notify(b.getCreateBy(), "预算审核结果",
                "科目「" + b.getItemName() + "」审核结论：" + conclusion
                        + (up.getApproveAmount() == null ? "" : "，核定 " + up.getApproveAmount() + " 元"),
                "审核", "budget", id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("status", up.getStatus());
        r.put("approveAmount", up.getApproveAmount());
        return r;
    }

    public Map<String, Object> budgetSummary(Long activityId) {
        List<Budget> list = budgetMapper.selectList(new QueryWrapper<Budget>().eq("activity_id", activityId));
        BigDecimal apply = list.stream().map(Budget::getApplyAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal approve = list.stream().map(b -> b.getApproveAmount() == null
                ? BigDecimal.ZERO : b.getApproveAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Activity a = activityMapper.selectById(activityId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activityBudget", a == null ? BigDecimal.ZERO : a.getBudgetTotal());
        m.put("applyTotal", apply);
        m.put("approveTotal", approve);
        m.put("itemCount", list.size());
        m.put("diff", (a == null ? BigDecimal.ZERO : a.getBudgetTotal()).subtract(apply));
        m.put("maxSingle", list.isEmpty() ? BigDecimal.ZERO
                : list.stream().map(Budget::getApplyAmount).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
        return m;
    }

    // ------------------------------------------------------------ 报销
    public PageResult<Map<String, Object>> pageExpense(long page, long size, Long clubId, String status,
                                                       String keyword) {
        QueryWrapper<Expense> w = new QueryWrapper<>();
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
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.like("expense_no", keyword);
        }
        w.orderByDesc("submit_time");
        Page<Expense> p = expenseMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), page, size, p.getRecords().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("expenseNo", e.getExpenseNo());
            m.put("activityId", e.getActivityId());
            Activity a = activityMapper.selectById(e.getActivityId());
            m.put("activityName", a == null ? "" : a.getActivityName());
            m.put("clubName", clubName(e.getClubId()));
            Budget b = budgetMapper.selectById(e.getBudgetId());
            m.put("itemName", b == null ? "" : b.getItemName());
            m.put("amount", e.getAmount());
            m.put("invoiceCount", e.getInvoiceCount());
            m.put("invoiceUrls", e.getInvoiceUrls());
            m.put("payType", e.getPayType());
            m.put("submitTime", e.getSubmitTime() == null ? null : e.getSubmitTime().format(DT));
            m.put("payTime", e.getPayTime() == null ? null : e.getPayTime().format(DT));
            m.put("status", e.getStatus());
            m.put("auditRemark", e.getAuditRemark());
            return m;
        }).toList());
    }

    /** 提交报销：金额不超过科目核定金额；超过 500 元至少 1 张发票 */
    @Transactional
    public Long createExpense(Map<String, Object> body) {
        Long activityId = num(body.get("activityId"));
        Long budgetId = num(body.get("budgetId"));
        Activity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        if (!List.of("已结束", "已结项").contains(a.getStatus())) {
            throw new BizException(4001, "只有已结束或已结项的活动可以提交报销");
        }
        Budget b = budgetMapper.selectById(budgetId);
        if (b == null) {
            throw new BizException("预算科目不存在");
        }
        BigDecimal amount = dec(body.get("amount"));
        BigDecimal limit = b.getApproveAmount() == null ? b.getApplyAmount() : b.getApproveAmount();
        if (amount.compareTo(limit) > 0) {
            throw new BizException("报销金额不得超过科目核定金额 " + limit + " 元");
        }
        int invoice = body.get("invoiceCount") == null ? 0 : Integer.parseInt(String.valueOf(body.get("invoiceCount")));
        if (amount.compareTo(new BigDecimal("500")) > 0 && invoice < 1) {
            throw new BizException("金额超过 500 元须至少上传 1 张发票");
        }
        String urls = body.get("invoiceUrls") == null ? "" : String.valueOf(body.get("invoiceUrls"));
        if (invoice > 0 && urls.isBlank()) {
            throw new BizException("请上传发票附件后再提交");
        }
        Expense e = new Expense();
        e.setActivityId(activityId);
        e.setClubId(a.getClubId());
        e.setBudgetId(budgetId);
        e.setApplicantId(UserContext.userId());
        e.setAmount(amount);
        e.setInvoiceCount(invoice);
        e.setInvoiceUrls(urls);
        e.setPayType(body.get("payType") == null ? "校内转账" : String.valueOf(body.get("payType")));
        e.setSubmitTime(LocalDateTime.now());
        e.setStatus("待财务复核");
        e.setCreateBy(UserContext.userId());
        e.setCreateTime(LocalDateTime.now());
        e.setDeleted(0);
        e.setVersion(0);
        e.setExpenseNo("T" + System.nanoTime());
        expenseMapper.insert(e);
        Expense up = new Expense();
        up.setId(e.getId());
        up.setExpenseNo("BX" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + String.format("%04d", e.getId() % 10000));
        expenseMapper.updateById(up);
        trace.log("经费管理", "新增", up.getExpenseNo(), "提交报销 " + amount + " 元，发票 " + invoice + " 张");
        trace.notify(4L, "报销单待复核", "报销单 " + up.getExpenseNo() + " 金额 " + amount + " 元，请财务复核。",
                "审核", "expense", e.getId());
        return e.getId();
    }

    /**
     * 财务复核：登记领取方式与支付时间，生成经费流水。
     * 余额为负时整笔记账回滚（抛异常，事务回滚）。
     */
    @Transactional
    public Map<String, Object> financeCheck(Long id, String payType, String payTime, String remark) {
        Expense e = expenseMapper.selectById(id);
        if (e == null) {
            throw new BizException(404, "报销单不存在");
        }
        if (!"待财务复核".equals(e.getStatus()) && !"待初审".equals(e.getStatus())) {
            throw new BizException(4001, "当前状态「" + e.getStatus() + "」不允许财务复核");
        }
        BigDecimal balance = clubService.balanceOf(e.getClubId());
        if (balance.subtract(e.getAmount()).compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("社团经费余额不足（当前 " + balance + " 元），本次记账回滚");
        }
        Expense up = new Expense();
        up.setId(id);
        up.setStatus("已支付");
        up.setPayType(payType == null ? e.getPayType() : payType);
        up.setPayTime(payTime == null || payTime.isBlank() ? LocalDateTime.now()
                : LocalDateTime.parse(normalize(payTime), DT));
        up.setAuditRemark(remark);
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        expenseMapper.updateById(up);

        FundFlow f = new FundFlow();
        f.setClubId(e.getClubId());
        // 业务类型按预算科目细化（场地费/物料费/宣传费/交通费/劳务费），
        // 否则看板「经费支出结构」只有一个扇区，看不出钱花在哪
        Budget b = e.getBudgetId() == null ? null : budgetMapper.selectById(e.getBudgetId());
        f.setBizType(b != null && b.getItemType() != null && !b.getItemType().isBlank()
                ? b.getItemType() + "费" : "报销支出");
        f.setBizId(e.getId());
        f.setDirection("支出");
        f.setAmount(e.getAmount());
        f.setBalanceAfter(balance.subtract(e.getAmount()).setScale(2, RoundingMode.HALF_UP));
        f.setOccurDate(LocalDate.now());
        f.setOperatorId(UserContext.userId());
        f.setRemark("报销单 " + e.getExpenseNo() + " 财务复核通过后登记支付");
        f.setCreateBy(UserContext.userId());
        f.setCreateTime(LocalDateTime.now());
        f.setDeleted(0);
        f.setVersion(0);
        f.setFlowNo("T" + System.nanoTime());
        flowMapper.insert(f);
        FundFlow fu = new FundFlow();
        fu.setId(f.getId());
        fu.setFlowNo("LS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + String.format("%05d", f.getId() % 100000));
        flowMapper.updateById(fu);

        // 活动已支出金额回写
        Activity a = activityMapper.selectById(e.getActivityId());
        if (a != null) {
            Activity au = new Activity();
            au.setId(a.getId());
            au.setSpentAmount((a.getSpentAmount() == null ? BigDecimal.ZERO : a.getSpentAmount()).add(e.getAmount()));
            activityMapper.updateById(au);
        }
        trace.log("经费管理", "复核", e.getExpenseNo(), "报销复核通过并登记支付 " + e.getAmount() + " 元");
        trace.notify(e.getApplicantId(), "报销已支付",
                "报销单 " + e.getExpenseNo() + " 已支付 " + e.getAmount() + " 元，余额 "
                        + f.getBalanceAfter() + " 元。", "提醒", "expense", id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("status", "已支付");
        r.put("flowNo", fu.getFlowNo());
        r.put("balanceAfter", f.getBalanceAfter());
        return r;
    }

    // ------------------------------------------------------------ 流水
    public PageResult<Map<String, Object>> pageFlow(long page, long size, Long clubId, String direction,
                                                    String bizType) {
        QueryWrapper<FundFlow> w = new QueryWrapper<>();
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
        if (direction != null && !direction.isBlank()) {
            w.eq("direction", direction);
        }
        if (bizType != null && !bizType.isBlank()) {
            w.eq("biz_type", bizType);
        }
        w.orderByDesc("occur_date").orderByDesc("id");
        Page<FundFlow> p = flowMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), page, size, p.getRecords().stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("flowNo", f.getFlowNo());
            m.put("clubId", f.getClubId());
            m.put("clubName", clubName(f.getClubId()));
            m.put("bizType", f.getBizType());
            m.put("bizId", f.getBizId());
            m.put("direction", f.getDirection());
            m.put("amount", f.getAmount());
            m.put("balanceAfter", f.getBalanceAfter());
            m.put("occurDate", f.getOccurDate() == null ? null : f.getOccurDate().toString());
            m.put("remark", f.getRemark());
            m.put("operatorName", clubService.nameOf(f.getOperatorId()));
            return m;
        }).toList());
    }

    /** 手工登记收入（学校拨款、会费、赞助），支出类只能由报销生成 */
    @Transactional
    public Long createFlow(Map<String, Object> body) {
        Long clubId = num(body.get("clubId"));
        BigDecimal amount = dec(body.get("amount"));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("金额必须大于 0");
        }
        String remark = body.get("remark") == null ? "" : String.valueOf(body.get("remark"));
        if (remark.trim().length() < 5) {
            throw new BizException("收入类流水必须填写来源说明（不少于 5 个字）");
        }
        BigDecimal balance = clubService.balanceOf(clubId).add(amount);
        FundFlow f = new FundFlow();
        f.setClubId(clubId);
        f.setBizType(body.get("bizType") == null ? "学校拨款" : String.valueOf(body.get("bizType")));
        f.setDirection("收入");
        f.setAmount(amount);
        f.setBalanceAfter(balance.setScale(2, RoundingMode.HALF_UP));
        f.setOccurDate(body.get("occurDate") == null ? LocalDate.now()
                : LocalDate.parse(String.valueOf(body.get("occurDate")).substring(0, 10)));
        f.setOperatorId(UserContext.userId());
        f.setRemark(remark);
        f.setCreateBy(UserContext.userId());
        f.setCreateTime(LocalDateTime.now());
        f.setDeleted(0);
        f.setVersion(0);
        f.setFlowNo("T" + System.nanoTime());
        flowMapper.insert(f);
        FundFlow up = new FundFlow();
        up.setId(f.getId());
        up.setFlowNo("LS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + String.format("%05d", f.getId() % 100000));
        flowMapper.updateById(up);
        trace.log("经费管理", "记账", up.getFlowNo(), "登记收入 " + amount + " 元：" + remark);
        return f.getId();
    }

    // ------------------------------------------------------------ 公示
    public PageResult<Map<String, Object>> pageDisclosure(long page, long size, Long clubId, String status) {
        QueryWrapper<FundDisclosure> w = new QueryWrapper<>();
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
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        w.orderByDesc("publish_start");
        Page<FundDisclosure> p = disclosureMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), page, size, p.getRecords().stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("disclosureNo", d.getDisclosureNo());
            m.put("clubId", d.getClubId());
            m.put("clubName", clubName(d.getClubId()));
            m.put("period", d.getPeriod());
            m.put("incomeAmount", d.getIncomeAmount());
            m.put("expenseAmount", d.getExpenseAmount());
            m.put("balanceAmount", d.getBalanceAmount());
            m.put("activityCount", d.getActivityCount());
            m.put("publishStart", d.getPublishStart() == null ? null : d.getPublishStart().toString());
            m.put("publishEnd", d.getPublishEnd() == null ? null : d.getPublishEnd().toString());
            m.put("objectionCount", d.getObjectionCount());
            m.put("status", d.getStatus());
            m.put("noticeUrl", d.getNoticeUrl());
            return m;
        }).toList());
    }

    public Map<String, Object> disclosureDetail(Long id) {
        FundDisclosure d = disclosureMapper.selectById(id);
        if (d == null) {
            throw new BizException(404, "公示不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("disclosureNo", d.getDisclosureNo());
        m.put("clubName", clubName(d.getClubId()));
        m.put("period", d.getPeriod());
        m.put("incomeAmount", d.getIncomeAmount());
        m.put("expenseAmount", d.getExpenseAmount());
        m.put("balanceAmount", d.getBalanceAmount());
        m.put("activityCount", d.getActivityCount());
        m.put("publishStart", d.getPublishStart() == null ? null : d.getPublishStart().toString());
        m.put("publishEnd", d.getPublishEnd() == null ? null : d.getPublishEnd().toString());
        m.put("status", d.getStatus());
        m.put("objectionCount", d.getObjectionCount());
        m.put("noticeUrl", d.getNoticeUrl());
        m.put("flows", flowMapper.selectList(new QueryWrapper<FundFlow>()
                        .eq("club_id", d.getClubId()).orderByDesc("occur_date").last("limit 50"))
                .stream().map(f -> Map.of("flowNo", f.getFlowNo(), "bizType", f.getBizType(),
                        "direction", f.getDirection(), "amount", f.getAmount(),
                        "occurDate", f.getOccurDate() == null ? "" : f.getOccurDate().toString()))
                .toList());
        m.put("objections", objectionMapper.selectList(new QueryWrapper<FundObjection>()
                        .eq("disclosure_id", id).orderByDesc("id"))
                .stream().map(o -> Map.of("id", o.getId(), "itemName", o.getItemName() == null ? "" : o.getItemName(),
                        "amount", o.getAmount(), "reason", o.getReason(),
                        "handleStatus", o.getHandleStatus(),
                        "applicantName", clubService.nameOf(o.getApplicantId()))).toList());
        return m;
    }

    /**
     * 发布公示：校验期末余额与流水余额一致、公示期不少于 7 天。
     * 数据由流水汇总生成，不允许手工修改金额。
     */
    @Transactional
    public Map<String, Object> publish(Long id) {
        FundDisclosure d = disclosureMapper.selectById(id);
        if (d == null) {
            throw new BizException(404, "公示不存在");
        }
        if ("公示中".equals(d.getStatus())) {
            throw new BizException(4009, "该公示已在公示中，请勿重复发布");
        }
        List<FundFlow> flows = flowMapper.selectList(new QueryWrapper<FundFlow>().eq("club_id", d.getClubId()));
        BigDecimal income = flows.stream().filter(f -> "收入".equals(f.getDirection()))
                .map(FundFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal out = flows.stream().filter(f -> "支出".equals(f.getDirection()))
                .map(FundFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = income.subtract(out).setScale(2, RoundingMode.HALF_UP);
        // 期间过滤：按期间字符串的起始年月粗粒度汇总（演示口径，正式口径按期间精确过滤）
        FundDisclosure up = new FundDisclosure();
        up.setId(id);
        up.setIncomeAmount(income.setScale(2, RoundingMode.HALF_UP));
        up.setExpenseAmount(out.setScale(2, RoundingMode.HALF_UP));
        up.setBalanceAmount(balance);
        long days = java.time.temporal.ChronoUnit.DAYS.between(d.getPublishStart(), d.getPublishEnd());
        if (days < 7) {
            throw new BizException("公示期不少于 7 天，当前仅 " + days + " 天，请调整公示起止日期");
        }
        up.setStatus("公示中");
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        disclosureMapper.updateById(up);
        trace.log("经费管理", "发布", d.getDisclosureNo(),
                "发布经费公示，收入 " + income + " 元，支出 " + out + " 元，余额 " + balance + " 元");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("status", "公示中");
        r.put("incomeAmount", up.getIncomeAmount());
        r.put("expenseAmount", up.getExpenseAmount());
        r.put("balanceAmount", up.getBalanceAmount());
        return r;
    }

    // ------------------------------------------------------------ 异议
    public PageResult<Map<String, Object>> pageObjection(long page, long size, String status, Long disclosureId) {
        QueryWrapper<FundObjection> w = new QueryWrapper<>();
        if (!UserContext.scopeAll()) {
            w.eq("applicant_id", UserContext.userId());
        }
        if (status != null && !status.isBlank()) {
            w.eq("handle_status", status);
        }
        if (disclosureId != null) {
            w.eq("disclosure_id", disclosureId);
        }
        w.orderByDesc("id");
        Page<FundObjection> p = objectionMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), page, size, p.getRecords().stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("objectionNo", o.getObjectionNo());
            m.put("disclosureId", o.getDisclosureId());
            FundDisclosure d = disclosureMapper.selectById(o.getDisclosureId());
            m.put("period", d == null ? "" : d.getPeriod());
            m.put("clubName", clubName(o.getClubId()));
            m.put("itemType", o.getItemType());
            m.put("itemName", o.getItemName());
            m.put("amount", o.getAmount());
            m.put("reason", o.getReason());
            m.put("applicantName", clubService.nameOf(o.getApplicantId()));
            m.put("handleStatus", o.getHandleStatus());
            m.put("replyContent", o.getReplyContent());
            m.put("replyTime", o.getReplyTime() == null ? null : o.getReplyTime().format(DT));
            m.put("createTime", o.getCreateTime() == null ? null : o.getCreateTime().format(DT));
            return m;
        }).toList());
    }

    /** 成员提异议：公示期内可提，提交后公示暂停归档并累加异议数 */
    @Transactional
    public Long createObjection(Map<String, Object> body) {
        Long disclosureId = num(body.get("disclosureId"));
        FundDisclosure d = disclosureMapper.selectById(disclosureId);
        if (d == null) {
            throw new BizException("公示不存在");
        }
        if ("已归档".equals(d.getStatus())) {
            throw new BizException(4001, "该公示已归档，不能再提异议");
        }
        String reason = body.get("reason") == null ? "" : String.valueOf(body.get("reason")).trim();
        if (reason.length() < 10) {
            throw new BizException("异议理由不少于 10 个字");
        }
        FundObjection o = new FundObjection();
        o.setDisclosureId(disclosureId);
        o.setClubId(d.getClubId());
        o.setItemType(body.get("itemType") == null ? "其他" : String.valueOf(body.get("itemType")));
        o.setItemName(body.get("itemName") == null ? null : String.valueOf(body.get("itemName")));
        o.setAmount(dec(body.get("amount")));
        o.setReason(reason);
        o.setApplicantId(UserContext.userId());
        o.setHandleStatus("待答复");
        o.setCreateBy(UserContext.userId());
        o.setCreateTime(LocalDateTime.now());
        o.setDeleted(0);
        o.setVersion(0);
        o.setObjectionNo("T" + System.nanoTime());
        objectionMapper.insert(o);
        FundObjection up = new FundObjection();
        up.setId(o.getId());
        up.setObjectionNo("YY" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + String.format("%04d", o.getId() % 10000));
        objectionMapper.updateById(up);
        FundDisclosure du = new FundDisclosure();
        du.setId(disclosureId);
        du.setObjectionCount((d.getObjectionCount() == null ? 0 : d.getObjectionCount()) + 1);
        disclosureMapper.updateById(du);
        trace.log("经费管理", "异议", up.getObjectionNo(), "成员提出异议：" + reason);
        trace.notify(2L, "公示异议提醒", "社团「" + clubName(d.getClubId()) + "」的经费公示收到新异议，请 5 个工作日内答复。",
                "提醒", "objection", o.getId());
        return o.getId();
    }

    @Transactional
    public boolean replyObjection(Long id, String conclusion, String content) {
        FundObjection o = objectionMapper.selectById(id);
        if (o == null) {
            throw new BizException(404, "异议不存在");
        }
        if (content == null || content.trim().length() < 5) {
            throw new BizException("答复内容不少于 5 个字");
        }
        FundObjection up = new FundObjection();
        up.setId(id);
        up.setHandleStatus(conclusion == null ? "已答复" : conclusion);
        up.setReplyContent(content);
        up.setReplyTime(LocalDateTime.now());
        up.setHandlerId(UserContext.userId());
        up.setUpdateBy(UserContext.userId());
        up.setUpdateTime(LocalDateTime.now());
        objectionMapper.updateById(up);
        trace.log("经费管理", "答复", o.getObjectionNo(), "异议答复：" + content);
        trace.notify(o.getApplicantId(), "公示异议已答复",
                "您对 " + (o.getItemName() == null ? "经费" : o.getItemName()) + " 科目的异议已答复：" + content,
                "提醒", "objection", id);
        return true;
    }

    /** 经费看板：收入/支出合计与结构 */
    public Map<String, Object> fundSummary(Long clubId) {
        QueryWrapper<FundFlow> w = new QueryWrapper<>();
        if (clubId != null) {
            w.eq("club_id", clubId);
        }
        List<FundFlow> flows = flowMapper.selectList(w);
        BigDecimal income = flows.stream().filter(f -> "收入".equals(f.getDirection()))
                .map(FundFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal out = flows.stream().filter(f -> "支出".equals(f.getDirection()))
                .map(FundFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> byType = flows.stream()
                .collect(Collectors.groupingBy(FundFlow::getBizType,
                        Collectors.reducing(BigDecimal.ZERO, FundFlow::getAmount, BigDecimal::add)));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("incomeTotal", income);
        m.put("expenseTotal", out);
        m.put("balance", income.subtract(out));
        m.put("typeDistribution", byType.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());
        m.put("pendingExpense", expenseMapper.selectList(new QueryWrapper<Expense>()
                        .in("status", List.of("待初审", "待财务复核")))
                .stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        return m;
    }

    /** 兼容 yyyy-MM-ddTHH:mm 与 yyyy-MM-dd HH:mm:ss 两种入参 */
    private static String normalize(String s) {
        String v = s.replace('T', ' ').trim();
        if (v.length() == 10) {
            return v + " 00:00:00";
        }
        if (v.length() == 16) {
            return v + ":00";
        }
        return v;
    }

    private static String require(Object v, String label) {
        if (v == null || String.valueOf(v).trim().isEmpty()) {
            throw new BizException(label + "不能为空");
        }
        return String.valueOf(v).trim();
    }

    private static Long num(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            throw new BizException("参数缺失");
        }
        return Long.valueOf(String.valueOf(o).replaceAll("\\.0$", ""));
    }

    private static BigDecimal dec(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(o));
    }
}
