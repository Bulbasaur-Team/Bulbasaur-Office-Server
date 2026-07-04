package ru.bulbasaur.office.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app")
public record AppProperties(Jwt jwt, Cors cors) {

    public record Jwt(String secret, Duration ttl) {
    }

    public record Cors(String origin) {
    }
}
