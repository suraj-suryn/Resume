package com.suraj.banking.auth.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Audit Logging Filter — runs before the JWT filter (Order 1).
 *
 * Logs every inbound request with: method, URI, client IP, and timestamp.
 * Logs every outbound response with: status code and total duration.
 *
 * In a banking context, audit trails are a compliance requirement.
 * This filter demonstrates the interceptor/filter skill listed on my resume.
 */
@Component
@Order(1)
public class AuditLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long start = System.currentTimeMillis();
        String clientIp = resolveClientIp(request);

        log.info("[AUDIT-IN]  {} {} | IP: {} | Time: {}",
                request.getMethod(), request.getRequestURI(), clientIp, LocalDateTime.now());

        filterChain.doFilter(request, response);

        long duration = System.currentTimeMillis() - start;
        log.info("[AUDIT-OUT] {} {} | Status: {} | Duration: {}ms",
                request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
    }

    /**
     * Resolves the real client IP, accounting for reverse proxies via X-Forwarded-For.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
