package com.fopost.spring.testapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal host application for the MockMvc tests. It lives in its own package so component
 * scanning cannot reach the starter's controller — only the auto-configuration may register it.
 */
@SpringBootApplication
public class WebhookTestApplication {}
