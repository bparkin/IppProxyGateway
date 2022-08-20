package com.parchment.ipp.proxy.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class IppProxyGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(IppProxyGatewayApplication.class, args);
    }

}
