package com.suraj.banking.auth.config;

import com.suraj.banking.auth.interceptor.RequestLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor requestLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply interceptor only to API endpoints — not to Swagger or H2 console
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/api/**");
    }
}
