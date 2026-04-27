package com.zhangyichuang.medicine.common.systemauth.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemAuthSignerTests {

    private final SystemAuthSigner signer = new SystemAuthSigner();

    @Test
    void sign_ShouldGenerateExpectedBase64UrlSignature() {
        String canonical = "GET\n/test\na=1\n1700000000\nnonce\nabc";

        String signature = signer.sign("secret", canonical);

        assertEquals("t_ec_ihZDMLfHm7rCbBnSALOF_CqAn3xOoByEwTBM9A", signature);
    }

    @Test
    void constantTimeEquals_ShouldCompareValueCorrectly() {
        assertTrue(signer.constantTimeEquals("abc", "abc"));
        assertFalse(signer.constantTimeEquals("abc", "abd"));
        assertFalse(signer.constantTimeEquals("abc", null));
    }
}
