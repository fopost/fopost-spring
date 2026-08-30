package com.fopost.spring.webhook;

/** Published on `delivery.delayed`. One account's copy of a post was held back and will be retried. */
public class FoPostDeliveryDelayedEvent extends FoPostWebhookEvent {

    private static final long serialVersionUID = 1L;

    public FoPostDeliveryDelayedEvent(Object source, FoPostWebhookDelivery delivery) {
        super(source, delivery);
    }
}
