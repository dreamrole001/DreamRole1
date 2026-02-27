package com.jobera.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jobera.entity.AuthToken;
import com.jobera.entity.User;
import com.jobera.repository.AuthTokenRepository;
import com.jobera.repository.UserRepository;
import com.jobera.util.PasswordEncoder;

@Service
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    
    public AuthenticationService(UserRepository userRepository,
                               AuthTokenRepository authTokenRepository) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = new PasswordEncoder();
    }
    
    public User registerUser(String email, String password, String fullName, String phone) {
        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new AuthenticationException("User with email " + email + " already exists");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setIsActive(true);
        user.setRoleId(1L); // ROLE_USER
        
        return userRepository.save(user);
    }
    
    public User loginUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            throw new AuthenticationException("Invalid email or password");
        }
        
        User user = userOptional.get();
        
        // Use primitive boolean check
        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new AuthenticationException("Account is deactivated");
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationException("Invalid email or password");
        }
        
        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        return user;
    }
    
    public String generateAuthToken(User user) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(30); // 30 days expiry
        
        AuthToken authToken = new AuthToken(user, token, expiryDate);
        authTokenRepository.save(authToken);
        
        return token;
    }
    
    public Optional<User> validateToken(String token) {
        Optional<AuthToken> authTokenOptional = authTokenRepository.findByToken(token);
        
        if (authTokenOptional.isEmpty()) {
            return Optional.empty();
        }
        
        AuthToken authToken = authTokenOptional.get();
        
        if (authToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            authTokenRepository.delete(authToken);
            return Optional.empty();
        }
        
        return Optional.of(authToken.getUser());
    }
    
    public void logout(String token) {
        authTokenRepository.deleteByToken(token);
    }
    
    public void logoutAllUserSessions(Long userId) {
        authTokenRepository.deleteByUserId(userId);
    }
    // Add this method to your existing AuthenticationService class
public boolean isUserRecruiter(Long userId) {
    Optional<User> user = userRepository.findById(userId);
    return user.isPresent() && user.get().getRoleId() == 3L; // ROLE_RECRUITER
}
// Add to AuthenticationService.java
public boolean isUserAdmin(Long userId) {
    Optional<User> user = userRepository.findById(userId);
    return user.isPresent() && user.get().getRoleId() == 2L; // ROLE_ADMIN
}
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}