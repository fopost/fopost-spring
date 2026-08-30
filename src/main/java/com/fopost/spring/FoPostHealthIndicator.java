package com.fopost.spring;

import com.fopost.sdk.FoPost;
import com.fopost.sdk.FoPostException;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Reports whether the FoPost API is reachable with the configured key, by listing the workspaces
 * that key can see.
 *
 * <p>Details carry the base URL, the workspace count, and the API's status and error code — never
 * the key, and never a message that still contains it.
 */
public class FoPostHealthIndicator implements HealthIndicator {

    private final FoPost client;
    private final String apiKey;

    public FoPostHealthIndicator(FoPost client, String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    @Override
    public Health health() {
        try {
            int workspaces = client.workspaces().list().size();
            return Health.up()
                    .withDetail("baseUrl", client.baseUrl())
                    .withDetail("workspaces", workspaces)
                    .build();
        } catch (FoPostException e) {
            Health.Builder down = Health.down()
                    .withDetail("baseUrl", client.baseUrl())
                    .withDetail("status", e.status())
                    .withDetail("reason", redact(e.getMessage()));
            if (e.code() != null) {
                down.withDetail("error", e.code());
            }
            return down.build();
        } catch (RuntimeException e) {
            return Health.down()
                    .withDetail("baseUrl", client.baseUrl())
                    .withDetail("reason", redact(e.getMessage()))
                    .build();
        }
    }

    /** Belt and braces: an upstream message must never carry the key into the actuator response. */
    private String redact(String message) {
        if (message == null) {
            return "unavailable";
        }
        return apiKey == null || apiKey.isEmpty() ? message : message.replace(apiKey, "***");
    }
}
