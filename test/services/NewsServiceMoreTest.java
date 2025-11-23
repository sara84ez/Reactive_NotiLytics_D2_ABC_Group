package services;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import play.cache.AsyncCacheApi;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Per-test stubbing to avoid null suppliers.
 * Covers trimming www, invalid domain catch, empty/missing articles, and cache hit.
 * @author Sara
 */
public class NewsServiceMoreTest {

    private WSClient wsClient;
    private WSRequest wsRequest;
    private WSResponse wsResponse;
    private AsyncCacheApi cache;
    private ObjectMapper objectMapper;
    private NewsService service;

    @Before
    public void setup() {
        wsClient = Mockito.mock(WSClient.class);
        wsRequest = Mockito.mock(WSRequest.class);
        wsResponse = Mockito.mock(WSResponse.class);
        cache = Mockito.mock(AsyncCacheApi.class);
        objectMapper = new ObjectMapper();
        service = new NewsService(wsClient, cache, objectMapper);

        when(wsClient.url(anyString())).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
    }

    @Test
    public void trims_www_prefix_and_returns_empty_when_no_articles() throws Exception {
        // Execute supplier path
        when(cache.getOrElseUpdate(anyString(), any(), anyInt()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Callable<CompletionStage<List<com.fasterxml.jackson.databind.JsonNode>>> supplier =
                            (Callable<CompletionStage<List<com.fasterxml.jackson.databind.JsonNode>>>) inv.getArgument(1);
                    return supplier.call();
                });

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.set("articles", JsonNodeFactory.instance.arrayNode()); // empty
        when(wsResponse.asJson()).thenReturn(payload);

        List<com.fasterxml.jackson.databind.JsonNode> list =
                service.getTop10ArticlesForSpecificSource("www.bbc.co.uk").toCompletableFuture().get();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(wsClient).url(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).contains("domains=bbc.co.uk").contains("pageSize=10");
        assertThat(list).isEmpty();
    }

    @Test
    public void invalid_domain_caught_and_still_requests_and_handles_missing_articles() throws Exception {
        when(cache.getOrElseUpdate(anyString(), any(), anyInt()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Callable<CompletionStage<List<com.fasterxml.jackson.databind.JsonNode>>> supplier =
                            (Callable<CompletionStage<List<com.fasterxml.jackson.databind.JsonNode>>>) inv.getArgument(1);
                    return supplier.call();
                });

        ObjectNode payload = JsonNodeFactory.instance.objectNode(); // no "articles"
        when(wsResponse.asJson()).thenReturn(payload);

        List<com.fasterxml.jackson.databind.JsonNode> list =
                service.getTop10ArticlesForSpecificSource("%%%invalid%%%").toCompletableFuture().get();

        verify(wsClient).url(anyString());
        assertThat(list).isEmpty();
    }

    @Test
    public void cache_hit_returns_cached_result() throws Exception {
        java.util.List<com.fasterxml.jackson.databind.JsonNode> cached =
                java.util.Arrays.asList(JsonNodeFactory.instance.objectNode().put("title", "cached"));

        // Directly return cached result; do NOT execute supplier here
        when(cache.getOrElseUpdate(anyString(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(cached));

        List<com.fasterxml.jackson.databind.JsonNode> list =
                service.getTop10ArticlesForSpecificSource("cnn.com").toCompletableFuture().get();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("title").asText()).isEqualTo("cached");
    }
}
