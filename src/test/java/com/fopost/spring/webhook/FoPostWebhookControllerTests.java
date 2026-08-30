package com.fopost.spring.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fopost.spring.testapp.WebhookTestApplication;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = {WebhookTestApplication.class, FoPostWebhookControllerTests.Recorder.class},
        properties = {
            "fopost.webhook.enabled=true",
            "fopost.webhook-secret=whsec_test",
            "fopost.webhook.path=/hooks/fopost"
        })
@AutoConfigureMockMvc
class FoPostWebhookControllerTests {

    private static final String BODY =
            "{\"event\":\"post.published\",\"data\":{\"post_id\":\"post_1\",\"workspace_id\":\"ws_1\"},"
                    + "\"timestamp\":\"2026-08-30T10:00:00Z\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Recorder recorder;

    @Autowired
    private FoPostWebhookSignatureVerifier verifier;

    @BeforeEach
    void reset() {
        recorder.received.clear();
    }

    @Test
    void rejectsABodyTheSecretDoesNotSign() throws Exception {
        mvc.perform(post("/hooks/fopost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FoPostWebhookController.SIGNATURE_HEADER, "sha256=" + "0".repeat(64))
                        .content(BODY))
                .andExpect(status().isUnauthorized());

        assertThat(recorder.received).isEmpty();
    }

    @Test
    void rejectsAnUnsignedDelivery() throws Exception {
        mvc.perform(post("/hooks/fopost").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        assertThat(recorder.received).isEmpty();
    }

    @Test
    void rejectsAPayloadEditedAfterSigning() throws Exception {
        String signature = sign(BODY);

        mvc.perform(post("/hooks/fopost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FoPostWebhookController.SIGNATURE_HEADER, signature)
                        .content(BODY.replace("post_1", "post_2")))
                .andExpect(status().isUnauthorized());

        assertThat(recorder.received).isEmpty();
    }

    @Test
    void acceptsASignedDeliveryAndPublishesTheTypedEvent() throws Exception {
        mvc.perform(post("/hooks/fopost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FoPostWebhookController.SIGNATURE_HEADER, sign(BODY))
                        .header(FoPostWebhookController.EVENT_HEADER, "post.published")
                        .header(FoPostWebhookController.DELIVERY_HEADER, "whd_1")
                        .content(BODY))
                .andExpect(status().isOk());

        assertThat(recorder.received).hasSize(1);
        FoPostWebhookEvent event = recorder.received.get(0);
        assertThat(event).isInstanceOf(FoPostPostPublishedEvent.class);
        assertThat(event.getEvent()).isEqualTo("post.published");
        assertThat(event.get("post_id")).isEqualTo("post_1");
        assertThat(event.getDeliveryId()).isEqualTo("whd_1");
        assertThat(event.getSentAt()).isEqualTo(Instant.parse("2026-08-30T10:00:00Z"));
        assertThat(event.getRawBody()).isEqualTo(BODY);
    }

    @Test
    void publishesTheBaseTypeForAnEventItDoesNotModel() throws Exception {
        String body = "{\"event\":\"post.something_new\",\"data\":{}}";

        mvc.perform(post("/hooks/fopost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FoPostWebhookController.SIGNATURE_HEADER, sign(body))
                        .content(body))
                .andExpect(status().isOk());

        assertThat(recorder.received).hasSize(1);
        assertThat(recorder.received.get(0)).isExactlyInstanceOf(FoPostWebhookEvent.class);
    }

    @Test
    void rejectsASignedBodyThatIsNotJsonObject() throws Exception {
        String body = "not json";

        mvc.perform(post("/hooks/fopost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FoPostWebhookController.SIGNATURE_HEADER, sign(body))
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(recorder.received).isEmpty();
    }

    @Test
    void isNotMappedOnTheDefaultPathWhenAnotherOneIsConfigured() throws Exception {
        mvc.perform(post("/fopost/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FoPostWebhookController.SIGNATURE_HEADER, sign(BODY))
                        .content(BODY))
                .andExpect(status().isNotFound());
    }

    private String sign(String body) {
        return verifier.sign(body.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Recorder {

        final List<FoPostWebhookEvent> received = new ArrayList<>();

        @Bean
        Listener listener() {
            return new Listener(received);
        }

        record Listener(List<FoPostWebhookEvent> sink) {

            @EventListener
            void on(FoPostWebhookEvent event) {
                sink.add(event);
            }
        }
    }
}
