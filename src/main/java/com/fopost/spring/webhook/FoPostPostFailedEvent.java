package com.fopost.spring.webhook;

/** Published on `post.failed`. No account accepted the post. */
public class FoPostPostFailedEvent extends FoPostWebhookEvent {

    private static final long serialVersionUID = 1L;

    public FoPostPostFailedEvent(Object source, FoPostWebhookDelivery delivery) {
        super(source, delivery);
    }
}
