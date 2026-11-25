package com.grad.club.controller;

import com.grad.club.common.PageResult;
import com.grad.club.common.R;
import com.grad.club.service.ActivityService;
import com.grad.club.service.FeasibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 活动：申报、审批、报名签到、流转与结项；可行性评分 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final FeasibilityService feasibilityService;

    @GetMapping("/activity/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long clubId,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String activityType,
                                                   @RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate) {
        return R.ok(activityService.page(page, size, clubId, keyword, status, activityType, startDate, endDate));
    }

    @GetMapping("/activity/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(activityService.detail(id));
    }

    @PostMapping("/activity")
    public R<Long> create(@RequestBody Map<String, Object> body) {
        return R.ok(activityService.create(body));
    }

    @PutMapping("/activity/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(activityService.update(id, body));
    }

    @DeleteMapping("/activity/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(activityService.delete(id));
    }

    /** 立项审批：targetStatus = 已立项 / 已驳回 */
    @PostMapping("/activity/{id}/approve")
    public R<Map<String, Object>> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(activityService.approve(id, String.valueOf(body.get("targetStatus")),
                body.get("reason") == null ? null : String.valueOf(body.get("reason"))));
    }

    /** 状态推进：已立项→进行中→已结束 */
    @PostMapping("/activity/{id}/transition")
    public R<Map<String, Object>> transition(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(activityService.transition(id, String.valueOf(body.get("targetStatus")),
                body.get("reason") == null ? null : String.valueOf(body.get("reason"))));
    }

    /** 结项：结转剩余预算 */
    @PostMapping("/activity/{id}/close")
    public R<Map<String, Object>> close(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(activityService.close(id, body == null ? null : String.valueOf(body.get("summary"))));
    }

    /** 可行性得分与因子明细（社团负责人 / 校团委） */
    @GetMapping("/activity/{id}/feasibility")
    public R<Map<String, Object>> feasibility(@PathVariable Long id) {
        return R.ok(feasibilityService.latest(id));
    }

    /** 重新计算（规则调整后可主动重算） */
    @PostMapping("/activity/{id}/feasibility")
    public R<Map<String, Object>> recalc(@PathVariable Long id) {
        return R.ok(feasibilityService.evaluate(id));
    }

    /** 人工修正等级，原因不少于 5 字 */
    @PostMapping("/activity/{id}/feasibility/correct")
    public R<Boolean> correct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(feasibilityService.correct(id,
                body.get("snapshotId") == null ? null : String.valueOf(body.get("snapshotId")),
                String.valueOf(body.get("newLevel")),
                body.get("reason") == null ? null : String.valueOf(body.get("reason"))));
    }

    // ---------------- 报名与签到
    @PostMapping("/activity-signup")
    public R<Long> signup(@RequestBody Map<String, Object> body) {
        return R.ok(activityService.signup(Long.valueOf(String.valueOf(body.get("activityId")))));
    }

    @PostMapping("/activity-signup/{id}/cancel")
    public R<Boolean> cancel(@PathVariable Long id) {
        return R.ok(activityService.cancelSignup(id));
    }

    /** 现场签到：6 位口令 */
    @PostMapping("/activity/{id}/checkin")
    public R<Map<String, Object>> checkin(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(activityService.checkin(id, String.valueOf(body.get("code"))));
    }

    @GetMapping("/activity/{id}/checkin-board")
    public R<Map<String, Object>> checkinBoard(@PathVariable Long id) {
        return R.ok(activityService.checkinBoard(id));
    }

    // ---------------- 门户
    @GetMapping("/portal/activity/page")
    public R<PageResult<Map<String, Object>>> portalList(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "9") long size,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String activityType) {
        return R.ok(activityService.portalList(page, size, keyword, activityType));
    }

    @GetMapping("/portal/signup/mine")
    public R<List<Map<String, Object>>> mySignups() {
        return R.ok(activityService.mySignups());
    }
}
