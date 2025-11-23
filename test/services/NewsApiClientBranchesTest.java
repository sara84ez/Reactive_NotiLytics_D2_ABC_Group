package services;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import models.Source;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.cache.AsyncCacheApi;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Branch tests for NewsApiClient that avoid DEMO mode
 * and cover:
 *  - empty API key
 *  - unexpected JSON without "sources"
 *  - valid JSON mapping
 */
public class NewsApiClientBranchesTest {

    private WSClient ws;
    private WSRequest req;
    private WSResponse resp;
    private AsyncCacheApi cache;
    private Config config;

    @Before
    public void setup() {
        ws = Mockito.mock(WSClient.class);
        req = Mockito.mock(WSRequest.class);
        resp = Mockito.mock(WSResponse.class);
        cache = Mockito.mock(AsyncCacheApi.class);
        config = Mockito.mock(Config.class);

        // basic WS stubbing
        when(ws.url(anyString())).thenReturn(req);
        when(req.addHeader(anyString(), anyString())).thenReturn(req);
        when(req.addQueryParameter(anyString(), anyString())).thenReturn(req);
        when(req.get()).thenReturn(CompletableFuture.completedFuture(resp));

        // make cache execute supplier (so we don't get null CompletionStage)
        when(cache.getOrElseUpdate(anyString(), any(), anyInt()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Callable<CompletionStage<List<Source>>> supplier =
                            (Callable<CompletionStage<List<Source>>>) invocation.getArgument(1);
                    return supplier.call();
                });

        // basic config
        when(config.hasPath("newsapi.baseUrl")).thenReturn(true);
        when(config.getString("newsapi.baseUrl")).thenReturn("https://newsapi.org/v2");
        when(config.hasPath("newsapi.cacheTtlSeconds")).thenReturn(true);
        when(config.getInt("newsapi.cacheTtlSeconds")).thenReturn(60);
    }

    /**
     * When API key is configured but empty, fetchSources should return empty list.
     */
    @Test
    public void emptyApiKey_returnsEmptyList() throws Exception {
        when(config.hasPath("newsapi.key")).thenReturn(true);
        when(config.getString("newsapi.key")).thenReturn(""); // blank key

        NewsApiClient client = new NewsApiClient(ws, config, cache);

        List<Source> result = client
                .getSources(Optional.empty(), Optional.empty(), Optional.empty())
                .toCompletableFuture().get();

        assertThat(result).isEmpty();
    }

    /**
     * When JSON has no "sources" field, we hit the "Unexpected response" branch and get empty list,
     * but query parameters must still be added.
     */
    @Test
    public void unexpectedResponse_noSourcesField_returnsEmpty_andAddsQueryParams() throws Exception {
        when(config.hasPath("newsapi.key")).thenReturn(true);
        when(config.getString("newsapi.key")).thenReturn("test-key");

        ObjectNode json = JsonNodeFactory.instance.objectNode(); // no "sources"
        when(resp.asJson()).thenReturn(json);

        NewsApiClient client = new NewsApiClient(ws, config, cache);

        List<Source> result = client
                .getSources(Optional.of("us"), Optional.of("general"), Optional.of("en"))
                .toCompletableFuture().get();

        assertThat(result).isEmpty();

        Mockito.verify(req).addQueryParameter("country", "us");
        Mockito.verify(req).addQueryParameter("category", "general");
        Mockito.verify(req).addQueryParameter("language", "en");
    }

    /**
     * Normal successful case: JSON has one source and it's mapped correctly.
     */
    @Test
    public void validResponse_parsesSourcesCorrectly() throws Exception {
        when(config.hasPath("newsapi.key")).thenReturn(true);
        when(config.getString("newsapi.key")).thenReturn("test-key");

        ObjectNode one = JsonNodeFactory.instance.objectNode();
        one.put("id", "bbc-news");
        one.put("name", "BBC News");
        one.put("description", "desc");
        one.put("url", "https://bbc.co.uk/news");
        one.put("category", "general");
        one.put("language", "en");
        one.put("country", "gb");

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.set("sources", JsonNodeFactory.instance.arrayNode().add(one));
        when(resp.asJson()).thenReturn(payload);

        NewsApiClient client = new NewsApiClient(ws, config, cache);

        List<Source> result = client
                .getSources(Optional.empty(), Optional.empty(), Optional.empty())
                .toCompletableFuture().get();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id).isEqualTo("bbc-news");
        assertThat(result.get(0).name).isEqualTo("BBC News");
    }
}

