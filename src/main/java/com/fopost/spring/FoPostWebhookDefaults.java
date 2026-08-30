package com.fopost.spring;

/** Defaults shared between the properties class and the controller's request mapping. */
public final class FoPostWebhookDefaults {

    private FoPostWebhookDefaults() {}

    /** Where the webhook controller listens unless {@code fopost.webhook.path} says otherwise. */
    public static final String PATH = "/fopost/webhooks";

    /** Placeholder the controller's {@code @PostMapping} resolves at startup. */
    public static final String PATH_EXPRESSION = "${fopost.webhook.path:" + PATH + "}";
}
