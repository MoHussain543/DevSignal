package com.devsignal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ OpenAiProperties.class, SupabaseProperties.class, CorsProperties.class })
public class OpenAiConfig {
}
