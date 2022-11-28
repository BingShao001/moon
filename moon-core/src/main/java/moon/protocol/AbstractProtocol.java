package moon.protocol;

import moon.common.URL;
import moon.exception.RpcFrameworkException;
import moon.rpc.*;
import moon.util.FrameworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public abstract class AbstractProtocol implements Protocol {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected ConcurrentHashMap<String, Exporter<?>> exporterMap = new ConcurrentHashMap<>();

    /**
     * @param clz        接口名
     * @param url        reference的信息
     * @param serviceUrl 注册中心上接口的信息
     * @param <T>
     * @return
     */
    @Override
    public <T> Reference<T> refer(Class<T> clz, URL url, URL serviceUrl) {
        if (url == null) {
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " refer Error: url is null");
        }
        if (clz == null) {
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " refer Error: class is null, url=" + url);
        }
        //根据目标service的资源和协议资源等创建Reference通讯对象(netty)
        Reference<T> reference = createReference(clz, url, serviceUrl);
        //打开netty连接service资源通道
        reference.init();

        logger.info(this.getClass().getSimpleName() + " refer service:{} success url:{}", clz.getName(), url);
        return reference;
    }

    /**
     * @param provider   对外发布者的信息
     * @param serviceUrl serviceUrl的信息(ip端口、依赖接口、版本、group，序列化方式，角色，超时时间等)
     * @param <T>
     * @return
     */
    @Override
    public <T> Exporter<T> export(Provider<T> provider, URL serviceUrl) {
        if (serviceUrl == null) {
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " export Error: url is null");
        }

        if (provider == null) {
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " export Error: provider is null, url=" + serviceUrl);
        }

        String protocolKey = FrameworkUtils.getProtocolKey(serviceUrl);
        //处理重复注册，锁住读与写之前的代码块并发资源
        synchronized (exporterMap) {
            Exporter<T> exporter = (Exporter<T>) exporterMap.get(protocolKey);

            if (exporter != null) {
                throw new RpcFrameworkException(this.getClass().getSimpleName() + " export Error: service already exist, url=" + serviceUrl);
            }
            // 根据provider的ip端口等；还有协议的序列化方式；业务线程池来创建netty的server资源
            exporter = createExporter(provider, serviceUrl);
            // 初始化Exporter，netty传输层启动服务
            exporter.init();
            exporterMap.put(protocolKey, exporter);
            logger.info(this.getClass().getSimpleName() + " export success: url=" + serviceUrl);
            return exporter;
        }
    }

    protected abstract <T> Reference<T> createReference(Class<T> clz, URL url, URL serviceUrl);

    protected abstract <T> Exporter<T> createExporter(Provider<T> provider, URL url);
}
