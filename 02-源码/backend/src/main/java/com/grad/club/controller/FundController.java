package com.grad.club.controller;

import com.grad.club.common.PageResult;
import com.grad.club.common.R;
import com.grad.club.service.FundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/** 经费：预算、报销、流水、公示与异议 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FundController {

    private final FundService fundService;

    // ---------------- 预算
    @GetMapping("/budget/page")
    public R<PageResult<Map<String, Object>>> budgetPage(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) Long activityId,
                                                         @RequestParam(required = false) Long clubId,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String keyword) {
        return R.ok(fundService.pageBudget(page, size, activityId, clubId, status, keyword));
    }

    @GetMapping("/budget/summary/{activityId}")
    public R<Map<String, Object>> budgetSummary(@PathVariable Long activityId) {
        return R.ok(fundService.budgetSummary(activityId));
    }

    @PostMapping("/budget")
    public R<Long> createBudget(@RequestBody Map<String, Object> body) {
        return R.ok(fundService.createBudget(body));
    }

    /** 预算审核：conclusion = 通过 / 调减 / 驳回 */
    @PostMapping("/budget/{id}/approve")
    public R<Map<String, Object>> approveBudget(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = body.get("approveAmount") == null ? null
                : new BigDecimal(String.valueOf(body.get("approveAmount")));
        return R.ok(fundService.approveBudget(id, String.valueOf(body.get("conclusion")), amount,
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    // ---------------- 报销
    @GetMapping("/expense/page")
    public R<PageResult<Map<String, Object>>> expensePage(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @RequestParam(required = false) Long clubId,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String keyword) {
        return R.ok(fundService.pageExpense(page, size, clubId, status, keyword));
    }

    @PostMapping("/expense")
    public R<Long> createExpense(@RequestBody Map<String, Object> body) {
        return R.ok(fundService.createExpense(body));
    }

    /** 财务复核并登记支付 */
    @PostMapping("/expense/{id}/finance-check")
    public R<Map<String, Object>> financeCheck(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(fundService.financeCheck(id,
                body.get("payType") == null ? null : String.valueOf(body.get("payType")),
                body.get("payTime") == null ? null : String.valueOf(body.get("payTime")),
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    // ---------------- 流水
    @GetMapping("/fund-flow/page")
    public R<PageResult<Map<String, Object>>> flowPage(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) Long clubId,
                                                       @RequestParam(required = false) String direction,
                                                       @RequestParam(required = false) String bizType) {
        return R.ok(fundService.pageFlow(page, size, clubId, direction, bizType));
    }

    @PostMapping("/fund-flow")
    public R<Long> createFlow(@RequestBody Map<String, Object> body) {
        return R.ok(fundService.createFlow(body));
    }

    @GetMapping("/fund-flow/summary")
    public R<Map<String, Object>> flowSummary(@RequestParam(required = false) Long clubId) {
        return R.ok(fundService.fundSummary(clubId));
    }

    // ---------------- 公示
    @GetMapping("/fund-disclosure/page")
    public R<PageResult<Map<String, Object>>> disclosurePage(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "10") long size,
                                                             @RequestParam(required = false) Long clubId,
                                                             @RequestParam(required = false) String status) {
        return R.ok(fundService.pageDisclosure(page, size, clubId, status));
    }

    @GetMapping("/fund-disclosure/{id}")
    public R<Map<String, Object>> disclosureDetail(@PathVariable Long id) {
        return R.ok(fundService.disclosureDetail(id));
    }

    @PostMapping("/fund-disclosure/{id}/publish")
    public R<Map<String, Object>> publish(@PathVariable Long id) {
        return R.ok(fundService.publish(id));
    }

    // ---------------- 异议
    @GetMapping("/fund-objection/page")
    public R<PageResult<Map<String, Object>>> objectionPage(@RequestParam(defaultValue = "1") long page,
                                                            @RequestParam(defaultValue = "10") long size,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) Long disclosureId) {
        return R.ok(fundService.pageObjection(page, size, status, disclosureId));
    }

    @PostMapping("/fund-objection")
    public R<Long> createObjection(@RequestBody Map<String, Object> body) {
        return R.ok(fundService.createObjection(body));
    }

    @PostMapping("/fund-objection/{id}/reply")
    public R<Boolean> replyObjection(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(fundService.replyObjection(id,
                body.get("conclusion") == null ? "已答复" : String.valueOf(body.get("conclusion")),
                String.valueOf(body.get("content"))));
    }
}
