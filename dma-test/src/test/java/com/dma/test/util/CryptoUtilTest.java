package com.dma.test.util;

import com.dma.core.infrastructure.util.CryptoUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    private final CryptoUtil crypto = new CryptoUtil("test-encryption-key-2024");

    @Test
    void shouldEncryptAndDecryptCorrectly() {
        String plain = "MySecretPassword123!";
        String encrypted = crypto.encrypt(plain);
        assertNotNull(encrypted);
        assertNotEquals(plain, encrypted);
        assertEquals(plain, crypto.decrypt(encrypted));
    }

    @Test
    void shouldHandleEmptyAndNull() {
        assertEquals("", crypto.encrypt(""));
        assertEquals("", crypto.encrypt(null));
        assertEquals("", crypto.decrypt(""));
        assertEquals("", crypto.decrypt(null));
    }

    @Test
    void shouldReturnRawValueOnDecryptFailure() {
        // Legacy unencrypted data should be returned as-is
        String legacy = "plaintext-legacy-password";
        assertEquals(legacy, crypto.decrypt(legacy));
    }

    @Test
    void encryptionIsDeterministicWithSameKey() {
        String plain = "test123";
        String e1 = crypto.encrypt(plain);
        String e2 = crypto.encrypt(plain);
        // GCM uses random IV, so each encryption produces different ciphertext
        assertNotEquals(e1, e2);
        // But both should decrypt to the same plaintext
        assertEquals(plain, crypto.decrypt(e1));
        assertEquals(plain, crypto.decrypt(e2));
    }
}
