
package test;

import org.junit.jupiter.api.Test;
import play.twirl.api.Html;
import static org.junit.jupiter.api.Assertions.*;

public class ViewRenderTest {

    @Test
    public void testIndexViewRenders() {
        Html html = views.html.index.render();
        assertNotNull(html);
        assertTrue(html.body().length() > 0);
    }
}
