package com.fopost.spring.webhook;

/** Published on `delivery.failed`. One account's copy of a post could not be sent. */
public class FoPostDeliveryFailedEvent extends FoPostWebhookEvent {

    private static final long serialVersionUID = 1L;

    public FoPostDeliveryFailedEvent(Object source, FoPostWebhookDelivery delivery) {
        super(source, delivery);
    }
}
