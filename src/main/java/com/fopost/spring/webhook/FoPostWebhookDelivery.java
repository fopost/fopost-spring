package com.fopost.spring.webhook;

import java.time.Instant;
import java.util.Map;

/**
 * One decoded webhook delivery, as it arrived on the wire.
 *
 * <p>FoPost posts {@code {"event": ..., "data": {...}, "timestamp": ...}} with the delivery id in
 * the {@code X-FoPost-Delivery} header.
 *
 * @param event the event name, e.g. {@code post.published}
 * @param data the event body — shape depends on the event
 * @param sentAt when the API built the payload, or {@code null} if it sent no usable timestamp
 * @param deliveryId value of {@code X-FoPost-Delivery}, unique per attempt
 * @param rawBody the exact bytes the signature was checked against, as UTF-8 text
 */
public record FoPostWebhookDelivery(
        String event, Map<String, Object> data, Instant sentAt, String deliveryId, String rawBody) {}
