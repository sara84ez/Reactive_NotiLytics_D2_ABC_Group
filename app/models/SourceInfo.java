package app.models;

/**
 * SourceInfo model representing a NewsAPI source.
 *
 * Author: Sara Ezzati
 */
public class SourceInfo {

    public final String id;
    public final String name;
    public final String country;
    public final String category;
    public final String language;
    public final String url;

    public SourceInfo(String id, String name, String country, String category, String language, String url) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.category = category;
        this.language = language;
        this.url = url;
    }

    /** Converts the source info to JSON for WebSocket push. */
    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"country\":\"%s\",\"category\":\"%s\",\"language\":\"%s\",\"url\":\"%s\"}",
            id, name, country, category, language, url
        );
    }
}
