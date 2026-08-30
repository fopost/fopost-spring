package com.fopost.spring.webhook;

import java.time.Instant;
import java.util.Map;
import org.springframework.context.ApplicationEvent;

/**
 * A verified FoPost webhook, republished on the Spring application event bus.
 *
 * <p>Listen to this type for every event, or to one of the subclasses for a single one:
 *
 * <pre>{@code
 * @EventListener
 * void onPublished(FoPostPostPublishedEvent event) {
 *     log.info("post {} went live", event.get("post_id"));
 * }
 * }</pre>
 *
 * <p>Listeners run synchronously on the request thread. An exception that escapes one becomes a
 * 500, which makes FoPost retry the delivery — throw deliberately, or annotate the listener with
 * {@code @Async} to hand off instead.
 */
public class FoPostWebhookEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final transient FoPostWebhookDelivery delivery;

    public FoPostWebhookEvent(Object source, FoPostWebhookDelivery delivery) {
        super(source);
        this.delivery = delivery;
    }

    /** Builds the subclass that matches the event name, or this type for one we do not model. */
    public static FoPostWebhookEvent of(Object source, FoPostWebhookDelivery delivery) {
        return switch (delivery.event() == null ? "" : delivery.event()) {
            case "post.published" -> new FoPostPostPublishedEvent(source, delivery);
            case "post.failed" -> new FoPostPostFailedEvent(source, delivery);
            case "post.partially_failed" -> new FoPostPostPartiallyFailedEvent(source, delivery);
            case "delivery.published" -> new FoPostDeliveryPublishedEvent(source, delivery);
            case "delivery.failed" -> new FoPostDeliveryFailedEvent(source, delivery);
            case "delivery.delayed" -> new FoPostDeliveryDelayedEvent(source, delivery);
            case "account.health_changed" -> new FoPostAccountHealthChangedEvent(source, delivery);
            default -> new FoPostWebhookEvent(source, delivery);
        };
    }

    /** The event name, e.g. {@code post.published}. */
    public String getEvent() {
        return delivery.event();
    }

    /** The event body. Never null; an empty map when the payload carried no {@code data}. */
    public Map<String, Object> getData() {
        return delivery.data();
    }

    /** One field of {@link #getData()}. */
    public Object get(String key) {
        return delivery.data().get(key);
    }

    /** When the API built the payload, or null if it sent no usable timestamp. */
    public Instant getSentAt() {
        return delivery.sentAt();
    }

    /** Value of {@code X-FoPost-Delivery} — unique per attempt, so it identifies a retry. */
    public String getDeliveryId() {
        return delivery.deliveryId();
    }

    /** The exact body the signature was verified against. */
    public String getRawBody() {
        return delivery.rawBody();
    }

    /** The whole delivery, if you would rather destructure it. */
    public FoPostWebhookDelivery getDelivery() {
        return delivery;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getEvent() + " delivery=" + getDeliveryId() + "]";
    }
}
