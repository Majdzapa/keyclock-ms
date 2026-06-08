package com.incert.certmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class CertmanagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertmanagerApplication.class, args);
    }
}
