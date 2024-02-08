package com.onboarding.onboarding.config;

import com.onboarding.onboarding.interceptor.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Divakar Verma
 * @created_at : 31/01/2024 - 12:35 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
@Configuration
public class AuthenticationInterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private AuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(authenticationInterceptor)
                .order(Ordered.HIGHEST_PRECEDENCE);
    }
}
