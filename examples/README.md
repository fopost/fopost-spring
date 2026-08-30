# Examples

## `spring-boot-app`

A Spring Boot application that publishes a post and receives FoPost webhooks.

```bash
export FOPOST_API_KEY=fp_your_key_here
export FOPOST_WORKSPACE_ID=ws_your_workspace_id
export FOPOST_WEBHOOK_SECRET=whsec_from_the_create_call

cd spring-boot-app
mvn spring-boot:run
```

- `PublishService` injects the auto-configured `FoPost` client and publishes a post.
- `WebhookListener` handles deliveries with `@EventListener`; the starter verifies the signature
  before anything reaches it.
- `GET /actuator/health` includes a `fopost` entry reporting API reachability.

Point a webhook at `https://<your host>/fopost/webhooks` and keep the secret the create call
returns — it is shown once.

The example builds against `com.fopost:fopost-spring-boot-starter:0.1.0`. Until that is on Maven
Central, run `mvn install` in the repository root first.
