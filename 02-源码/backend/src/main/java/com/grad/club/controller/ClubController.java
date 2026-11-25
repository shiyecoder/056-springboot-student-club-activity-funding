package com.grad.club.controller;

import com.grad.club.common.PageResult;
import com.grad.club.common.R;
import com.grad.club.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 社团与成员：列表、年审、名册、干部任免 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @GetMapping("/club/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String clubType) {
        return R.ok(clubService.pageClub(page, size, keyword, status, clubType));
    }

    @GetMapping("/club/options")
    public R<List<Map<String, Object>>> options() {
        return R.ok(clubService.clubOptions());
    }

    @GetMapping("/club/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(clubService.clubDetail(id));
    }

    @PostMapping("/club")
    public R<Long> create(@RequestBody Map<String, Object> body) {
        return R.ok(clubService.createClub(body));
    }

    @PutMapping("/club/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(clubService.updateClub(id, body));
    }

    @DeleteMapping("/club/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(clubService.deleteClub(id));
    }

    /** 校团委年审：结论 通过 / 整改 / 注销 */
    @PostMapping("/club/{id}/annual-review")
    public R<Map<String, Object>> annualReview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String conclusion = body.get("conclusion") == null ? String.valueOf(body.get("targetStatus"))
                : String.valueOf(body.get("conclusion"));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        Integer star = body.get("starLevel") == null ? null
                : Integer.valueOf(String.valueOf(body.get("starLevel")));
        BigDecimal limit = body.get("fundLimit") == null ? null
                : new BigDecimal(String.valueOf(body.get("fundLimit")));
        return R.ok(clubService.annualReview(id, conclusion, remark, star, limit));
    }

    // ---------------- 成员
    @GetMapping("/club-member/page")
    public R<PageResult<Map<String, Object>>> members(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) Long clubId,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String memberRole,
                                                      @RequestParam(required = false) String deptName) {
        return R.ok(clubService.pageMember(page, size, clubId, keyword, status, memberRole, deptName));
    }

    @GetMapping("/club-member/report")
    public R<Map<String, Object>> memberReport(@RequestParam(required = false) Long clubId) {
        return R.ok(clubService.memberReport(clubId));
    }

    /** 干部任免 */
    @PostMapping("/club-member/{id}/appoint")
    public R<Boolean> appoint(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(clubService.appoint(id, String.valueOf(body.get("memberRole")),
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    /** 成员退出（社长确认） */
    @PostMapping("/club-member/{id}/quit")
    public R<Boolean> quit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(clubService.quit(id, String.valueOf(body.get("reason"))));
    }

    @PostMapping("/club-member/{id}/audit")
    public R<Boolean> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(clubService.auditMember(id, String.valueOf(body.get("status"))));
    }
}
