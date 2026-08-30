package com.fopost.spring;

import com.fopost.sdk.internal.HttpRequestData;
import com.fopost.sdk.internal.HttpResponseData;
import com.fopost.sdk.internal.Transport;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Canned responses, so nothing in this suite touches the network. */
final class StubTransport implements Transport {

    private final int status;
    private final String body;

    StubTransport(int status, String body) {
        this.status = status;
        this.body = body;
    }

    @Override
    public HttpResponseData send(HttpRequestData request) {
        return new HttpResponseData(
                status,
                Map.of("content-type", "application/json"),
                body.getBytes(StandardCharsets.UTF_8));
    }
}
