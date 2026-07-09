package com.fitfuel.admin;

import com.fitfuel.user.AppUser;
import com.fitfuel.user.Role;
import com.fitfuel.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminDataInitializer.class);
    private static final String DEV_DEFAULT_PASSWORD = "FitFuel@Admin2026";

    // Seeded on first boot only if no ADMIN exists. In production, override these via
    // ADMIN_EMAIL / ADMIN_PASSWORD / ADMIN_NAME / ADMIN_PHONE so the well-known dev
    // password is never used.
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminPhone;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                @Value("${fitfuel.admin.name:FitFuel Admin}") String adminName,
                                @Value("${fitfuel.admin.email:admin@fitfuel.com}") String adminEmail,
                                @Value("${fitfuel.admin.password:" + DEV_DEFAULT_PASSWORD + "}") String adminPassword,
                                @Value("${fitfuel.admin.phone:0000000000}") String adminPhone) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminPhone = adminPhone;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (userRepository.existsByEmail(adminEmail) || userRepository.existsByPhone(adminPhone)) {
            log.warn("No ADMIN user exists, but the admin email/phone is already taken by another account. "
                    + "Skipping admin seed — promote an existing user to ADMIN manually.");
            return;
        }
        AppUser admin = new AppUser();
        admin.setName(adminName);
        admin.setEmail(adminEmail);
        admin.setPhone(adminPhone);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        if (DEV_DEFAULT_PASSWORD.equals(adminPassword)) {
            log.warn("Created default admin account: {} / {} — CHANGE THIS IMMEDIATELY. "
                    + "In production set ADMIN_PASSWORD to a strong value.", adminEmail, adminPassword);
        } else {
            log.info("Created admin account {} from configured ADMIN_PASSWORD.", adminEmail);
        }
    }
}
