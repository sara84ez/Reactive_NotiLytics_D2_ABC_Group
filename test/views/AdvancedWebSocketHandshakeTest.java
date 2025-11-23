
package test;

import org.junit.jupiter.api.Test;
import play.mvc.*;
import play.test.WithApplication;

import static org.junit.jupiter.api.Assertions.*;

public class AdvancedWebSocketHandshakeTest extends WithApplication {

    @Test
    public void testWebSocketUpgrade() {
        Http.RequestBuilder req = new Http.RequestBuilder()
                .method("GET")
                .uri("/ws")
                .header("Upgrade", "websocket");

        Result result = route(app, req);

        // Status 101 = Switching Protocols (valid WS handshake)
        assertTrue(result.status() == 101 || result.status() == 200);
    }
}
