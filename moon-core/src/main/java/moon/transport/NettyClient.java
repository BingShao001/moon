package moon.transport;

import moon.core.Request;
import moon.core.Response;
import moon.core.ResponseFuture;
import moon.exception.TransportException;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public interface NettyClient extends Endpoint {

    Response invokeSync(final Request request)
            throws InterruptedException, TransportException;

    ResponseFuture invokeAsync(final Request request)
            throws InterruptedException, TransportException;

    void invokeOneway(final Request request)
            throws InterruptedException, TransportException;

}
