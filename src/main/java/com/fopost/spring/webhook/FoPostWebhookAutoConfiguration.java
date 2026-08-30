package com.fopost.spring.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fopost.spring.FoPostAutoConfiguration;
import com.fopost.spring.FoPostProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;

/**
 * Maps the inbound webhook endpoint when {@code fopost.webhook.enabled} is true.
 *
 * <p>Independent of the client: an application can receive webhooks without an API key, and the
 * other way round. It does need {@code fopost.webhook-secret} — a receiver that cannot verify a
 * signature would take anything anyone posted at it, so a missing secret stops startup.
 */
@AutoConfiguration(after = FoPostAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({RestController.class, ObjectMapper.class})
@ConditionalOnProperty(prefix = "fopost.webhook", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FoPostProperties.class)
public class FoPostWebhookAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FoPostWebhookSignatureVerifier foPostWebhookSignatureVerifier(FoPostProperties properties) {
        String secret = properties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "fopost: fopost.webhook.enabled is true but fopost.webhook-secret is not set — "
                            + "the endpoint cannot verify a delivery without it");
        }
        return new FoPostWebhookSignatureVerifier(secret);
    }

    @Bean
    @ConditionalOnMissingBean
    public FoPostWebhookController foPostWebhookController(
            FoPostWebhookSignatureVerifier verifier,
            ApplicationEventPublisher events,
            ObjectMapper mapper) {
        return new FoPostWebhookController(verifier, events, mapper);
    }
}
