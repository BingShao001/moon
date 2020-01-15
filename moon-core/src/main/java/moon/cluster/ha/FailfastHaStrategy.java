package moon.cluster.ha;

import moon.cluster.HaStrategy;
import moon.cluster.LoadBalance;
import moon.core.Request;
import moon.core.Response;
import moon.rpc.Reference;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class FailfastHaStrategy<T> implements HaStrategy<T> {

    @Override
    public Response call(Request request, LoadBalance loadBalance) {
        Reference<T> reference = loadBalance.select(request);
        return reference.call(request);
    }
}
