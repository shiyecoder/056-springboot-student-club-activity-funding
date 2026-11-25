package com.grad.club.common;

/**
 * 当前登录用户上下文。由 JWT 过滤器写入，Controller / Service 只读。
 * 数据范围按需求文档第 2 节三层控制：ALL 全域、DEPT 本部门、SELF 本人（本社团/本人数据）。
 */
public class UserContext {

    /** roleId 用于按角色取菜单，orgName 复用为院系/部门名称 */
    public record CurrentUser(Long userId, String username, String realName, String roleCode,
                              Long roleId, String orgName, String dataScope) {
    }

    /** 角色编码常量，与 sys_role.role_code 一致 */
    public static final String ROLE_ADMIN = "ADMIN";       // 系统管理员
    public static final String ROLE_LEAGUE = "LEAGUE";     // 校团委干事
    public static final String ROLE_FINANCE = "FINANCE";   // 财务人员
    public static final String ROLE_LEADER = "LEADER";     // 社团负责人
    public static final String ROLE_MEMBER = "MEMBER";     // 社团成员

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static String username() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.username();
    }

    public static String realName() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.realName();
    }

    public static String roleCode() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.roleCode();
    }

    public static Long roleId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.roleId();
    }

    public static String orgName() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.orgName();
    }

    public static String dataScope() {
        CurrentUser u = HOLDER.get();
        return u == null ? "SELF" : u.dataScope();
    }

    public static boolean isAdmin() {
        return ROLE_ADMIN.equals(roleCode());
    }

    /** 校团委干事：年审、立项审批、预算审核、异议处理 */
    public static boolean isLeague() {
        return ROLE_LEAGUE.equals(roleCode()) || isAdmin();
    }

    /** 财务人员：报销复核、支付登记、流水与公示发布 */
    public static boolean isFinance() {
        return ROLE_FINANCE.equals(roleCode()) || isAdmin();
    }

    public static boolean isLeader() {
        return ROLE_LEADER.equals(roleCode());
    }

    public static boolean isMember() {
        return ROLE_MEMBER.equals(roleCode());
    }

    /** 管理端角色（有后台数据权限） */
    public static boolean isBackendStaff() {
        return isAdmin() || isLeague() || isFinance() || isLeader();
    }

    public static boolean scopeAll() {
        return "ALL".equals(dataScope());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
