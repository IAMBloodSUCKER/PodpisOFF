package com.podpisoff.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.push")
public record PushProperties(
    String publicKey,
    String privateKey,
    String subject
) {
}
