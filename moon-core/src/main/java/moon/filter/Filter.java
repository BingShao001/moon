package moon.filter;

import moon.core.Request;
import moon.core.Response;
import moon.core.extension.SPI;
import moon.rpc.Caller;

/**
 * @author Ricky Fung
 */
@SPI
public interface Filter {

    Response filter(Caller<?> caller, Request request);

}
