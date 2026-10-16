package com.suiyin.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class SuiyinApiApplicationTests {

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        // 测试时若不连真实库，可后续换成 Testcontainers；先跳过强制校验
        registry.add("spring.datasource.url",
                () -> "jdbc:mysql://127.0.0.1:3306/suiyin_ledger?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("MYSQL_PASSWORD", ""));
    }

    @Test
    void contextLoads() {
        // 仅验证 Spring 容器能启动（需要本机 MySQL 可用）
    }
}
