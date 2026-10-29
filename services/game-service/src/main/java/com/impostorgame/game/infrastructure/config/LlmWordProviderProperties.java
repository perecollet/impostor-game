package com.impostorgame.game.infrastructure.config;

import com.impostorgame.game.domain.exception.WordPairUnavailableException;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "word-provider")
public record LlmWordProviderProperties(
        Integer maxAttempts,
        String language
) {

    public LlmWordProviderProperties {

        if (maxAttempts == null) {
            maxAttempts = 3;
        }

        if (maxAttempts <= 0) {
            throw new WordPairUnavailableException("max attempts must be greater than 0");
        }

        if (language == null || language.isBlank()) {
            language = "español";
        }
    }
}
