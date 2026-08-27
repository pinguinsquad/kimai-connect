package de.p10d.kimai.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kimai")
public record KimaiProperties(String baseUrl, String token, int pageSize) {
}
