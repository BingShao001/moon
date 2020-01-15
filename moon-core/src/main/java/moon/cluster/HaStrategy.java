package moon.cluster;

import moon.core.Request;
import moon.core.Response;
import moon.core.extension.SPI;
import moon.core.extension.Scope;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
@SPI(scope = Scope.PROTOTYPE)
public interface HaStrategy<T> {

    Response call(Request request, LoadBalance loadBalance);
}
