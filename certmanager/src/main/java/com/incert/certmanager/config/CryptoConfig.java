package com.incert.certmanager.config;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

@Slf4j
@Configuration
public class CryptoConfig {

    @Bean
    public BouncyCastleProvider bouncyCastleProvider() {
        BouncyCastleProvider provider = new BouncyCastleProvider();
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(provider);
        }
        return provider;
    }
}

