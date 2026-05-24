package com.library.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.library.factory.ServiceFactory;
import com.library.model.User;
import com.library.security.SecurityContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Validates Bearer JWT, attaches User into request attribute "currentUser".
 */
public class JwtFilter implements Filter {
    private static final Logger log = LogManager.getLogger(JwtFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;
        HttpServletResponse out = (HttpServletResponse) res;

        String auth = http.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            out.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Bearer token");
            return;
        }
        try {
            DecodedJWT jwt = SecurityContext.strategy().verify(auth.substring(7));
            String sub = jwt.getSubject();
            String email = claim(jwt, "email", sub + "@unknown");
            String name = claim(jwt, "name", "Unknown");

            User user = ServiceFactory.getInstance().users().findOrCreate(sub, email, name);
            http.setAttribute("currentUser", user);
            chain.doFilter(req, res);
        } catch (SecurityException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            out.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        }
    }

    private static String claim(DecodedJWT jwt, String name, String fallback) {
        var c = jwt.getClaim(name);
        return c.isMissing() || c.isNull() ? fallback : c.asString();
    }
}
