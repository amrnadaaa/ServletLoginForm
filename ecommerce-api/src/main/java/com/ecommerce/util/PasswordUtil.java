package com.ecommerce.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 32;

    public static String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            byte[] saltAndHash = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, saltAndHash, 0, salt.length);
            System.arraycopy(hashedPassword, 0, saltAndHash, salt.length, hashedPassword.length);

            return Base64.getEncoder().encodeToString(saltAndHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not found", e);
        }
    }

    public static boolean verifyPassword(String password, String hashedPassword) {
        try {
            byte[] decoded = Base64.getDecoder().decode(hashedPassword);
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(decoded, 0, salt, 0, SALT_LENGTH);

            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedInput = md.digest(password.getBytes());

            byte[] storedHash = new byte[decoded.length - SALT_LENGTH];
            System.arraycopy(decoded, SALT_LENGTH, storedHash, 0, storedHash.length);

            return MessageDigest.isEqual(hashedInput, storedHash);
        } catch (Exception e) {
            return false;
        }
    }
}