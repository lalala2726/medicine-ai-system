package com.zhangyichuang.medicine.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
