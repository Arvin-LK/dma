package com.dma.core.domain.service;

/** SPI 扩展点：许可证管理（商业化预留） */
public interface LicenseManager {
    default boolean isFeatureEnabled(String feature) { return true; }
    default boolean validateLicense(String licenseKey) { return true; }
}
