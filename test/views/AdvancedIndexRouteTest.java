
package test;

import org.junit.jupiter.api.Test;
import play.mvc.*;
import play.test.WithApplication;
import static org.junit.jupiter.api.Assertions.*;

public class AdvancedIndexRouteTest extends WithApplication {

    @Test
    public void testIndexRoute() {
        Http.RequestBuilder req = new Http.RequestBuilder().method("GET").uri("/");
        Result result = route(app, req);
        assertEquals(200, result.status());
        assertTrue(contentAsString(result).contains("Live Stream"));
    }
}
