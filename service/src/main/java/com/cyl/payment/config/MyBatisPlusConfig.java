package com.cyl.payment.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author cyl
 * @date 2022-11-26 16:55
 * @description
 */
@Configuration
// 启用事务管理
@EnableTransactionManagement
@MapperScan(basePackages = "com.cyl.payment.mapper")
public class MyBatisPlusConfig {
}
