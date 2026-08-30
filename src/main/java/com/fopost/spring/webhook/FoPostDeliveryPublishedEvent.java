package com.fopost.spring.webhook;

/** Published on `delivery.published`. One account's copy of a post went live. */
public class FoPostDeliveryPublishedEvent extends FoPostWebhookEvent {

    private static final long serialVersionUID = 1L;

    public FoPostDeliveryPublishedEvent(Object source, FoPostWebhookDelivery delivery) {
        super(source, delivery);
    }
}
