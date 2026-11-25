package com.grad.club.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grad.club.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 安全配置：无状态 JWT。JwtFilter 把 roleCode 直接写成 authority（不带 ROLE_ 前缀），
 * 因此这里统一用 hasAuthority，避免 hasRole 自动加前缀导致全员 403。
 *
 * 角色编码：ADMIN 系统管理员 / LEAGUE 校团委干事 / FINANCE 财务人员 /
 *          LEADER 社团负责人 / MEMBER 社团成员。
 * 前端隐藏按钮不等于授权，写接口必须后端复核（需求文档第 2 节）。
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/login", "/uploads/**", "/error").permitAll()
                // 门户公开数据：登录用户都可读（活动广场、公示列表）
                .requestMatchers(HttpMethod.GET, "/api/portal/**").authenticated()
                .requestMatchers("/api/auth/**").authenticated()
                // 消息中心：所有登录用户只能读写自己的消息
                .requestMatchers("/api/system/message/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/system/notice/list").authenticated()
                .requestMatchers("/api/meta/**").authenticated()
                // 系统管理（用户 / 角色 / 菜单 / 字典 / 日志）：仅系统管理员
                .requestMatchers("/api/system/**").hasAuthority("ADMIN")
                // 看板与统计报表：管理端角色
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**", "/api/stat/**")
                        .hasAnyAuthority("ADMIN", "LEAGUE", "FINANCE", "LEADER", "MEMBER")

                // ---------- 社团年审：校团委干事 ----------
                .requestMatchers(HttpMethod.POST, "/api/club/*/annual-review")
                        .hasAnyAuthority("ADMIN", "LEAGUE")
                // ---------- 活动立项审批：校团委干事 ----------
                .requestMatchers(HttpMethod.POST, "/api/activity/*/approve")
                        .hasAnyAuthority("ADMIN", "LEAGUE")
                // ---------- 预算审核：校团委干事 ----------
                .requestMatchers(HttpMethod.POST, "/api/budget/*/approve")
                        .hasAnyAuthority("ADMIN", "LEAGUE")
                // ---------- 财务复核与公示发布：财务人员 ----------
                .requestMatchers(HttpMethod.POST, "/api/expense/*/finance-check")
                        .hasAnyAuthority("ADMIN", "FINANCE")
                .requestMatchers(HttpMethod.POST, "/api/fund-disclosure/*/publish")
                        .hasAnyAuthority("ADMIN", "FINANCE")
                // ---------- 可行性规则配置：校团委干事 ----------
                .requestMatchers(HttpMethod.POST, "/api/stat/rule", "/api/stat/rule/**",
                        "/api/activity/*/feasibility/correct").hasAnyAuthority("ADMIN", "LEAGUE")
                .requestMatchers(HttpMethod.PUT, "/api/stat/rule/**").hasAnyAuthority("ADMIN", "LEAGUE")

                // ---------- 活动写操作：社团负责人 ----------
                .requestMatchers(HttpMethod.POST, "/api/activity", "/api/activity/**")
                        .hasAnyAuthority("ADMIN", "LEAGUE", "LEADER")
                .requestMatchers(HttpMethod.PUT, "/api/activity/**")
                        .hasAnyAuthority("ADMIN", "LEAGUE", "LEADER")
                .requestMatchers(HttpMethod.DELETE, "/api/activity/**").hasAuthority("ADMIN")
                // ---------- 报名与签到：成员本人 ----------
                .requestMatchers(HttpMethod.POST, "/api/activity-signup", "/api/activity-signup/**")
                        .hasAnyAuthority("ADMIN", "LEADER", "MEMBER")
                // ---------- 社团与成员维护 ----------
                .requestMatchers(HttpMethod.POST, "/api/club", "/api/club/**")
                        .hasAnyAuthority("ADMIN", "LEAGUE", "LEADER")
                .requestMatchers(HttpMethod.PUT, "/api/club/**")
                        .hasAnyAuthority("ADMIN", "LEAGUE", "LEADER")
                .requestMatchers(HttpMethod.DELETE, "/api/club/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/club-member", "/api/club-member/**")
                        .hasAnyAuthority("ADMIN", "LEAGUE", "LEADER")
                .requestMatchers(HttpMethod.PUT, "/api/club-member/**")
                        .hasAnyAuthority("ADMIN", "LEAGUE", "LEADER")
                .requestMatchers(HttpMethod.DELETE, "/api/club-member/**").hasAuthority("ADMIN")
                // ---------- 预算与报销：社团负责人编制，财务复核 ----------
                .requestMatchers(HttpMethod.POST, "/api/budget", "/api/budget/**")
                        .hasAnyAuthority("ADMIN", "LEAGUE", "LEADER")
                .requestMatchers(HttpMethod.PUT, "/api/budget/**")
                        .hasAnyAuthority("ADMIN", "LEAGUE", "LEADER")
                .requestMatchers(HttpMethod.DELETE, "/api/budget/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/expense", "/api/expense/**")
                        .hasAnyAuthority("ADMIN", "LEADER")
                .requestMatchers(HttpMethod.PUT, "/api/expense/**")
                        .hasAnyAuthority("ADMIN", "LEADER")
                .requestMatchers(HttpMethod.DELETE, "/api/expense/**").hasAuthority("ADMIN")
                // ---------- 流水记账：财务人员 ----------
                .requestMatchers(HttpMethod.POST, "/api/fund-flow/**")
                        .hasAnyAuthority("ADMIN", "FINANCE")
                // ---------- 公示异议：成员提交，校团委答复 ----------
                .requestMatchers(HttpMethod.POST, "/api/fund-objection")
                        .hasAnyAuthority("ADMIN", "MEMBER", "LEADER")
                .requestMatchers(HttpMethod.POST, "/api/fund-objection/*/reply")
                        .hasAnyAuthority("ADMIN", "LEAGUE")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> write(res, 401, "登录状态已失效，请重新登录"))
                .accessDeniedHandler((req, res, ex) -> write(res, 403, "没有该操作的权限")))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable());
        return http.build();
    }

    private void write(jakarta.servlet.http.HttpServletResponse res, int code, String msg) throws java.io.IOException {
        res.setStatus(200);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(R.fail(code, msg)));
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
