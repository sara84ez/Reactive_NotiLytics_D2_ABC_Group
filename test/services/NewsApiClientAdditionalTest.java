package services;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.cache.AsyncCacheApi;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Additional branch coverage for NewsApiClient that works in both environments:
 * - DEMO mode (when NEWSAPI_KEY=DEMO) returns 3 static sources
 * - Non-DEMO uses network path with cache
 *
 * Keep both this test and NewsApiClientTest.
 * File name and public class name must match.
 * @author Sara
 */
public class NewsApiClientAdditionalTest {

    private WSClient ws;
    private WSRequest req;
    private WSResponse resp;
    private AsyncCacheApi cache;
    private Config config;

    private static boolean isDemo() {
        String k = System.getenv("NEWSAPI_KEY");
        return k != null && k.equalsIgnoreCase("DEMO");
    }

    @Before
    public void setup() {
        ws = Mockito.mock(WSClient.class);
        req = Mockito.mock(WSRequest.class);
        resp = Mockito.mock(WSResponse.class);
        cache = Mockito.mock(AsyncCacheApi.class);
        config = Mockito.mock(Config.class);

        // Provide config values so in non-DEMO runs the client goes through "network" branch.
        when(config.hasPath(eq("newsapi.baseUrl"))).thenReturn(true);
        when(config.getString(eq("newsapi.baseUrl"))).thenReturn("https://newsapi.org/v2");
        when(config.hasPath(eq("newsapi.key"))).thenReturn(true);
        when(config.getString(eq("newsapi.key"))).thenReturn("dummy-key");
        when(config.hasPath(eq("newsapi.cacheTtlSeconds"))).thenReturn(true);
        when(config.getInt(eq("newsapi.cacheTtlSeconds"))).thenReturn(60);

        when(ws.url(anyString())).thenReturn(req);
        when(req.addHeader(anyString(), anyString())).thenReturn(req);
        when(req.addQueryParameter(anyString(), anyString())).thenReturn(req);
    }

    @Test
    public void cacheHit_shortCircuitsNetwork_orReturnsDemoList() throws Exception {
        NewsApiClient client = new NewsApiClient(ws, config, cache);

        List<models.Source> cached = Arrays.asList(
                new models.Source("id-x","Name","Desc","https://x","general","en","us")
        );

        when(cache.getOrElseUpdate(anyString(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(cached));

        List<models.Source> out = client
                .getSources(Optional.of("us"), Optional.of("general"), Optional.empty())
                .toCompletableFuture().get();

        if (isDemo()) {
            // In DEMO, the client returns static 3 items regardless of cache.
            assertThat(out).extracting(s -> s.id)
                    .contains("reuters", "bbc-news", "the-verge");
            assertThat(out.size()).isGreaterThanOrEqualTo(3);
        } else {
            // Non-DEMO: should come back from cache supplier (our single item)
            assertThat(out).hasSize(1);
            assertThat(out.get(0).id).isEqualTo("id-x");
        }
    }

    @Test
    public void networkPath_success200_mapsJson_orDemoList() throws Exception {
        NewsApiClient client = new NewsApiClient(ws, config, cache);

        when(cache.getOrElseUpdate(anyString(), any(), anyInt()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Callable<CompletionStage<List<models.Source>>> supplier =
                            (Callable<CompletionStage<List<models.Source>>>) inv.getArgument(1);
                    return supplier.call();
                });

        ObjectNode source = JsonNodeFactory.instance.objectNode();
        source.put("id", "bbc-news");
        source.put("name", "BBC News");
        source.put("description", "desc");
        source.put("url", "https://bbc.co.uk/news");
        source.put("category", "general");
        source.put("language", "en");
        source.put("country", "gb");

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.set("sources", JsonNodeFactory.instance.arrayNode().add(source));

        when(req.get()).thenReturn(CompletableFuture.completedFuture(resp));
        when(resp.getStatus()).thenReturn(200);
        when(resp.asJson()).thenReturn(payload);

        List<models.Source> out = client
                .getSources(Optional.empty(), Optional.empty(), Optional.empty())
                .toCompletableFuture().get();

        if (isDemo()) {
            // DEMO always returns the static list (contains "bbc-news" too)
            assertThat(out).extracting(s -> s.id)
                    .contains("reuters", "bbc-news", "the-verge");
        } else {
            assertThat(out).extracting(s -> s.id).containsExactly("bbc-news");
            assertThat(out.get(0).name).isEqualTo("BBC News");
        }
    }

    @Test
    public void networkPath_non200_returnsEmpty_orDemoList() throws Exception {
        NewsApiClient client = new NewsApiClient(ws, config, cache);

        when(cache.getOrElseUpdate(anyString(), any(), anyInt()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Callable<CompletionStage<List<models.Source>>> supplier =
                            (Callable<CompletionStage<List<models.Source>>>) inv.getArgument(1);
                    return supplier.call();
                });

        when(req.get()).thenReturn(CompletableFuture.completedFuture(resp));
        when(resp.getStatus()).thenReturn(500);
        when(resp.asJson()).thenReturn(JsonNodeFactory.instance.objectNode());

        List<models.Source> out = client
                .getSources(Optional.of("gb"), Optional.empty(), Optional.empty())
                .toCompletableFuture().get();

        if (isDemo()) {
            // DEMO: همچنان لیست ثابت 3تایی
            assertThat(out).extracting(s -> s.id)
                    .contains("reuters", "bbc-news", "the-verge");
        } else {
            assertThat(out).isEmpty();
        }
    }
}
