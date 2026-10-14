package com.fopost.spring.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fopost.sdk.WebhookVerifier;
import com.fopost.sdk.model.BlogArticlePublished;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(
        classes = {WebhookTestApplication.class, FoPostArticleControllerTests.Recorder.class},
        properties = {"fopost.blog.website-secret=whsec_site", "fopost.blog.webhook-path=/hooks/articles"})
@AutoConfigureMockMvc
class FoPostArticleControllerTests {

    private static final String SECRET = "whsec_site";

    private static final String TEST_BODY =
            "{\"event\":\"article.test\",\"data\":{},\"timestamp\":\"2026-10-11T10:00:00Z\"}";

    private static final String PUBLISHED_BODY = "{\"event\":\"article.published\",\"data\":{"
            + "\"id\":\"art_1\",\"title\":\"Scheduling posts\",\"slug\":\"scheduling-posts\","
            + "\"excerpt\":null,\"metaTitle\":null,\"metaDescription\":\"How to plan a week\","
            + "\"bodyMarkdown\":\"# Scheduling\",\"bodyHtml\":\"<h1>Scheduling</h1>\","
            + "\"featuredImage\":{\"mediaId\":\"med_1\",\"url\":\"https://cdn.example.com/a.png\",\"alt\":\"Calendar\"},"
            + "\"images\":[],\"tags\":[\"scheduling\"],\"keyword\":\"schedule posts\",\"wordCount\":640,"
            + "\"publishedAt\":\"2026-10-11T09:59:00Z\",\"publishedUrl\":\"https://fopost.com/blog/scheduling-posts\"},"
            + "\"timestamp\":\"2026-10-11T10:00:00Z\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Recorder recorder;

    @BeforeEach
    void reset() {
        recorder.handled.clear();
        recorder.events.clear();
    }

    @Test
    void acknowledgesASignedTestDelivery() throws Exception {
        mvc.perform(signed(TEST_BODY, TEST_BODY))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ok\":true}"));

        assertThat(recorder.handled).isEmpty();
        assertThat(recorder.events).isEmpty();
    }

    @Test
    void rejectsAPayloadEditedAfterSigning() throws Exception {
        mvc.perform(signed(TEST_BODY, TEST_BODY.replace("article.test", "article.published")))
                .andExpect(status().isUnauthorized());

        assertThat(recorder.handled).isEmpty();
    }

    @Test
    void handsAPublishedArticleToTheHandlerAndEchoesItsUrl() throws Exception {
        mvc.perform(signed(PUBLISHED_BODY, PUBLISHED_BODY).header(WebhookVerifier.DELIVERY_HEADER, "whd_1"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"url\":\"https://example.com/blog/scheduling-posts\"}"));

        assertThat(recorder.handled).hasSize(1);
        BlogArticlePublished article = recorder.handled.get(0);
        assertThat(article.id()).isEqualTo("art_1");
        assertThat(article.slug()).isEqualTo("scheduling-posts");
        assertThat(article.bodyHtml()).isEqualTo("<h1>Scheduling</h1>");
        assertThat(article.featuredImage().alt()).isEqualTo("Calendar");
        assertThat(article.tags()).containsExactly("scheduling");
        assertThat(article.wordCount()).isEqualTo(640);
        assertThat(article.publishedAt()).isEqualTo(Instant.parse("2026-10-11T09:59:00Z"));
        assertThat(article.publishedUrl()).isEqualTo("https://fopost.com/blog/scheduling-posts");

        assertThat(recorder.events).hasSize(1);
        assertThat(recorder.events.get(0).getArticle()).isSameAs(article);
        assertThat(recorder.events.get(0).getDeliveryId()).isEqualTo("whd_1");
    }

    private static MockHttpServletRequestBuilder signed(String signedBody, String sentBody) {
        long timestamp = Instant.now().getEpochSecond();
        String signature = WebhookVerifier.sign(signedBody.getBytes(StandardCharsets.UTF_8), SECRET, timestamp);
        return post("/hooks/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .header(WebhookVerifier.TIMESTAMP_HEADER, Long.toString(timestamp))
                .header(WebhookVerifier.TIMESTAMPED_SIGNATURE_HEADER, signature)
                .content(sentBody);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Recorder {

        final List<BlogArticlePublished> handled = new ArrayList<>();
        final List<FoPostArticlePublishedEvent> events = new ArrayList<>();

        @Bean
        ArticlePublishedHandler articlePublishedHandler() {
            return article -> {
                handled.add(article);
                return "https://example.com/blog/" + article.slug();
            };
        }

        @Bean
        Listener listener() {
            return new Listener(events);
        }

        record Listener(List<FoPostArticlePublishedEvent> sink) {

            @EventListener
            void on(FoPostArticlePublishedEvent event) {
                sink.add(event);
            }
        }
    }
}
