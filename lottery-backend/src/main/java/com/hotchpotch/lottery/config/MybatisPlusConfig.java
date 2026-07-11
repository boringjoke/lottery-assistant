package com.hotchpotch.lottery.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 基础配置。
 */
@Configuration
@MapperScan("com.hotchpotch.lottery.**.mapper")
public class MybatisPlusConfig {
}
