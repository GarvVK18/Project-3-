package com.iam.server.config;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.iam.server.entity.Authority;
import com.iam.server.entity.Role;
import com.iam.server.entity.User;
import com.iam.server.repository.AuthorityRepository;
import com.iam.server.repository.RoleRepository;
import com.iam.server.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuthorityRepository authorityRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed default authorities
        Authority readProfile = authorityRepository.findByName("READ_PROFILE")
                .orElseGet(() -> authorityRepository.save(new Authority("READ_PROFILE")));
        Authority writeProfile = authorityRepository.findByName("WRITE_PROFILE")
                .orElseGet(() -> authorityRepository.save(new Authority("WRITE_PROFILE")));
        Authority manageUsers = authorityRepository.findByName("MANAGE_USERS")
                .orElseGet(() -> authorityRepository.save(new Authority("MANAGE_USERS")));

        // Seed default roles
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role r = new Role("USER");
                    r.getAuthorities().add(readProfile);
                    r.getAuthorities().add(writeProfile);
                    return roleRepository.save(r);
                });

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role r = new Role("ADMIN");
                    r.getAuthorities().add(readProfile);
                    r.getAuthorities().add(writeProfile);
                    r.getAuthorities().add(manageUsers);
                    return roleRepository.save(r);
                });

        // Seed default admin user if not present
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User("admin", passwordEncoder.encode("Admin@123"));
            admin.setRoles(Set.of(adminRole, userRole));
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("Initialized default admin account: username='admin'");
        }
    }
}
