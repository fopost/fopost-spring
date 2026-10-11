package com.fopost.spring;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Everything the starter reads from {@code application.yml}, under the {@code fopost} prefix.
 *
 * <pre>{@code
 * fopost:
 *   api-key: ${FOPOST_API_KEY}
 *   default-workspace-id: ws_123
 *   webhook-secret: ${FOPOST_WEBHOOK_SECRET}
 *   webhook:
 *     enabled: true
 *     path: /fopost/webhooks
 *   blog:
 *     website-secret: ${FOPOST_WEBSITE_SECRET}
 *     webhook-path: /fopost/articles
 * }</pre>
 *
 * <p>Constraints are checked at startup, not at the first call, so a missing environment variable
 * shows up in the deploy instead of in production traffic.
 */
@ConfigurationProperties(prefix = FoPostProperties.PREFIX)
@Validated
public class FoPostProperties {

    /** The configuration prefix, {@code fopost}. */
    public static final String PREFIX = "fopost";

    /**
     * API key from fopost.com/dashboard/api-keys. Sent as the {@code X-API-Key} header.
     *
     * <p>Not a bean constraint, because an application may receive webhooks without ever calling
     * the API. {@link FoPostAutoConfiguration} rejects a blank one when it builds the client.
     */
    private String apiKey;

    /** API root. A host with no path gets the versioned API path appended for you. */
    @NotBlank
    private String baseUrl = "https://api.fopost.com";

    /** How long a single request may take before it gives up. */
    private Duration timeout = Duration.ofSeconds(30);

    /** Total attempts for a rate limited request, so 3 means two retries. */
    @Min(1)
    private int maxRetries = 3;

    /**
     * Workspace used when a caller does not name one. The starter never sends it on your behalf —
     * inject {@link FoPostProperties} and read it where your own code needs a default.
     */
    private String defaultWorkspaceId;

    /**
     * Signing secret of the webhook endpoint, returned once by the create call. Required when
     * {@code fopost.webhook.enabled} is true.
     */
    private String webhookSecret;

    private final Webhook webhook = new Webhook();

    private final Blog blog = new Blog();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getDefaultWorkspaceId() {
        return defaultWorkspaceId;
    }

    public void setDefaultWorkspaceId(String defaultWorkspaceId) {
        this.defaultWorkspaceId = defaultWorkspaceId;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public Blog getBlog() {
        return blog;
    }

    /** The inbound webhook endpoint this application exposes to FoPost. */
    public static class Webhook {

        /** Whether to map a controller that receives FoPost webhook deliveries. */
        private boolean enabled = false;

        /** Path the controller is mapped to. */
        @NotBlank
        private String path = FoPostWebhookDefaults.PATH;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    /** The endpoint a FoPost website target publishes articles to. */
    public static class Blog {

        /**
         * Signing secret of the website target, issued once when the website URL is set. The
         * article receiver is mapped only when this is present.
         */
        private String websiteSecret;

        /** Path the article receiver is mapped to. */
        @NotBlank
        private String webhookPath = FoPostBlogDefaults.PATH;

        public String getWebsiteSecret() {
            return websiteSecret;
        }

        public void setWebsiteSecret(String websiteSecret) {
            this.websiteSecret = websiteSecret;
        }

        public String getWebhookPath() {
            return webhookPath;
        }

        public void setWebhookPath(String webhookPath) {
            this.webhookPath = webhookPath;
        }
    }
}
