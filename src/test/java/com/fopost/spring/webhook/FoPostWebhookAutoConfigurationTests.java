package com.fopost.spring.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class FoPostWebhookAutoConfigurationTests {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PropertyPlaceholderAutoConfiguration.class,
                    JacksonAutoConfiguration.class,
                    FoPostWebhookAutoConfiguration.class));

    @Test
    void mapsNothingByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(FoPostWebhookController.class));
    }

    @Test
    void mapsTheControllerWhenEnabled() {
        runner.withPropertyValues("fopost.webhook.enabled=true", "fopost.webhook-secret=whsec_test")
                .run(context -> {
                    assertThat(context).hasSingleBean(FoPostWebhookController.class);
                    assertThat(context).hasSingleBean(FoPostWebhookSignatureVerifier.class);
                });
    }

    @Test
    void refusesToStartWithoutASigningSecret() {
        runner.withPropertyValues("fopost.webhook.enabled=true").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("fopost.webhook-secret");
        });
    }
}
