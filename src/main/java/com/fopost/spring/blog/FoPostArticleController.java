package com.fopost.spring.blog;

import com.fopost.sdk.FoPostException;
import com.fopost.sdk.WebhookEnvelope;
import com.fopost.sdk.WebhookVerifier;
import com.fopost.sdk.model.BlogArticlePublished;
import com.fopost.sdk.model.WebhookEvents;
import com.fopost.spring.FoPostBlogDefaults;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives the articles a FoPost website target publishes.
 *
 * <p>Mapped only when {@code fopost.blog.website-secret} is set, at {@code fopost.blog.webhook-path}.
 * The raw body is verified through the parent SDK's {@link WebhookVerifier}; a bad signature is a
 * 401. {@code article.test} answers {@code {"ok": true}}. {@code article.published} hands the typed
 * payload to the {@link ArticlePublishedHandler} bean and publishes a
 * {@link FoPostArticlePublishedEvent}; a URL the handler returns is echoed as {@code {"url": ...}}.
 */
@RestController
public class FoPostArticleController {

    private static final Map<String, Object> OK = Map.of("ok", true);

    private final String secret;
    private final ObjectProvider<ArticlePublishedHandler> handlers;
    private final ApplicationEventPublisher events;

    public FoPostArticleController(
            String secret, ObjectProvider<ArticlePublishedHandler> handlers, ApplicationEventPublisher events) {
        this.secret = secret;
        this.handlers = handlers;
        this.events = events;
    }

    @PostMapping(
            path = FoPostBlogDefaults.PATH_EXPRESSION,
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> receive(
            @RequestBody(required = false) byte[] body,
            @RequestHeader Map<String, String> headers,
            @RequestHeader(name = WebhookVerifier.DELIVERY_HEADER, required = false) String deliveryId) {

        WebhookEnvelope envelope;
        try {
            envelope = WebhookVerifier.verify(body == null ? new byte[0] : body, headers, secret);
        } catch (FoPostException e) {
            HttpStatus status =
                    "invalid_payload".equals(e.code()) ? HttpStatus.BAD_REQUEST : HttpStatus.UNAUTHORIZED;
            return ResponseEntity.status(status).build();
        }

        if (!WebhookEvents.ARTICLE_PUBLISHED.equals(envelope.event())) {
            return ResponseEntity.ok(OK);
        }

        BlogArticlePublished article = envelope.data(BlogArticlePublished.class);
        if (article == null) {
            return ResponseEntity.badRequest().build();
        }

        String url = null;
        for (ArticlePublishedHandler handler : handlers.orderedStream().toList()) {
            String returned = handler.onArticlePublished(article);
            if (url == null && returned != null && !returned.isBlank()) {
                url = returned;
            }
        }
        events.publishEvent(new FoPostArticlePublishedEvent(this, article, deliveryId));

        return ResponseEntity.ok(url == null ? OK : Map.of("url", url));
    }
}
