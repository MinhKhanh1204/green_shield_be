package com.chatbox.chatbox.config;

import com.chatbox.chatbox.model.Admin;
import com.chatbox.chatbox.repository.AdminRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeedRunner implements ApplicationRunner {

    private static final String DEFAULT_EMAIL = "admin@gmail.com";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;

    public AdminSeedRunner(AdminRepository adminRepository, PasswordEncoder passwordEncoder, Environment env) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.existsByEmail(DEFAULT_EMAIL)) {
            exitIfSeedOnly();
            return;
        }
        String password = env.getProperty("ADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD must be set in environment for initial seed");
        }
        Admin admin = Admin.builder()
                .email(DEFAULT_EMAIL)
                .passwordHash(passwordEncoder.encode(password))
                .build();
        adminRepository.save(admin);
        exitIfSeedOnly();
    }

    private void exitIfSeedOnly() {
        if (Boolean.parseBoolean(env.getProperty("seed-only", "false"))) {
            System.exit(0);
        }
    }
}
