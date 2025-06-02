package com.vn.zalopay.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "zalo-pay.config")
@Getter
@Setter
public class ZaloPayConfig {
    private String appid;
    private String key1;
    private String key2;
    private Endpoints endpoints;
    
    @Getter
    @Setter
    public static class Endpoints {
        private String create;
        private String banks;
        private String query;
        private String refund;
        private String refundQuery;
        private String callback;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}