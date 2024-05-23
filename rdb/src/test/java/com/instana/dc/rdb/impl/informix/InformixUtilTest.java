package com.instana.dc.rdb.impl.informix;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InformixUtilTest {

    @Test
    public void shouldDecodePassword() {
        assertEquals("encoded_password", InformixUtil.decodePassword(
                Base64.getEncoder().encodeToString("encoded_password".getBytes())));
    }
}
