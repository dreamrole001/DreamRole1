package com.jobera.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Simple password encoder using SHA-256 with salt
 */
public class PasswordEncoder {
    
    public String encode(CharSequence rawPassword) {
        try {
            // Generate salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            // Hash password with salt
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hashedPassword = digest.digest(rawPassword.toString().getBytes());
            
            // Combine salt and hashed password
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);
            
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (NoSuchAlgorithmException e) {
            throw new PasswordEncodingException("Error encoding password", e);
        }
    }
    
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        try {
            // Decode the combined salt + hash
            byte[] combined = Base64.getDecoder().decode(encodedPassword);
            
            // Extract salt (first 16 bytes)
            byte[] salt = new byte[16];
            System.arraycopy(combined, 0, salt, 0, salt.length);
            
            // Extract stored hash (remaining bytes)
            byte[] storedHash = new byte[combined.length - 16];
            System.arraycopy(combined, 16, storedHash, 0, storedHash.length);
            
            // Hash the raw password with the same salt
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] testHash = digest.digest(rawPassword.toString().getBytes());
            
            // Compare the hashes
            return MessageDigest.isEqual(storedHash, testHash);
            
        } catch (NoSuchAlgorithmException e) {
            throw new PasswordEncodingException("Error verifying password", e);
        }
    }
    
    // Custom exception for password encoding errors
    public static class PasswordEncodingException extends RuntimeException {
        public PasswordEncodingException(String message) {
            super(message);
        }
        
        public PasswordEncodingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}