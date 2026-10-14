import org.junit.Test;
import play.test.Helpers;

import static org.junit.Assert.assertNotEquals;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;

public class SmokeTest {
    @Test
    public void helpPageDoesNotCrash() {
        play.Application app = Helpers.fakeApplication();
        Helpers.running(app, () -> {
            play.mvc.Result result = Helpers.route(
                app,
                Helpers.fakeRequest("GET", "/help")
            );
            assertNotEquals(INTERNAL_SERVER_ERROR, result.status());
        });
    }
}
