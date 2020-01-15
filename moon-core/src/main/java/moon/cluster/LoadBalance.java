package moon.cluster;

import moon.core.Request;
import moon.core.extension.SPI;
import moon.core.extension.Scope;
import moon.rpc.Reference;
import java.util.List;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
@SPI(scope = Scope.PROTOTYPE)
public interface LoadBalance<T> {

    void setReferences(List<Reference<T>> references);

    Reference<T> select(Request request);
}
