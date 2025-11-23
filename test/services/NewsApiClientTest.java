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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Resilient tests for NewsApiClient covering DEMO and non-DEMO paths.
 * - If NEWSAPI_KEY=DEMO: expects static demo sources (reuters/bbc-news/the-verge).
 * - Else: stubs WS calls to return a single "bbc-news" source.
 * This avoids NullPointerExceptions when the environment isn't DEMO.
 * Keep this class name in a file named NewsApiClientTest.java
 * @author Sara
 */
public class NewsApiClientTest {

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

        // Default config: allow network branch when not in DEMO
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
    public void demo_or_network_returnsSources_withoutNulls() throws Exception {
        NewsApiClient client = new NewsApiClient(ws, config, cache);

        if (isDemo()) {
            // In DEMO: directly call, expect static list
            List<models.Source> sources = client
                    .getSources(Optional.empty(), Optional.empty(), Optional.empty())
                    .toCompletableFuture().get();

            assertThat(sources).extracting(s -> s.id)
                    .contains("reuters", "bbc-news", "the-verge");
            assertThat(sources.size()).isGreaterThanOrEqualTo(3);
        } else {
            // Not DEMO: stub cache MISS -> supplier executes -> WS 200 with one source
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

            List<models.Source> sources = client
                    .getSources(Optional.empty(), Optional.empty(), Optional.empty())
                    .toCompletableFuture().get();

            assertThat(sources).extracting(s -> s.id).containsExactly("bbc-news");
            assertThat(sources.get(0).name).isEqualTo("BBC News");
        }
    }
}
