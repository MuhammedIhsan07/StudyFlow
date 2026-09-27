package com.studyflow.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** PBKDF2 password hashing using only standard JDK cryptography. */
final class PasswordHasher {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private final SecureRandom random = new SecureRandom();

    byte[] newSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec specification = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Secure password hashing is unavailable.", exception);
        } finally {
            specification.clearPassword();
        }
    }

    boolean matches(char[] candidate, byte[] salt, byte[] expectedHash) {
        return MessageDigest.isEqual(hash(candidate, salt), expectedHash);
    }
}
