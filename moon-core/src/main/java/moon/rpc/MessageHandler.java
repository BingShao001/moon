package moon.rpc;

import moon.core.Request;
import moon.core.Response;

/**
 * @author Ricky Fung
 */
public interface MessageHandler {

    Response handle(Request request);

}
