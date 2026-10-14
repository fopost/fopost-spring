package com.fopost.spring.blog;

import com.fopost.sdk.model.BlogArticlePublished;

/**
 * Receives each published article. Declare one bean of this type:
 *
 * <pre>{@code
 * @Bean
 * ArticlePublishedHandler articles(ArticleStore store) {
 *     return article -> store.save(article).publicUrl();
 * }
 * }</pre>
 *
 * <p>Return the URL the article now lives at and the receiver echoes it back to FoPost, or return
 * {@code null}. An exception becomes a 500 and FoPost retries the delivery.
 */
@FunctionalInterface
public interface ArticlePublishedHandler {

    String onArticlePublished(BlogArticlePublished article);
}
