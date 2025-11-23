
package test;

import org.junit.jupiter.api.Test;
import play.mvc.*;
import play.test.WithApplication;
import static org.junit.jupiter.api.Assertions.*;

public class AdvancedSourcesRouteTest extends WithApplication {

    @Test
    public void testSourcesRoute() {
        Http.RequestBuilder req = new Http.RequestBuilder().method("GET").uri("/sources");
        Result result = route(app, req);
        assertEquals(200, result.status());
        assertTrue(contentAsString(result).contains("Sources"));
    }
}
