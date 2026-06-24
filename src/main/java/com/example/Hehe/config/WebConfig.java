package com.example.Hehe.config;

import org.springframework.context.annotation.Configuration;
import com.example.Hehe.security.BranchLockInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.lang.NonNull;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final BranchLockInterceptor branchLockInterceptor;

    public WebConfig(BranchLockInterceptor branchLockInterceptor) {
        this.branchLockInterceptor = branchLockInterceptor;
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Expose the "uploads" directory so files can be accessed via HTTP
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(branchLockInterceptor)
                .addPathPatterns("/api/**");
    }
}
