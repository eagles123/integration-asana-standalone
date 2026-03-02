package com.baker.integration.asana.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Value("${file-transfer.max-concurrent:5}")
    private int maxConcurrent;

    @Bean("fileTransferExecutor")
    public Executor fileTransferExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(maxConcurrent);
        executor.setMaxPoolSize(maxConcurrent * 2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("file-transfer-");
        executor.initialize();
        return executor;
    }
}
