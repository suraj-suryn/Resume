package com.suraj.banking.auth.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Request Logging Interceptor — implements HandlerInterceptor (Spring MVC layer).
 *
 * Difference from AuditLoggingFilter (Servlet layer):
 *  - Filter runs for every servlet request (including static resources, error pages).
 *  - Interceptor runs only for requests matched to a Spring MVC controller,
 *    giving access to handler metadata.
 *
 * Demonstrates the Filter vs Interceptor distinction from the resume skill list.
 *
 * Lifecycle:
 *  preHandle  → before the controller method executes (returns false to abort)
 *  postHandle → after the controller method, before view rendering
 *  afterCompletion → after the full request cycle, even on exception
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_KEY = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_TIME_KEY, System.currentTimeMillis());
        log.debug("[INTERCEPTOR PRE]  {} {} → handler: {}",
                request.getMethod(), request.getRequestURI(), handler.getClass().getSimpleName());
        return true; // true = continue chain; false = abort and return response here
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        log.debug("[INTERCEPTOR POST] {} {} | Status: {}",
                request.getMethod(), request.getRequestURI(), response.getStatus());
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_KEY);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[INTERCEPTOR DONE] {} {} | {}ms | Status: {}",
                    request.getMethod(), request.getRequestURI(), duration, response.getStatus());
        }
        if (ex != null) {
            log.error("[INTERCEPTOR ERR]  {} {} | Exception: {}",
                    request.getMethod(), request.getRequestURI(), ex.getMessage());
        }
    }
}
