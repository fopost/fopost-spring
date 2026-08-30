package com.fopost.spring;

import com.fopost.sdk.FoPost;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Builds the {@link FoPost} client from {@code fopost.*} properties.
 *
 * <p>Nothing is registered until {@code fopost.api-key} is set, and a {@link FoPost} bean you
 * declare yourself always wins.
 */
@AutoConfiguration
@ConditionalOnClass(FoPost.class)
@ConditionalOnProperty(prefix = FoPostProperties.PREFIX, name = "api-key")
@EnableConfigurationProperties(FoPostProperties.class)
public class FoPostAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FoPost foPost(FoPostProperties properties) {
        return FoPost.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .timeout(properties.getTimeout())
                .maxRetries(properties.getMaxRetries())
                .build();
    }
}
