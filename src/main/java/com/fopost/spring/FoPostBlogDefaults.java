package com.fopost.spring;

/** Defaults shared between the properties class and the article receiver's request mapping. */
public final class FoPostBlogDefaults {

    private FoPostBlogDefaults() {}

    /** Where the article receiver listens unless {@code fopost.blog.webhook-path} says otherwise. */
    public static final String PATH = "/fopost/articles";

    /** Placeholder the receiver's {@code @PostMapping} resolves at startup. */
    public static final String PATH_EXPRESSION = "${fopost.blog.webhook-path:" + PATH + "}";
}
