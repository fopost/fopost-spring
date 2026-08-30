package com.fopost.spring.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Checks the {@code X-FoPost-Signature} header FoPost sends with every delivery.
 *
 * <p>The header is {@code sha256=<hex>}, where the hex is an HMAC-SHA256 of the raw request body
 * keyed with the webhook's signing secret. Sign the bytes as they arrived: re-serialising a parsed
 * body reorders keys and changes whitespace, and the signature will not match.
 */
public final class FoPostWebhookSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    private final SecretKeySpec key;

    public FoPostWebhookSignatureVerifier(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("fopost: a webhook signing secret is required");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /** The value FoPost would send in {@code X-FoPost-Signature} for this body. */
    public String sign(byte[] body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return PREFIX + HexFormat.of().formatHex(mac.doFinal(body == null ? new byte[0] : body));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("fopost: HMAC-SHA256 is unavailable", e);
        }
    }

    /**
     * Whether {@code header} signs {@code body}. Compared in constant time, so a mismatch tells an
     * attacker nothing about how far it got.
     */
    public boolean verify(byte[] body, String header) {
        if (header == null || header.isBlank()) {
            return false;
        }
        String presented = header.startsWith(PREFIX) ? header : PREFIX + header;
        return MessageDigest.isEqual(
                sign(body).getBytes(StandardCharsets.US_ASCII),
                presented.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
}
