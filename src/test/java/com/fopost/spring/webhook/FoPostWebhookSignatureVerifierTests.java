package com.fopost.spring.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FoPostWebhookSignatureVerifierTests {

    private static final byte[] BODY = "{\"event\":\"post.published\"}".getBytes(StandardCharsets.UTF_8);

    private final FoPostWebhookSignatureVerifier verifier = new FoPostWebhookSignatureVerifier("whsec_test");

    @Test
    void producesThePrefixedHexTheApiSends() {
        assertThat(verifier.sign(BODY)).startsWith("sha256=").hasSize("sha256=".length() + 64);
    }

    @Test
    void acceptsItsOwnSignature() {
        assertThat(verifier.verify(BODY, verifier.sign(BODY))).isTrue();
    }

    @Test
    void acceptsABareHexSignature() {
        String bare = verifier.sign(BODY).substring("sha256=".length());
        assertThat(verifier.verify(BODY, bare)).isTrue();
    }

    @Test
    void rejectsAnotherSecretsSignature() {
        String other = new FoPostWebhookSignatureVerifier("whsec_other").sign(BODY);
        assertThat(verifier.verify(BODY, other)).isFalse();
    }

    @Test
    void rejectsATamperedBody() {
        String signature = verifier.sign(BODY);
        assertThat(verifier.verify("{\"event\":\"post.failed\"}".getBytes(StandardCharsets.UTF_8), signature))
                .isFalse();
    }

    @Test
    void rejectsAMissingSignature() {
        assertThat(verifier.verify(BODY, null)).isFalse();
        assertThat(verifier.verify(BODY, "  ")).isFalse();
    }

    @Test
    void refusesToBeBuiltWithoutASecret() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FoPostWebhookSignatureVerifier(" "));
    }
}
