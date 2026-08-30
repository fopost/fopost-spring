package com.fopost.spring.webhook;

/** Published on `post.partially_failed`. Some accounts took the post and some did not. */
public class FoPostPostPartiallyFailedEvent extends FoPostWebhookEvent {

    private static final long serialVersionUID = 1L;

    public FoPostPostPartiallyFailedEvent(Object source, FoPostWebhookDelivery delivery) {
        super(source, delivery);
    }
}
