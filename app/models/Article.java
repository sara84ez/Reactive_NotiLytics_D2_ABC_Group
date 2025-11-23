package app.models;

/**
 * Article model representing a single NewsAPI article.
 *
 * Author: Sara Ezzati
 */
public class Article {

    public final String id;
    public final String title;
    public final String description;
    public final String url;
    public final String sourceName;

    public Article(String id, String title, String description, String url, String sourceName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url = url;
        this.sourceName = sourceName;
    }

    /** Converts the article to JSON for WebSocket push. */
    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"title\":\"%s\",\"description\":\"%s\",\"url\":\"%s\",\"source\":\"%s\"}",
            id, escape(title), escape(description), url, sourceName
        );
    }

    private String escape(String s){
        return s.replace("\"","'");
    }
}
