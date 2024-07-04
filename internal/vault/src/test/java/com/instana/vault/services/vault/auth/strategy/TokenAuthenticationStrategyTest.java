package com.instana.vault.services.vault.auth.strategy;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TokenAuthenticationStrategyTest {

    @Test
    void testTokenPresent() {
        // Arrange
        String testToken = "test-token";
        TokenAuthenticationStrategy strategy = new TokenAuthenticationStrategy(testToken);

        // Act
        Optional<String> result = strategy.token();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testToken, result.get());
    }

    @Test
    void testTokenAbsent() {
        // Arrange
        TokenAuthenticationStrategy strategy = new TokenAuthenticationStrategy(null);

        // Act
        Optional<String> result = strategy.token();

        // Assert
        assertFalse(result.isPresent());
    }
}