package com.example.fopostdemo;

import com.fopost.spring.webhook.FoPostDeliveryFailedEvent;
import com.fopost.spring.webhook.FoPostPostPublishedEvent;
import com.fopost.spring.webhook.FoPostWebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The starter verifies the signature and republishes each delivery, so a listener is all it takes.
 * Listen to a subclass for one event, or to {@link FoPostWebhookEvent} for every one.
 */
@Component
public class WebhookListener {

    private static final Logger log = LoggerFactory.getLogger(WebhookListener.class);

    @EventListener
    public void onPublished(FoPostPostPublishedEvent event) {
        log.info("post {} is live", event.get("post_id"));
    }

    @EventListener
    public void onDeliveryFailed(FoPostDeliveryFailedEvent event) {
        log.warn("delivery failed on {}: {}", event.get("platform"), event.get("error"));
    }

    @EventListener
    public void onAnything(FoPostWebhookEvent event) {
        log.debug("received {} (delivery {})", event.getEvent(), event.getDeliveryId());
    }
}
