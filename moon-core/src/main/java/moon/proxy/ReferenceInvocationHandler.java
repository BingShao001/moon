package moon.proxy;

import moon.cluster.Cluster;
import moon.common.URLParam;
import moon.core.DefaultRequest;
import moon.core.Response;
import moon.exception.RpcFrameworkException;
import moon.exception.RpcServiceException;
import moon.util.Constants;
import moon.util.ExceptionUtil;
import moon.util.RequestIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class ReferenceInvocationHandler<T> implements InvocationHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<Cluster<T>> clusters;
    private Class<T> clz;

    public ReferenceInvocationHandler(Class<T> clz, List<Cluster<T>> clusters) {
        this.clz = clz;
        this.clusters = clusters;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //toString,equals,hashCode,finalize等接口未声明的方法不进行远程调用
        if(method.getDeclaringClass().equals(Object.class)){
            if ("toString".equals(method.getName())) {
                return "";
            }
            throw new RpcFrameworkException("can not invoke local method:" + method.getName());
        }
        //组装request通讯对象
        DefaultRequest request = new DefaultRequest();
        //递增生成一个RequestId，用于映射rpcFuture保存在map中
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        //方法入参类型
        request.setParameterTypes(method.getParameterTypes());
        request.setArguments(args);
        request.setType(Constants.REQUEST_SYNC);


        boolean throwException = checkMethodExceptionSignature(method);
        //cluster 封装reference《——》service通讯对象
        for (Cluster<T> cluster : clusters) {
            //调用参数
            request.setAttachment(URLParam.version.getName(), cluster.getUrl().getVersion());
            request.setAttachment(URLParam.group.getName(), cluster.getUrl().getGroup());
            try {
                //远程调用服务
                Response resp = cluster.call(request);
                return getValue(resp);
            } catch (RuntimeException e) {
                //分别处理check和runtime异常
                if (ExceptionUtil.isBizException(e)) {
                    Throwable t = e.getCause();
                    if (t != null && t instanceof Exception) {
                        throw t;
                    } else {
                        String msg =
                                t == null ? "biz exception cause is null" : ("biz exception cause is throwable error:" + t.getClass()
                                        + ", errmsg:" + t.getMessage());
                        throw new RpcServiceException(msg);
                    }
                } else if (!throwException) {
                    logger.warn(this.getClass().getSimpleName()+" invoke false, so return default value: uri=" + cluster.getUrl().getUri(), e);
                    return getDefaultReturnValue(method.getReturnType());
                } else {
                    logger.error(this.getClass().getSimpleName()+" invoke Error: uri=" + cluster.getUrl().getUri(), e);
                    throw e;
                }
            }
        }
        throw new RpcServiceException("Reference call Error: cluster not exist, interface=" + clz.getName());
    }

    private boolean checkMethodExceptionSignature(Method method) {
        Class<?>[] exps = method.getExceptionTypes();
        return exps!=null && exps.length>0;
    }

    public Object getValue(Response resp) {
        Exception exception = resp.getException();
        if (exception != null) {
            throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new RpcFrameworkException(
                    exception.getMessage(), exception);
        }
        return resp.getResult();
    }


    private Object getDefaultReturnValue(Class<?> returnType) {
        if (returnType != null && returnType.isPrimitive()) {
            return PrimitiveDefault.getDefaultReturnValue(returnType);
        }
        return null;
    }

    private static class PrimitiveDefault {
        private static boolean defaultBoolean;
        private static char defaultChar;
        private static byte defaultByte;
        private static short defaultShort;
        private static int defaultInt;
        private static long defaultLong;
        private static float defaultFloat;
        private static double defaultDouble;

        private static Map<Class<?>, Object> primitiveValues = new HashMap<Class<?>, Object>();

        static {
            primitiveValues.put(boolean.class, defaultBoolean);
            primitiveValues.put(char.class, defaultChar);
            primitiveValues.put(byte.class, defaultByte);
            primitiveValues.put(short.class, defaultShort);
            primitiveValues.put(int.class, defaultInt);
            primitiveValues.put(long.class, defaultLong);
            primitiveValues.put(float.class, defaultFloat);
            primitiveValues.put(double.class, defaultDouble);
        }

        public static Object getDefaultReturnValue(Class<?> returnType) {
            return primitiveValues.get(returnType);
        }

    }
}
