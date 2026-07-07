package ru.bulbasaur.office.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.LocalDate;

@ConfigurationProperties("app")
public record AppProperties(Jwt jwt, Cors cors, Wotd wotd) {

    public record Jwt(String secret, Duration ttl) {
    }

    public record Cors(String origin) {
    }

    public record Wotd(String secret, LocalDate launchDate) {
    }
}
