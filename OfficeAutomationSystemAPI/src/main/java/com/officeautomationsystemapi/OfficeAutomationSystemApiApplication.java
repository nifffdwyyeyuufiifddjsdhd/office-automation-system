package com.officeautomationsystemapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// 应用入口：开启定时任务，并把 repository 包下的 Mapper 全部注册成 Bean
@SpringBootApplication
@EnableScheduling
@MapperScan("com.officeautomationsystemapi.repository")
public class OfficeAutomationSystemApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfficeAutomationSystemApiApplication.class, args);
    }

}
