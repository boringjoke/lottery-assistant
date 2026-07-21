package com.hotchpotch.lottery.config;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * SMTP 邮件发送器配置。
 */
@Configuration
public class MailSenderConfig {

    /**
     * 根据业务配置创建邮件发送器；是否真正发送由 MailSendService 判断。
     */
    @Bean
    public JavaMailSender javaMailSender(MailProperties mailProperties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailProperties.host());
        mailSender.setPort(mailProperties.port());
        mailSender.setUsername(mailProperties.username());
        mailSender.setPassword(mailProperties.password());
        mailSender.setDefaultEncoding("UTF-8");

        Properties javaMailProperties = mailSender.getJavaMailProperties();
        javaMailProperties.put("mail.smtp.auth", String.valueOf(mailProperties.auth()));
        javaMailProperties.put("mail.smtp.ssl.enable", String.valueOf(mailProperties.sslEnabled()));
        javaMailProperties.put("mail.smtp.starttls.enable", String.valueOf(mailProperties.starttlsEnabled()));
        javaMailProperties.put("mail.smtp.connectiontimeout", String.valueOf(mailProperties.connectTimeoutMillis()));
        javaMailProperties.put("mail.smtp.timeout", String.valueOf(mailProperties.readTimeoutMillis()));
        javaMailProperties.put("mail.smtp.writetimeout", String.valueOf(mailProperties.readTimeoutMillis()));

        return mailSender;
    }
}
