package com.dma.core.infrastructure.config;

import com.dma.core.infrastructure.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    @Value("${dma.security.encryption-key:DMADefaultKey2024!@#$}")
    private String encryptionKey;

    @Bean
    public CryptoUtil cryptoUtil() {
        return new CryptoUtil(encryptionKey);
    }
}
