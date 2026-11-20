package com.proj.ScholarAssist.security;

import com.proj.ScholarAssist.entity.User;
import com.proj.ScholarAssist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Creates explicitly configured local reviewer accounts only under a bootstrap profile. */
@Component
@Profile("local-reviewer-bootstrap")
public class LocalReviewerBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String emails;
    private final String password;
    private final String role;
    private final boolean promoteExisting;

    public LocalReviewerBootstrap(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${SCHOLARASSIST_LOCAL_REVIEWER_EMAILS:${SCHOLARASSIST_LOCAL_REVIEWER_EMAIL:}}")
            String emails,
            @Value("${SCHOLARASSIST_LOCAL_REVIEWER_PASSWORD:}") String password,
            @Value("${SCHOLARASSIST_LOCAL_REVIEWER_ROLE:REVIEWER}") String role,
            @Value("${SCHOLARASSIST_LOCAL_REVIEWER_PROMOTE_EXISTING:false}") boolean promoteExisting) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.emails = emails == null ? "" : emails.trim();
        this.password = password;
        this.role = role == null ? "" : role.trim().toUpperCase(java.util.Locale.ROOT);
        this.promoteExisting = promoteExisting;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (emails.isBlank() || password == null || password.length() < 8
                || !java.util.List.of("REVIEWER", "ADMIN").contains(role)) {
            throw new IllegalStateException(
                    "Set reviewer email(s), a password of at least 8 characters, and role "
                            + "REVIEWER or ADMIN for local account bootstrap");
        }
        for (String configuredEmail : emails.split(",")) {
            String email = configuredEmail.trim();
            if (email.isBlank()) {
                continue;
            }
            var existing = users.findByEmailIgnoreCase(email);
            if (existing.isPresent()) {
                User user = existing.get();
                boolean authorizedRole = "REVIEWER".equals(user.getRole())
                        || "ADMIN".equals(user.getRole());
                boolean needsRoleChange = !role.equals(user.getRole());
                if ((!authorizedRole || needsRoleChange) && !promoteExisting) {
                    throw new IllegalStateException(
                            "The configured email has a different role: " + email
                                    + ". Set SCHOLARASSIST_LOCAL_REVIEWER_PROMOTE_EXISTING=true "
                                    + "only when explicitly changing this account.");
                }
                if (promoteExisting) {
                    user.setRole(role);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setUpdatedAt(LocalDateTime.now());
                    users.save(user);
                }
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            User reviewer = new User();
            reviewer.setEmail(email);
            reviewer.setPassword(passwordEncoder.encode(password));
            reviewer.setRole(role);
            reviewer.setCreatedAt(now);
            reviewer.setUpdatedAt(now);
            users.save(reviewer);
        }
    }
}
