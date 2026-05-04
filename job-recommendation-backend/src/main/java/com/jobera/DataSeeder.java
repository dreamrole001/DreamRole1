package com.jobera;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jobera.entity.User;
import com.jobera.repository.UserRepository;
import com.jobera.util.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedDemoUser(UserRepository userRepository) {
        return args -> {
            String demoEmail = "user@example.com";

            if (userRepository.findByEmail(demoEmail).isEmpty()) {
                PasswordEncoder encoder = new PasswordEncoder();

                User user = new User();
                user.setEmail(demoEmail);
                user.setPassword(encoder.encode("password123"));
                user.setFullName("Demo User");
                user.setPhone("9999999999");
                user.setIsActive(true);
                user.setRoleId(1L);

                userRepository.save(user);
                System.out.println("Demo user created: user@example.com / password123");
            }
        };
    }
}