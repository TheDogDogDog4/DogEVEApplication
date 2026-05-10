package com.dog.evesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.dog.evesystem",
        "com.Dog"
})
@EnableDiscoveryClient
@EnableFeignClients
public class EveSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EveSystemApplication.class, args);
    }

}
