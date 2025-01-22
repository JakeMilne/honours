package com.example.PythonIDE_Testing;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.http.HttpServlet;

@Configuration
public class ServletConfig {
    @Bean
    public ServletRegistrationBean<HttpServlet> myServletRegistrationBean() {
        return new ServletRegistrationBean<>(new MyServlet(), "/MyServlet");
    }
}