package com.grad.club.controller;

import com.grad.club.common.R;
import com.grad.club.common.UserContext;
import com.grad.club.service.ClubService;
import com.grad.club.service.DashboardService;
import com.grad.club.service.FeasibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 看板、统计报表与可行性规则配置 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StatController {

    private final DashboardService dashboardService;
    private final FeasibilityService feasibilityService;
    private final ClubService clubService;

    // ---------------- 看板
    @GetMapping("/dashboard/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(dashboardService.overview());
    }

    @GetMapping("/dashboard/charts")
    public R<Map<String, Object>> charts() {
        return R.ok(dashboardService.charts());
    }

    @GetMapping("/stat/report")
    public R<Map<String, Object>> report(@RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate) {
        return R.ok(dashboardService.report(startDate, endDate));
    }

    // ---------------- 可行性规则（创新点）
    @GetMapping("/stat/rule/list")
    public R<List<Map<String, Object>>> rules() {
        return R.ok(feasibilityService.rules());
    }

    @GetMapping("/stat/rule/current")
    public R<Map<String, Object>> currentRule() {
        return R.ok(feasibilityService.rules().stream().filter(r -> "生效".equals(r.get("status")))
                .findFirst().orElse(null));
    }

    @PostMapping("/stat/rule")
    public R<Long> saveRule(@RequestBody Map<String, Object> body) {
        return R.ok(feasibilityService.saveRule(body));
    }

    @GetMapping("/stat/feasibility")
    public R<Map<String, Object>> feasibilityStat() {
        return R.ok(feasibilityService.stat());
    }

    // ---------------- 门户：我的社团
    @GetMapping("/portal/club/mine")
    public R<List<Map<String, Object>>> myClubs() {
        List<Long> ids = clubService.visibleClubIds();
        if (ids == null || ids.isEmpty()) {
            return R.ok(List.of());
        }
        return R.ok(ids.stream().map(clubService::clubDetail).toList());
    }

    @GetMapping("/portal/profile/roles")
    public R<Map<String, Object>> roles() {
        return R.ok(Map.of("roleCode", UserContext.roleCode(),
                "roleName", UserContext.roleCode(), "deptName", UserContext.orgName(),
                "userId", UserContext.userId() == null ? 0 : UserContext.userId()));
    }
}
