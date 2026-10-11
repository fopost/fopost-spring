package com.fopost.spring.blog;

import com.fopost.sdk.model.BlogArticlePublished;
import org.springframework.context.ApplicationEvent;

/**
 * A verified {@code article.published} delivery, republished on the application event bus as the
 * alternative to an {@link ArticlePublishedHandler} bean.
 *
 * <p>Listeners run synchronously on the request thread; an exception that escapes one becomes a
 * 500 and FoPost retries the delivery.
 */
public class FoPostArticlePublishedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final transient BlogArticlePublished article;
    private final String deliveryId;

    public FoPostArticlePublishedEvent(Object source, BlogArticlePublished article, String deliveryId) {
        super(source);
        this.article = article;
        this.deliveryId = deliveryId;
    }

    public BlogArticlePublished getArticle() {
        return article;
    }

    /** Value of {@code X-FoPost-Delivery}, unique per attempt, so it identifies a retry. */
    public String getDeliveryId() {
        return deliveryId;
    }
}
