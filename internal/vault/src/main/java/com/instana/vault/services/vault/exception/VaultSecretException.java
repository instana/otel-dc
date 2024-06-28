/*
 * IBM Confidential
 * Copyright IBM Corp. 2024
 */

package com.instana.vault.services.vault.exception;

public class VaultSecretException extends RuntimeException {

    public VaultSecretException(String message) {
        super(message);
    }
}
