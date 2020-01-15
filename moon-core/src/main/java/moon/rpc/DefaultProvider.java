package moon.rpc;

import moon.common.URL;
import moon.core.DefaultResponse;
import moon.core.Request;
import moon.core.Response;
import moon.exception.RpcBizException;
import moon.exception.RpcFrameworkException;

import java.lang.reflect.Method;

/**
 * @author Ricky Fung
 */
public class DefaultProvider<T> extends AbstractProvider<T> {
    protected T proxyImpl;

    public DefaultProvider(T proxyImpl, URL url, Class<T> clz) {
        super(url, clz);
        this.proxyImpl = proxyImpl;
    }

    @Override
    public Class<T> getInterface() {
        return clz;
    }

    /**
     *
     * @param request
     * @return
     */
    @Override
    public Response invoke(Request request) {

        DefaultResponse response = new DefaultResponse();
        response.setRequestId(request.getRequestId());
        //根据方法名和参数匹配Method
        Method method = lookup(request);
        if (method == null) {
            RpcFrameworkException exception =
                    new RpcFrameworkException("Service method not exist: " + request.getInterfaceName() + "." + request.getMethodName());

            response.setException(exception);
            return response;
        }
        try {
            method.setAccessible(true);
            //通过反射调用对应方法，实现动态代理（ref的对象）
            Object result = method.invoke(proxyImpl, request.getArguments());
            response.setResult(result);
        } catch (Exception e) {
            response.setException(new RpcBizException("invoke failure", e));
        }
        return response;
    }
}
