package models;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import services.NewsService;
import services.WordStatisticsService;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;

/**
 * Unit tests for models.WordStatsViewModel#getWordCountsForSessionAsync
 * Styled like `IndexViewModelTest`
 */
public class WordStatsViewModelTest {

    private NewsService newsService;
    private WordStatisticsService statsService;

    @Before
    public void setUp() {
        newsService = mock(NewsService.class);
        statsService = mock(WordStatisticsService.class);
    }

    @Test
    public void getWordCountsForSessionAsyncWithNonEmptySearchResult() throws ExecutionException, InterruptedException, TimeoutException {
        ObjectNode articles = JsonNodeFactory.instance.objectNode();
        ArrayNode articlesArray = JsonNodeFactory.instance.arrayNode();
        articlesArray.addObject().put("title", "a");
        articlesArray.addObject().put("title", "b");
        articles.set("articles", articlesArray);
        articles.put("totalResults", 2);

        when(newsService.getSpecificNumberOfArticles("test", "publishedAt", 50))
                .thenReturn(CompletableFuture.completedFuture(articles));
        when(statsService.computeWordCounts(any())).thenReturn(Collections.emptyList());

        WordStatsViewModel vm = new WordStatsViewModel(statsService, newsService);
        vm.getWordCountsForSessionAsync("test").toCompletableFuture().get(2, TimeUnit.SECONDS);

        verify(newsService, times(1)).getSpecificNumberOfArticles(anyString(), anyString(), anyInt());
    }

    @Test
    public void getWordCountsForSessionAsyncWithEmptySearchResult() throws ExecutionException, InterruptedException, TimeoutException {
        ObjectNode articles = JsonNodeFactory.instance.objectNode();

        when(newsService.getSpecificNumberOfArticles("test", "publishedAt", 50))
                .thenReturn(CompletableFuture.completedFuture(articles));
        when(statsService.computeWordCounts(any())).thenReturn(Collections.emptyList());

        WordStatsViewModel vm = new WordStatsViewModel(statsService, newsService);
        vm.getWordCountsForSessionAsync("test").toCompletableFuture().get(2, TimeUnit.SECONDS);

        verify(newsService, times(1)).getSpecificNumberOfArticles(anyString(), anyString(), anyInt());
    }

    @Test
    public void getWordCountsForSessionAsyncWithNullQuery() throws ExecutionException, InterruptedException, TimeoutException {

        WordStatsViewModel vm = new WordStatsViewModel(statsService, newsService);
        vm.getWordCountsForSessionAsync(null).toCompletableFuture().get(2, TimeUnit.SECONDS);

        verify(newsService, times(0)).getSpecificNumberOfArticles(anyString(), anyString(), anyInt());
    }
}