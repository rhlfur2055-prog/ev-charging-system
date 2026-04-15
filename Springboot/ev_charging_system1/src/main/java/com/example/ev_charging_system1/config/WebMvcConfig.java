package com.example.ev_charging_system1.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.Arrays;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${ev.plate-logs.dir}")
    private String plateLogsDir;

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsCsv;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        Path absolute = Path.of(plateLogsDir).toAbsolutePath().normalize();
        String osPath = absolute.toString();
        if (!osPath.endsWith(java.io.File.separator)) {
            osPath = osPath + java.io.File.separator;
        }
        FileSystemResource location = new FileSystemResource(osPath);
        log.info("[ResourceHandler] /images/** -> {} (exists={})", osPath, absolute.toFile().isDirectory());

        registry.addResourceHandler("/images/**")
                .addResourceLocations(location)
                .setCachePeriod(0);
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        log.info("[CORS] allowedOriginPatterns = {}", Arrays.toString(origins));

        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
