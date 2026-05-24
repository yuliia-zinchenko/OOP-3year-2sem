package com.library.security;

import com.library.domain.Role;
import com.library.domain.UserAccount;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository users;
    private final JwtRolesStrategy rolesStrategy;

    /** Loads (or auto-provisions) the user behind the current JWT. */
    @Transactional
    public UserAccount current() {
        Jwt jwt = jwt();
        String sub = jwt.getSubject();
        return users.findBySub(sub).orElseGet(() -> {
            String email = jwt.getClaimAsString("email");
            String name  = jwt.getClaimAsString("name");
            Role role = rolesStrategy.extractRoles(jwt).stream()
                    .map(String::toUpperCase)
                    .anyMatch("LIBRARIAN"::equals) ? Role.LIBRARIAN : Role.READER;
            return users.save(UserAccount.builder()
                    .sub(sub).email(email).fullName(name).role(role).build());
        });
    }

    private static Jwt jwt() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken t) return t.getToken();
        throw new IllegalStateException("No JWT authentication in context");
    }
}
