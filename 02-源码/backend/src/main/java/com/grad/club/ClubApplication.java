package com.grad.club;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 学生社团活动与经费管理系统启动类。
 * Mapper 统一扫描 com.grad.club.mapper，避免每个接口重复加 @Mapper。
 */
@SpringBootApplication
@MapperScan("com.grad.club.mapper")
public class ClubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClubApplication.class, args);
    }
}
