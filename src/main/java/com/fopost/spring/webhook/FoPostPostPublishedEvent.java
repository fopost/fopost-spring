package com.fopost.spring.webhook;

/** Published on `post.published`. Every account on the post accepted it. */
public class FoPostPostPublishedEvent extends FoPostWebhookEvent {

    private static final long serialVersionUID = 1L;

    public FoPostPostPublishedEvent(Object source, FoPostWebhookDelivery delivery) {
        super(source, delivery);
    }
}
