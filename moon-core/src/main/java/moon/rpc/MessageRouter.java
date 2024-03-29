package moon.rpc;

import moon.core.DefaultResponse;
import moon.core.Request;
import moon.core.Response;
import moon.exception.RpcBizException;
import moon.exception.RpcFrameworkException;
import moon.util.FrameworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Ricky Fung
 */
public class MessageRouter implements MessageHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ConcurrentHashMap<String, Provider<?>> providers = new ConcurrentHashMap<>();

    public MessageRouter() {}

    public MessageRouter(Provider<?> provider) {
        addProvider(provider);
    }

    @Override
    public Response handle(Request request) {
        //serviceKey: group/interface/version
        String serviceKey = FrameworkUtils.getServiceKey(request);
        //通过之前注册过的map记录，拿到对应请求的provider对象，以空间换时间
        Provider<?> provider = providers.get(serviceKey);

        if (provider == null) {
            logger.error(this.getClass().getSimpleName() + " handler Error: provider not exist serviceKey=" + serviceKey);
            RpcFrameworkException exception =
                    new RpcFrameworkException(this.getClass().getSimpleName() + " handler Error: provider not exist serviceKey="
                            + serviceKey );

            DefaultResponse response = new DefaultResponse();
            response.setException(exception);
            return response;
        }

        return call(request, provider);
    }

    protected Response call(Request request, Provider<?> provider) {
        try {
            return provider.call(request);
        } catch (Exception e) {
            DefaultResponse response = new DefaultResponse();
            response.setException(new RpcBizException("provider call process error", e));
            return response;
        }
    }

    public synchronized void addProvider(Provider<?> provider) {
        String serviceKey = FrameworkUtils.getServiceKey(provider.getUrl());
        if (providers.containsKey(serviceKey)) {
            throw new RpcFrameworkException("provider alread exist: " + serviceKey);
        }
        providers.put(serviceKey, provider);
        logger.info("RequestRouter addProvider: url=" + provider.getUrl());
    }

    public synchronized void removeProvider(Provider<?> provider) {
        String serviceKey = FrameworkUtils.getServiceKey(provider.getUrl());
        providers.remove(serviceKey);
        logger.info("RequestRouter removeProvider: url=" + provider.getUrl());
    }
}
