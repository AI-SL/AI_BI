package com.laow.springbootinit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "spark.x1")
public class SparkX1Config {
    private String baseUrl;
    private String apiKey; // 必须配置，无默认值
    private String model;
    private int maxTokens;
    private double temperature;
    private double topP;
}
