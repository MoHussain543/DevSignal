package com.devsignal.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncJobConfig {

	@Bean(name = "analysisJobExecutor")
	public Executor analysisJobExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(32);
		executor.setThreadNamePrefix("analysis-job-");
		executor.initialize();
		return executor;
	}
}
