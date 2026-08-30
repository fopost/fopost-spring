package com.fopost.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.fopost.sdk.FoPost;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FoPostHealthIndicatorTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    FoPostAutoConfiguration.class, FoPostHealthAutoConfiguration.class));

    @Test
    void contributesAnIndicatorAlongsideTheClient() {
        runner.withPropertyValues("fopost.api-key=fp_test")
                .run(context -> assertThat(context).hasSingleBean(FoPostHealthIndicator.class));
    }

    @Test
    void staysOutWhenTurnedOff() {
        runner.withPropertyValues("fopost.api-key=fp_test", "management.health.fopost.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(FoPostHealthIndicator.class));
    }

    @Test
    void isUpWhenTheApiAnswers() {
        Health health = indicator(200, "{\"data\":[{\"id\":\"ws_1\",\"name\":\"Studio\"}]}").health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("workspaces", 1);
        assertThat(health.getDetails()).containsEntry("baseUrl", "https://api.fopost.com");
    }

    @Test
    void isDownWithTheApiErrorCode() {
        Health health = indicator(401, "{\"error\":\"invalid_api_key\",\"message\":\"Invalid key\"}")
                .health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", 401);
        assertThat(health.getDetails()).containsEntry("error", "invalid_api_key");
    }

    @Test
    void neverLeaksTheApiKeyIntoTheDetails() {
        Health health = indicator(500, "{\"error\":\"boom\",\"message\":\"upstream said fp_secret_key\"}")
                .health();

        assertThat(health.getDetails().toString()).doesNotContain("fp_secret_key").contains("***");
    }

    private FoPostHealthIndicator indicator(int status, String body) {
        FoPost client = FoPost.builder()
                .apiKey("fp_secret_key")
                .maxRetries(1)
                .transport(new StubTransport(status, body))
                .build();
        return new FoPostHealthIndicator(client, "fp_secret_key");
    }
}
