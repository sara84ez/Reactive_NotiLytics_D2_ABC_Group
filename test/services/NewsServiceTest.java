package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import play.cache.AsyncCacheApi;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Pham Bao Quynh Nguyen
 */
public class NewsServiceTest {

    @Mock private WSClient wsClient;
    @Mock private AsyncCacheApi cache;
    @Mock private WSRequest wsRequest;
    @Mock private WSResponse wsResponse;

    private ObjectMapper objectMapper;
    private NewsService newsService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        objectMapper = new ObjectMapper();
        newsService = new NewsService(wsClient, cache, objectMapper);
    }

    @Test
    public void testSearchNews() throws Exception {
        // Arrange
        String query = "bitcoin";
        String sortBy = "latest";

        ObjectNode fakeJson = JsonNodeFactory.instance.objectNode();
        fakeJson.put("status", "ok");

        when(wsClient.url(any(String.class))).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
        when(wsResponse.asJson()).thenReturn(fakeJson);

        // Mock cache: directly execute the supplier (simulate cache miss)

        when(cache.getOrElseUpdate(eq("bitcoin:latest"), any(), eq(300)))
                .thenAnswer(invocation -> {
                    return ((java.util.concurrent.Callable<CompletionStage<JsonNode>>) invocation.getArguments()[1]).call();
                });

        // Act
        JsonNode result = newsService.searchNews(query, sortBy).toCompletableFuture().join();

        // Assert
        assertThat(result.get("status").asText()).isEqualTo("ok");
        verify(wsClient, times(1)).url(any(String.class));
        verify(wsRequest, times(1)).get();
    }

    @Test
    public void testGetSourceInfo_returnsMatchingSource() throws Exception {
        String domain = "cnn.com";

        ObjectNode sourceNode = JsonNodeFactory.instance.objectNode();
        sourceNode.put("url", "https://www.cnn.com");

        ObjectNode jsonResponse = JsonNodeFactory.instance.objectNode();
        jsonResponse.set("sources", JsonNodeFactory.instance.arrayNode().add(sourceNode));

        when(wsClient.url(any(String.class))).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
        when(wsResponse.asJson()).thenReturn(jsonResponse);

        when(cache.getOrElseUpdate(eq("sourceInfo_cnn.com"), any(), eq(300)))
                .thenAnswer(invocation -> {
                    return ((java.util.concurrent.Callable<CompletionStage<JsonNode>>) invocation.getArguments()[1]).call();
                });

        JsonNode result = newsService.getSourceInfo(domain).toCompletableFuture().join();

        assertThat(result.get("url").asText()).contains("cnn.com");
    }


    @Test
    public void testGetTop10ArticlesForSpecificSource_returnsArticlesList() throws Exception {
        String domain = "bbc.com";

        ObjectNode article = JsonNodeFactory.instance.objectNode();
        article.put("title", "News headline");

        ObjectNode jsonResponse = JsonNodeFactory.instance.objectNode();
        jsonResponse.set("articles", JsonNodeFactory.instance.arrayNode().add(article));

        when(wsClient.url(any(String.class))).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
        when(wsResponse.asJson()).thenReturn(jsonResponse);

        when(cache.getOrElseUpdate(eq("top10_bbc.com"), any(), eq(300)))
                .thenAnswer(invocation -> {
                    return ((java.util.concurrent.Callable<CompletionStage<List<JsonNode>>>) invocation.getArguments()[1]).call();
                });

        List<JsonNode> articles = newsService.getTop10ArticlesForSpecificSource(domain)
                .toCompletableFuture().join();

        assertThat(articles).hasSize(1);
        assertThat(articles.get(0).get("title").asText()).isEqualTo("News headline");
    }

    @Test
    public void testGetTop10Articles_InvalidDomain() throws Exception {
        String domain = "invalid_domain_@#";

        when(cache.getOrElseUpdate(anyString(), any(), anyInt()))
                .thenAnswer(invocation -> {
                    java.util.concurrent.Callable<CompletionStage<List<JsonNode>>> callable =
                            invocation.getArgument(1);
                    return callable.call();
                });

        // Mock WS returning empty JSON
        JsonNode jsonResponse = objectMapper.createObjectNode();

        when(wsClient.url(anyString())).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
        when(wsResponse.asJson()).thenReturn(jsonResponse);

        List<JsonNode> result = newsService.getTop10ArticlesForSpecificSource(domain)
                .toCompletableFuture().join();

        assertThat(result).isEmpty();
    }

    @Test
    public void testGetTop10ArticlesForSpecificSource_notUnderPublicSuffix() throws Exception {
        String domain = "localhost"; // valid format but not under a public suffix

        ObjectNode article = JsonNodeFactory.instance.objectNode();
        article.put("title", "Local headline");

        ObjectNode jsonResponse = JsonNodeFactory.instance.objectNode();
        jsonResponse.set("articles", JsonNodeFactory.instance.arrayNode().add(article));

        when(wsClient.url(anyString())).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
        when(wsResponse.asJson()).thenReturn(jsonResponse);

        when(cache.getOrElseUpdate(eq("top10_localhost"), any(), eq(300)))
                .thenAnswer(invocation -> {
                    java.util.concurrent.Callable<CompletionStage<List<JsonNode>>> callable =
                            invocation.getArgument(1);
                    return callable.call();
                });

        List<JsonNode> articles = newsService.getTop10ArticlesForSpecificSource(domain)
                .toCompletableFuture().join();

        assertThat(articles).hasSize(1);
        assertThat(articles.get(0).get("title").asText()).isEqualTo("Local headline");
    }

    @Test
    public void testGetTop10ArticlesForSpecificSource_withWWWPrefix() throws Exception {
        String domain = "www.bbc.com"; // triggers the 'true' branch

        ObjectNode article = JsonNodeFactory.instance.objectNode();
        article.put("title", "WWW headline");

        ObjectNode jsonResponse = JsonNodeFactory.instance.objectNode();
        jsonResponse.set("articles", JsonNodeFactory.instance.arrayNode().add(article));

        when(wsClient.url(anyString())).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
        when(wsResponse.asJson()).thenReturn(jsonResponse);

        // The edited domain becomes "bbc.com" after trimming 'www.'
        when(cache.getOrElseUpdate(eq("top10_bbc.com"), any(), eq(300)))
                .thenAnswer(invocation -> {
                    java.util.concurrent.Callable<CompletionStage<List<JsonNode>>> callable =
                            invocation.getArgument(1);
                    return callable.call();
                });

        List<JsonNode> articles = newsService.getTop10ArticlesForSpecificSource(domain)
                .toCompletableFuture().join();

        assertThat(articles).hasSize(1);
        assertThat(articles.get(0).get("title").asText()).isEqualTo("WWW headline");
    }


    //P2 Test---------------------------------------------------------------------------------------------------

    @Test
    public void getSpecificNumberOfArticlesTest(){
        String query = "bitcoin";
        String sortBy = "latest";

        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.put("status", "ok");

        when(wsClient.url(any(String.class))).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
        when(wsResponse.asJson()).thenReturn(response);


        when(cache.getOrElseUpdate(eq("bitcoin:latest:pageSize=50"), any(), eq(300)))
                .thenAnswer(invocation -> ((java.util.concurrent.Callable<CompletionStage<JsonNode>>) invocation.getArguments()[1]).call());

        JsonNode result = newsService.getSpecificNumberOfArticles(query, sortBy,50).toCompletableFuture().join();

        assertThat(result.get("status").asText()).isEqualTo("ok");
        verify(wsClient, times(1)).url(any(String.class));
        verify(wsRequest, times(1)).get();
    }


}

