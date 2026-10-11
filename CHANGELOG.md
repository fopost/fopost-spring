# Changelog

## Unreleased

- Receive blog articles: an article receiver mapped at `fopost.blog.webhook-path` whenever
  `fopost.blog.website-secret` is set. It verifies each delivery through the parent SDK, answers
  `article.test`, and hands `article.published` to an `ArticlePublishedHandler` bean and a
  `FoPostArticlePublishedEvent`.
- The `blog()` resource is reachable through the auto-configured `FoPost` bean, with
  `com.fopost:fopost-java` pinned to 0.3.0.
