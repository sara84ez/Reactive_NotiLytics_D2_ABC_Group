package models;

import org.junit.Test;
import static org.junit.Assert.*;

public class SourceTest {

    @Test
    public void testConstructorAndFields() {
        Source source = new Source(
                "bbc-news",
                "BBC News",
                "British news provider",
                "https://www.bbc.co.uk/news",
                "general",
                "en",
                "gb"
        );

        assertEquals("bbc-news", source.id);
        assertEquals("BBC News", source.name);
        assertEquals("British news provider", source.description);
        assertEquals("https://www.bbc.co.uk/news", source.url);
        assertEquals("general", source.category);
        assertEquals("en", source.language);
        assertEquals("gb", source.country);
    }

    @Test
    public void testToStringNotNullOrEmpty() {
        Source source = new Source(
                "cnn",
                "CNN",
                "US news channel",
                "https://cnn.com",
                "general",
                "en",
                "us"
        );

        String text = null;
        try {
            text = source.toString();
        } catch (Exception e) {
            fail("toString() should not throw exception: " + e.getMessage());
        }

        assertNotNull("toString() must not return null", text);
        assertFalse("toString() must not be empty", text.trim().isEmpty());
    }

    @Test
    public void testEqualityByFields() {
        Source s1 = new Source("a", "n", "d", "u", "c", "l", "co");
        Source s2 = new Source("a", "n", "d", "u", "c", "l", "co");

        assertEquals(s1.id, s2.id);
        assertEquals(s1.name, s2.name);
        assertEquals(s1.category, s2.category);
        assertEquals(s1.country, s2.country);
    }

    @Test
    public void testNonEmptyFields() {
        Source s = new Source("id", "name", "desc", "url", "cat", "lang", "cty");
        assertNotNull(s.id);
        assertNotNull(s.name);
        assertNotNull(s.url);
    }
}
