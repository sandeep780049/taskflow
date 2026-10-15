package com.taskflow.config;

import com.taskflow.user.Role;
import com.taskflow.user.UserAccount;
import com.taskflow.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    @Bean
    CommandLineRunner seedDemoUser(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.findByEmailIgnoreCase("demo@taskflow.dev").isPresent()) {
                log.info("Demo user already exists, skipping seed");
                return;
            }
            UserAccount demo = new UserAccount("demo@taskflow.dev", encoder.encode("demo-pass-1234"), Role.USER);
            users.save(demo);
            log.info("Seeded demo user: demo@taskflow.dev / demo-pass-1234 (id={})", demo.getId());
        };
    }
}
