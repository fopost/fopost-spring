# fopost-spring-boot-starter

[![CI](https://github.com/fopost/fopost-spring/actions/workflows/ci.yml/badge.svg)](https://github.com/fopost/fopost-spring/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Official Spring Boot starter for the [FoPost](https://fopost.com) API. Schedule and publish to +30
social platforms from your Spring application.

This is a thin wrapper around [`com.fopost:fopost-java`](https://github.com/fopost/fopost-java).
Every request, retry, model, and error type lives there; the starter only wires it into Spring
Boot — a configured client bean, `fopost.*` properties with IDE autocomplete, an actuator health
entry, and inbound webhooks turned into application events.

Requires Java 17 and Spring Boot 3.

## Installation

```xml
<dependency>
  <groupId>com.fopost</groupId>
  <artifactId>fopost-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

```kotlin
implementation("com.fopost:fopost-spring-boot-starter:0.1.0")
```

> **0.x release.** The public API is still settling and minor versions may contain breaking
> changes. Pin an exact version if that matters to you.

## Configuration

Set a key and you have a client. Nothing is registered until `fopost.api-key` is present, so the
starter is inert on a classpath that merely happens to contain it.

```yaml
fopost:
  api-key: ${FOPOST_API_KEY} # required; sent as X-API-Key
  base-url: https://api.fopost.com # default
  timeout: 30s # default
  max-retries: 3 # default: total attempts, so two retries
  default-workspace-id: ws_123 # optional, for your own code to read
  webhook-secret: ${FOPOST_WEBHOOK_SECRET} # required only when receiving webhooks
  webhook:
    enabled: false # default
    path: /fopost/webhooks # default
```

Every key ships configuration metadata, so an IDE completes and documents them in
`application.yml` and `application.properties`.

| Property | Default | What it does |
| --- | --- | --- |
| `fopost.api-key` | none, required | API key from [fopost.com/dashboard/api-keys](https://fopost.com/dashboard/api-keys) |
| `fopost.base-url` | `https://api.fopost.com` | API root |
| `fopost.timeout` | `30s` | How long one request may take |
| `fopost.max-retries` | `3` | Total attempts for a rate limited request |
| `fopost.default-workspace-id` | none | Workspace your code falls back to |
| `fopost.webhook-secret` | none | Signing secret for inbound deliveries |
| `fopost.webhook.enabled` | `false` | Map the webhook controller |
| `fopost.webhook.path` | `/fopost/webhooks` | Where it listens |
| `management.health.fopost.enabled` | `true` | Contribute the actuator health entry |

A blank `fopost.api-key` — usually an environment variable that never got set — fails the context
at startup instead of at the first call.

## Using the client

Inject `FoPost` anywhere. It is a singleton, immutable, and safe to share across threads.

```java
import com.fopost.sdk.FoPost;
import com.fopost.sdk.model.Account;
import com.fopost.sdk.model.Post;
import com.fopost.sdk.param.CreatePostParams;

@Service
class Announcements {

    private final FoPost fopost;

    Announcements(FoPost fopost) {
        this.fopost = fopost;
    }

    String publish(String workspaceId, String text) {
        List<String> accounts =
                fopost.accounts().list(workspaceId).stream().map(Account::id).toList();

        Post post = fopost.posts()
                .create(CreatePostParams.of(workspaceId).content(text).accounts(accounts));

        fopost.posts().publish(post.id());
        return post.id();
    }
}
```

Declare your own `FoPost` bean and the starter backs off, so an unusual setup — a custom transport,
a proxy, one client per tenant — needs no fighting with the auto-configuration.

The full resource surface (`posts`, `accounts`, `workspaces`, `labels`, `webhooks`, `analytics`,
`automations`, `media`, `ai`), pagination, error types, and the `request` escape hatch are
documented in [fopost-java](https://github.com/fopost/fopost-java).

## Receiving webhooks

Turn the endpoint on and give it the signing secret the create call returned:

```yaml
fopost:
  webhook-secret: ${FOPOST_WEBHOOK_SECRET}
  webhook:
    enabled: true
    path: /fopost/webhooks
```

The controller verifies `X-FoPost-Signature` — HMAC-SHA256 over the raw request body, compared in
constant time — and republishes the delivery on the application event bus. A body the secret does
not sign is a 401 and never reaches a listener.

```java
@Component
class Deliveries {

    @EventListener
    void onPublished(FoPostPostPublishedEvent event) {
        log.info("post {} is live", event.get("post_id"));
    }

    @EventListener
    void onFailed(FoPostDeliveryFailedEvent event) {
        log.warn("{} rejected it: {}", event.get("platform"), event.get("error"));
    }

    @EventListener
    void everything(FoPostWebhookEvent event) {
        log.debug("{} (delivery {})", event.getEvent(), event.getDeliveryId());
    }
}
```

| Event | Class |
| --- | --- |
| `post.published` | `FoPostPostPublishedEvent` |
| `post.failed` | `FoPostPostFailedEvent` |
| `post.partially_failed` | `FoPostPostPartiallyFailedEvent` |
| `delivery.published` | `FoPostDeliveryPublishedEvent` |
| `delivery.failed` | `FoPostDeliveryFailedEvent` |
| `delivery.delayed` | `FoPostDeliveryDelayedEvent` |
| `account.health_changed` | `FoPostAccountHealthChangedEvent` |
| anything newer | `FoPostWebhookEvent` |

Listeners run synchronously on the request thread. An exception that escapes one becomes a 500 and
FoPost retries the delivery — useful on purpose, but annotate the listener with `@Async` if you
would rather acknowledge first. `event.getDeliveryId()` is unique per attempt, so keep your handler
idempotent.

If your application uses Spring Security, permit the webhook path: FoPost signs its deliveries but
does not carry a session or a bearer token.

## Health

With Spring Boot Actuator on the classpath, `/actuator/health` gains a `fopost` entry that lists
the workspaces the key can reach:

```json
{ "status": "UP", "details": { "baseUrl": "https://api.fopost.com", "workspaces": 2 } }
```

A failure reports the API's status and error code. The key never appears in the response. Switch
the entry off with `management.health.fopost.enabled=false`.

## Example

[`examples/spring-boot-app`](examples/spring-boot-app) publishes a post, receives webhooks, and
exposes the health entry.

## Contributing

Issues and pull requests are welcome at
[fopost/fopost-spring](https://github.com/fopost/fopost-spring).

```bash
mvn verify
```

Tests run against stub transports and a MockMvc request; nothing touches the network. Until
`com.fopost:fopost-java` is on Maven Central, install it from source first — see `CLAUDE.md`.

## License

MIT

Questions or a problem: [fopost.com/contact](https://fopost.com/contact).
