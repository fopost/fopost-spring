# CLAUDE.md

Guidance for Claude Code (claude.ai/code) when working in this repository.

## What This Is

`com.fopost:fopost-spring-boot-starter` — the official Spring Boot 3 starter for the FoPost API,
published to Maven Central. It is a **thin wrapper** around `com.fopost:fopost-java`. HTTP,
retries, pagination, models, params, and the error hierarchy all live in the parent SDK and must
never be reimplemented here. What belongs here is Spring wiring and nothing else: auto-configured
beans, `@ConfigurationProperties`, configuration metadata, an actuator health indicator, and the
inbound webhook controller.

If a change would add API knowledge — a new endpoint, a new model, a retry rule — it belongs in
`fopost-java`, not here.

## Brand Rules

- The product is **FoPost** (`fopost.com`). Never write "OwlStack" — retired Aug 2026.
- Never write an email address. Support is https://fopost.com/contact and GitHub issues.
- Never name AI providers/models, infrastructure vendors, or any person.

## Architecture

```
src/main/java/com/fopost/spring/
  FoPostProperties.java              @ConfigurationProperties(prefix = "fopost"), @Validated
  FoPostWebhookDefaults.java         default path + the ${...} expression the controller maps on
  FoPostAutoConfiguration.java       builds the FoPost bean
  FoPostHealthIndicator.java         actuator entry, redacts the key out of any detail
  FoPostHealthAutoConfiguration.java
  webhook/
    FoPostWebhookAutoConfiguration.java
    FoPostWebhookController.java     raw-body signature check, then publishes the event
    FoPostWebhookSignatureVerifier.java
    FoPostWebhookDelivery.java       decoded payload record
    FoPostWebhookEvent.java          base ApplicationEvent + `of(...)` dispatch to a subclass
    FoPost*Event.java                one per modelled event name
src/main/resources/META-INF/
  spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  additional-spring-configuration-metadata.json
```

Three auto-configurations, registered through the **Boot 3 imports file** — never `spring.factories`,
which Boot 3 ignores:

1. `FoPostAutoConfiguration` — `@ConditionalOnClass(FoPost.class)` plus
   `@ConditionalOnProperty(prefix = "fopost", name = "api-key")`, so nothing at all is registered
   until a key is configured. The `FoPost` bean is `@ConditionalOnMissingBean`, so a user-declared
   bean always wins.
2. `FoPostHealthAutoConfiguration` — `@ConditionalOnClass(HealthIndicator.class)` and
   `@ConditionalOnBean(FoPost.class)`, ordered `after` the client. Off with
   `management.health.fopost.enabled=false`.
3. `webhook/FoPostWebhookAutoConfiguration` — servlet web applications only, gated on
   `fopost.webhook.enabled=true`. Independent of the client: an application may receive webhooks
   without an API key.

**Why `api-key` carries no `@NotBlank`.** Both the client and the webhook auto-configuration bind
the same properties class, and a webhook-only application has no key. The blank check therefore
lives in `FoPostAutoConfiguration.foPost(...)`, which only runs when a client is actually being
built. `@Validated` still covers `base-url`, `max-retries`, and `webhook.path`, and a JSR-303
implementation must be on the classpath for those to fire (`spring-boot-starter-validation`).

**Configuration metadata.** `spring-boot-configuration-processor` runs through an explicit
`annotationProcessorPaths` entry in the POM. It is not optional plumbing: a JDK 23 or newer javac
refuses to pick a processor off the classpath, and without the entry no
`spring-configuration-metadata.json` is generated and IDE autocomplete silently disappears.
`additional-spring-configuration-metadata.json` adds what the processor cannot infer —
`management.health.fopost.enabled`, group descriptions, and hints.

### Webhook signature scheme

Determined from the API source, `apps/api/src/services/webhook-dispatcher.ts` (`signPayload`) and
`apps/api/src/workers/webhook.worker.ts` in the `fopost` monorepo:

- Body: `{"event": "<name>", "data": {...}, "timestamp": "<ISO 8601>"}`
- `X-FoPost-Signature: sha256=<hex>` — HMAC-SHA256 of the **exact request body bytes**, keyed with
  the webhook's signing secret, lowercase hex
- `X-FoPost-Event: <name>` and `X-FoPost-Delivery: <id>` (unique per attempt)
- Delivery is retried up to 5 times with exponential backoff on any non-2xx; a webhook is
  auto-disabled after 10 consecutive failures

The controller therefore takes `@RequestBody byte[]` and verifies before parsing. Re-serialising a
parsed body reorders keys and the signature will not match. The comparison is
`MessageDigest.isEqual`. If either of those changes, the tests in
`FoPostWebhookSignatureVerifierTests` are the thing to update first.

## Parent dependency

**`com.fopost:fopost-java` is not on Maven Central yet.** `pom.xml` declares the normal released
coordinate `com.fopost:fopost-java:0.1.0` — that is what ships — but nothing can resolve it, so
both workflows build the parent from source into the local repository before running Maven:

```yaml
- name: Install the parent SDK from source
  run: |
    git clone --depth 1 https://github.com/fopost/fopost-java.git "${RUNNER_TEMP}/fopost-java"
    mvn -B -q install -DskipTests -f "${RUNNER_TEMP}/fopost-java/pom.xml"
```

Locally, the sibling checkout does the same job:

```bash
mvn -q install -DskipTests -f ../fopost-java/pom.xml
```

**This shim is deletable the moment `com.fopost:fopost-java:0.1.0` is on Central** — remove the
step from `.github/workflows/ci.yml` and `.github/workflows/release.yml`, and drop the note from
the README. Nothing in `pom.xml` changes.

Until then, `release.yml` will fail at the publish step even with correct secrets: Central rejects
a release whose declared dependency it cannot resolve. Publish `fopost-java` first.

## Commands

```bash
mvn -q install -DskipTests -f ../fopost-java/pom.xml   # once, until the parent is on Central
mvn verify                                             # compile, test, sources jar, javadoc jar
mvn -B test
mvn test -Dtest=FoPostWebhookControllerTests
mvn -Prelease deploy                                   # signs and publishes; CI only
mvn -q compile -f examples/spring-boot-app/pom.xml   # the example builds against the installed starter
```

Java 17 is the release target (`maven.compiler.release`); a newer JDK builds it fine.

## Conventions

- One auto-configuration per concern, each independently conditional. Never make one of them
  require another's beans except through an explicit `after` plus `@ConditionalOnBean`.
- Every bean the starter contributes is `@ConditionalOnMissingBean`. A user's bean always wins.
- Adding a property means three edits in one commit: the field on `FoPostProperties` with a javadoc
  sentence, an entry in `additional-spring-configuration-metadata.json` if the processor cannot
  infer it, and the README table.
- Adding an auto-configuration means adding its class to
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Never leak the API key into a health detail, a log line, or an exception message.
- Tests are offline. `ApplicationContextRunner` / `WebApplicationContextRunner` for
  auto-configuration, `@SpringBootTest` plus `MockMvc` for the controller, and `StubTransport` for
  anything that would otherwise reach the network. The MockMvc host application lives in
  `com.fopost.spring.testapp` on purpose, so component scanning cannot register the starter's
  controller behind the auto-configuration's back.
- Everything optional in the POM — `spring-boot-starter-web`, `spring-boot-actuator`,
  `jakarta.validation-api` — is optional because the matching auto-configuration backs off without
  it. Do not promote one to a required dependency.

## Releasing

Tag `v<version>` matching `pom.xml`; `.github/workflows/release.yml` publishes to Maven Central
through the Sonatype Central Portal with GPG-signed sources and javadoc jars. Requires four repo
secrets on the `central` environment:

| Secret | What it is |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Central Portal token username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal token password |
| `GPG_PRIVATE_KEY` | ASCII-armoured private key used to sign artifacts |
| `GPG_PASSPHRASE` | Passphrase for that key |

Central rejects a release without full POM metadata, so keep `name`, `description`, `url`,
`licenses`, `developers`, and `scm` in `pom.xml`.

## Git

Conventional Commits, atomic. Branch `feature/<description>`, merge to `main` via PR.
Never `gh pr create` — push the branch and hand over the compare link.
