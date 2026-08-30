package com.fopost.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.fopost.sdk.FoPost;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class FoPostAutoConfigurationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FoPostAutoConfiguration.class));

    @Test
    void registersAClientWhenAnApiKeyIsConfigured() {
        runner.withPropertyValues("fopost.api-key=fp_test").run(context -> {
            assertThat(context).hasSingleBean(FoPost.class);
            assertThat(context.getBean(FoPost.class).baseUrl()).isEqualTo("https://api.fopost.com");
        });
    }

    @Test
    void registersNothingWithoutAnApiKey() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(FoPost.class);
            assertThat(context).doesNotHaveBean(FoPostProperties.class);
        });
    }

    @Test
    void backsOffToAUserDefinedClient() {
        runner.withPropertyValues("fopost.api-key=fp_test")
                .withUserConfiguration(OwnClient.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(FoPost.class);
                    assertThat(context.getBean(FoPost.class))
                            .isSameAs(context.getBean("myFoPost", FoPost.class));
                });
    }

    @Test
    void bindsEveryProperty() {
        runner.withPropertyValues(
                        "fopost.api-key=fp_test",
                        "fopost.base-url=https://api.example.test",
                        "fopost.timeout=5s",
                        "fopost.max-retries=1",
                        "fopost.default-workspace-id=ws_42",
                        "fopost.webhook-secret=shh",
                        "fopost.webhook.enabled=true",
                        "fopost.webhook.path=/hooks/fopost")
                .run(context -> {
                    FoPostProperties properties = context.getBean(FoPostProperties.class);
                    assertThat(properties.getApiKey()).isEqualTo("fp_test");
                    assertThat(properties.getBaseUrl()).isEqualTo("https://api.example.test");
                    assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(5));
                    assertThat(properties.getMaxRetries()).isOne();
                    assertThat(properties.getDefaultWorkspaceId()).isEqualTo("ws_42");
                    assertThat(properties.getWebhookSecret()).isEqualTo("shh");
                    assertThat(properties.getWebhook().isEnabled()).isTrue();
                    assertThat(properties.getWebhook().getPath()).isEqualTo("/hooks/fopost");
                    assertThat(context.getBean(FoPost.class).baseUrl())
                            .isEqualTo("https://api.example.test");
                });
    }

    @Test
    void failsFastOnABlankApiKey() {
        runner.withPropertyValues("fopost.api-key=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessageContaining("fopost.api-key");
        });
    }

    @Test
    void failsFastOnAnImpossibleRetryCount() {
        runner.withPropertyValues("fopost.api-key=fp_test", "fopost.max-retries=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnClient {

        @Bean
        FoPost myFoPost() {
            return FoPost.builder().apiKey("fp_mine").baseUrl("https://mine.test").build();
        }
    }
}
