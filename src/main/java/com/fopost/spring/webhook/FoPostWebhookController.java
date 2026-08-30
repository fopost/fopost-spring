package com.fopost.spring.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fopost.spring.FoPostWebhookDefaults;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives FoPost webhook deliveries, verifies the signature, and republishes each one as a
 * {@link FoPostWebhookEvent} so listeners handle it with {@code @EventListener}.
 *
 * <p>Mapped only when {@code fopost.webhook.enabled} is true, at {@code fopost.webhook.path}.
 * The endpoint answers 401 for a body the secret does not sign and 400 for one that is not JSON,
 * and it never echoes the payload back.
 */
@RestController
public class FoPostWebhookController {

    /** Header carrying {@code sha256=<hex>} over the raw body. */
    public static final String SIGNATURE_HEADER = "X-FoPost-Signature";

    /** Header carrying the event name, mirrored in the body. */
    public static final String EVENT_HEADER = "X-FoPost-Event";

    /** Header carrying the per-attempt delivery id. */
    public static final String DELIVERY_HEADER = "X-FoPost-Delivery";

    private final FoPostWebhookSignatureVerifier verifier;
    private final ApplicationEventPublisher events;
    private final ObjectMapper mapper;

    public FoPostWebhookController(
            FoPostWebhookSignatureVerifier verifier,
            ApplicationEventPublisher events,
            ObjectMapper mapper) {
        this.verifier = verifier;
        this.events = events;
        this.mapper = mapper;
    }

    @PostMapping(path = FoPostWebhookDefaults.PATH_EXPRESSION, consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> receive(
            @RequestBody(required = false) byte[] body,
            @RequestHeader(name = SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(name = EVENT_HEADER, required = false) String eventHeader,
            @RequestHeader(name = DELIVERY_HEADER, required = false) String deliveryId) {

        byte[] raw = body == null ? new byte[0] : body;
        if (!verifier.verify(raw, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        JsonNode payload;
        try {
            payload = mapper.readTree(raw);
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest().build();
        }
        if (payload == null || !payload.isObject()) {
            return ResponseEntity.badRequest().build();
        }

        events.publishEvent(
                FoPostWebhookEvent.of(this, decode(payload, raw, eventHeader, deliveryId)));
        return ResponseEntity.ok().build();
    }

    private FoPostWebhookDelivery decode(
            JsonNode payload, byte[] raw, String eventHeader, String deliveryId) {
        String event = payload.path("event").asText(null);
        if (event == null || event.isBlank()) {
            event = eventHeader;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        JsonNode node = payload.get("data");
        if (node != null && node.isObject()) {
            data = mapper.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        }

        return new FoPostWebhookDelivery(
                event,
                Map.copyOf(data),
                parseInstant(payload.path("timestamp").asText(null)),
                deliveryId,
                new String(raw, StandardCharsets.UTF_8));
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
