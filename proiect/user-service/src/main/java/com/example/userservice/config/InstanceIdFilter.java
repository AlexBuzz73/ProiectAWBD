package com.example.userservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InstanceIdFilter extends OncePerRequestFilter implements InfoContributor {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${server.port:8081}")
    private int port;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("X-Instance-Id", serviceName + ":" + port);
        response.setHeader("X-Service-Port", String.valueOf(port));
        filterChain.doFilter(request, response);
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("service", serviceName)
               .withDetail("port", port)
               .withDetail("instanceId", serviceName + ":" + port);
    }
}
