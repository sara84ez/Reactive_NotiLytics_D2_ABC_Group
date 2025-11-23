package services;

import models.WordCount;
import models.WordStatsResult;
import org.junit.Test;
import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WordStatisticsServiceTest {
    @Test
    public void computeWordCountsReturnsRightCountTest() {
        WordStatisticsService svc = new WordStatisticsService();

        ObjectNode a1 = JsonNodeFactory.instance.objectNode();
        a1.put("description", "This is a news article with news");
        a1.put("publishedAt", "2025-11-05T00:00:00Z");

        ObjectNode a2 = JsonNodeFactory.instance.objectNode();
        a2.put("description", "This is another news article");
        a2.put("publishedAt", "2025-11-05T09:00:00Z");

        List<JsonNode> articles = Arrays.asList(a1, a2);
        List<WordCount> counts = svc.computeWordCounts(articles);
        WordStatsResult result = new WordStatsResult(counts);

        assertFalse(counts.isEmpty());
        assertEquals("news", counts.get(0).getWord());
        assertEquals("article", counts.get(1).getWord());
        assertEquals(3, result.getCounts().get(0).getCount());
        assertEquals(2, result.getCounts().get(1).getCount());

    }

    @Test
    public void computeWordCountsWithEmptyListOrNullTest() {
        WordStatisticsService svc = new WordStatisticsService();

        List<JsonNode> articles = new ArrayList<>();
        List<WordCount> counts = svc.computeWordCounts(articles);
        List<WordCount> counts2 = svc.computeWordCounts(null);

        assertTrue(counts.isEmpty());
        assertTrue(counts2.isEmpty());
    }
}

