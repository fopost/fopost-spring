package com.fopost.spring;

import com.fopost.sdk.FoPost;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Adds a {@code fopost} entry to the actuator health endpoint. Backs off entirely without
 * actuator on the classpath, and can be switched off with
 * {@code management.health.fopost.enabled=false}.
 */
@AutoConfiguration(after = FoPostAutoConfiguration.class)
@ConditionalOnClass({FoPost.class, HealthIndicator.class})
@ConditionalOnBean(FoPost.class)
@ConditionalOnProperty(name = "management.health.fopost.enabled", matchIfMissing = true)
public class FoPostHealthAutoConfiguration {

    @Bean("fopostHealthIndicator")
    @ConditionalOnMissingBean(name = "fopostHealthIndicator")
    public FoPostHealthIndicator fopostHealthIndicator(FoPost client, FoPostProperties properties) {
        return new FoPostHealthIndicator(client, properties.getApiKey());
    }
}
