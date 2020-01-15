package moon.cluster;

import moon.core.extension.SPI;
import moon.core.extension.Scope;
import moon.rpc.Caller;
import moon.rpc.Reference;

import java.util.List;

/**
 * @author Ricky Fung
 */
@SPI(scope = Scope.PROTOTYPE)
public interface Cluster<T> extends Caller<T> {

    void setLoadBalance(LoadBalance<T> loadBalance);

    void setHaStrategy(HaStrategy<T> haStrategy);

    List<Reference<T>> getReferences();

    LoadBalance<T> getLoadBalance();
}
