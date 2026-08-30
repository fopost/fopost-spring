package com.fopost.spring.webhook;

/** Published on `account.health_changed`. A connected account's health moved, e.g. its token expired. */
public class FoPostAccountHealthChangedEvent extends FoPostWebhookEvent {

    private static final long serialVersionUID = 1L;

    public FoPostAccountHealthChangedEvent(Object source, FoPostWebhookDelivery delivery) {
        super(source, delivery);
    }
}
