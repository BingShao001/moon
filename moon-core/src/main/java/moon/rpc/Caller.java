package moon.rpc;

import moon.core.Request;
import moon.core.Response;

/**
 *
 * @author Ricky Fung
 */
public interface Caller<T> extends Node {

    Class<T> getInterface();

    Response call(Request request);

}
