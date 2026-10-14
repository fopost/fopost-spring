package com.fopost.spring.blog;

import com.fopost.sdk.WebhookVerifier;
import com.fopost.spring.FoPostAutoConfiguration;
import com.fopost.spring.FoPostProperties;
import org.springframework.beans.factory.ObjectProvider;
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
 * Maps the article receiver when {@code fopost.blog.website-secret} is set.
 *
 * <p>Independent of the client and of the general webhook endpoint: a site that only receives
 * articles needs neither an API key nor {@code fopost.webhook.enabled}.
 */
@AutoConfiguration(after = FoPostAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({RestController.class, WebhookVerifier.class})
@ConditionalOnProperty(prefix = "fopost.blog", name = "website-secret")
@EnableConfigurationProperties(FoPostProperties.class)
public class FoPostBlogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FoPostArticleController foPostArticleController(
            FoPostProperties properties,
            ObjectProvider<ArticlePublishedHandler> handlers,
            ApplicationEventPublisher events) {
        String secret = properties.getBlog().getWebsiteSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "fopost: fopost.blog.website-secret is present but blank, check the environment variable it reads");
        }
        return new FoPostArticleController(secret, handlers, events);
    }
}
