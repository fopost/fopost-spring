package com.fopost.spring.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class FoPostBlogAutoConfigurationTests {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PropertyPlaceholderAutoConfiguration.class, FoPostBlogAutoConfiguration.class));

    @Test
    void mapsNothingWithoutASecret() {
        runner.run(context -> assertThat(context).doesNotHaveBean(FoPostArticleController.class));
    }

    @Test
    void mapsTheReceiverWhenTheSecretIsSet() {
        runner.withPropertyValues("fopost.blog.website-secret=whsec_test")
                .run(context -> assertThat(context).hasSingleBean(FoPostArticleController.class));
    }

    @Test
    void refusesToStartWithABlankSecret() {
        runner.withPropertyValues("fopost.blog.website-secret=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("fopost.blog.website-secret");
        });
    }
}
