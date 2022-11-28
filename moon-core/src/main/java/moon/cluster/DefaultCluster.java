package moon.cluster;

import moon.common.URL;
import moon.common.URLParam;
import moon.core.DefaultResponse;
import moon.core.Request;
import moon.core.Response;
import moon.core.extension.ExtensionLoader;
import moon.exception.RpcFrameworkException;
import moon.exception.RpcServiceException;
import moon.protocol.ProtocolFilterWrapper;
import moon.registry.NotifyListener;
import moon.registry.Registry;
import moon.registry.RegistryFactory;
import moon.rpc.Protocol;
import moon.rpc.Reference;
import moon.util.CollectionUtil;
import moon.util.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ricky Fung
 */
public class DefaultCluster<T> implements Cluster<T>, NotifyListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<URL> registryUrls;
    private URL url;
    private Class<T> interfaceClass;
    private Protocol protocol;

    private HaStrategy<T> haStrategy;

    private LoadBalance<T> loadBalance;

    private volatile List<Reference<T>> references;

    private ConcurrentHashMap<URL, List<Reference<T>>> registryReferences = new ConcurrentHashMap<>();

    private volatile boolean available;

    /**
     *
     * @param interfaceClass
     * @param url reference的信息(ip端口、依赖接口、版本、group，序列化方式，角色，超时时间等)
     * @param registryUrls 注册中心相关信息
     */
    public DefaultCluster(Class<T> interfaceClass, URL url, List<URL> registryUrls) {
        this.registryUrls = registryUrls;
        this.interfaceClass = interfaceClass;
        this.url = url;
        //DefaultRpcProtocol
        protocol = new ProtocolFilterWrapper(ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(url.getProtocol()));
    }

    @Override
    public void init() {

        URL subscribeUrl = url.clone0();
        for (URL ru : registryUrls) {
            //获取zk连接资源等
            Registry registry = getRegistry(ru);
            try {

                notify(ru, registry.discover(subscribeUrl));
            } catch (Exception e) {
                logger.error(String.format("Cluster init discover for the reference:%s, registry:%s", this.url, ru), e);
            }
            // client 注册自己，同时订阅service列表
            registry.subscribe(subscribeUrl, this);
        }

        logger.info("Cluster init over, url:{}, references size:{}", url, references!=null ? references.size():0);
        boolean check = Boolean.parseBoolean(url.getParameter(URLParam.check.getName(), URLParam.check.getValue()));
        //如果reference和注册中心的provider没匹配到，就为空，说明没有对应的provider
        if(CollectionUtil.isEmpty(references)) {
            logger.warn(String.format("Cluster No service urls for the reference:%s, registries:%s",
                    this.url, registryUrls));
            if(check) {
                throw new RpcFrameworkException(String.format("Cluster No service urls for the reference:%s, registries:%s",
                        this.url, registryUrls));
            }
        }
        available = true;
    }

    @Override
    public void destroy() {
        URL subscribeUrl = url.clone0();
        for (URL ru : registryUrls) {
            try {
                Registry registry = getRegistry(ru);
                registry.unsubscribe(subscribeUrl, this);
                registry.unregister(url);
            } catch (Exception e) {
                logger.warn(String.format("Unregister or unsubscribe false for url (%s), registry= %s", url, ru), e);
            }
        }
        if(references!=null) {
            for (Reference<T> reference : this.references) {
                reference.destroy();
            }
        }
        available = false;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String desc() {
        return null;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public Class<T> getInterface() {
        return interfaceClass;
    }

    @Override
    public Response call(Request request) {
        if(available) {
            try {
                //默认：FailfastHaStrategy  RandomLoadBalance
                return haStrategy.call(request, loadBalance);
            } catch (Exception e) {
                if (ExceptionUtil.isBizException(e)) {
                    throw (RuntimeException) e;
                }
                return buildErrorResponse(request, e);
            }
        }
        return buildErrorResponse(request, new RpcServiceException("service not available"));
    }

    private Response buildErrorResponse(Request request, Exception motanException) {
        DefaultResponse rs = new DefaultResponse();
        rs.setException(motanException);
        rs.setRequestId(request.getRequestId());
        return rs;
    }

    @Override
    public void setLoadBalance(LoadBalance<T> loadBalance) {
        this.loadBalance = loadBalance;
    }

    @Override
    public void setHaStrategy(HaStrategy<T> haStrategy) {
        this.haStrategy = haStrategy;
    }

    @Override
    public List<Reference<T>> getReferences() {
        return references;
    }

    @Override
    public LoadBalance<T> getLoadBalance() {
        return loadBalance;
    }

    /**
     * 1.根据reference信息匹配出对应provider上service的信息；
     * 2.创建netty通讯对象
     * @param registryUrl 注册中心的信息
     * @param urls provider接口信息：地址端口group等等
     */
    @Override
    public synchronized void notify(URL registryUrl, List<URL> urls) {
        if (CollectionUtil.isEmpty(urls)) {
            logger.warn("Cluster config change notify, urls is empty: registry={} service={} urls=[]", registryUrl.getUri(),
                    url, urls);
            return;
        }

        logger.info("Cluster config change notify: registry={} service={} urls={}", registryUrl.getUri(), url,
                urls);

        List<Reference<T>> newReferences = new ArrayList<>();
        for (URL u : urls) {
          //过滤reference和provider匹配信息
            if (!u.canServe(url)) {
                continue;
            }
            //获取在注册中心上能对应上的reference
            Reference<T> reference = getExistingReference(u, registryReferences.get(registryUrl));
            if (reference == null) {
                URL referenceURL = u.clone0();
                reference = protocol.refer(interfaceClass, referenceURL, u);
            }
            if (reference != null) {
                newReferences.add(reference);
            }
        }
        //按注册中心信息维度记录下References对象
        registryReferences.put(registryUrl, newReferences);
        //更新
        refresh();
    }

    private void refresh() {
        List<Reference<T>> references = new ArrayList<>();
        for (List<Reference<T>> refs : registryReferences.values()) {
            references.addAll(refs);
        }
        this.references = references;
        this.loadBalance.setReferences(references);
    }

    private Reference<T> getExistingReference(URL url, List<Reference<T>> referers) {
        if (referers == null) {
            return null;
        }
        for (Reference<T> r : referers) {
            if (url.equals(r.getUrl()) || url.equals(r.getServiceUrl())) {
                return r;
            }
        }
        return null;
    }

    private Registry getRegistry(URL registryUrl) {
        //spi zookeeper
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(registryUrl.getProtocol());
        if (registryFactory == null) {
            throw new RpcFrameworkException("register error! Could not find extension for registry protocol:" + registryUrl.getProtocol()
                    + ", make sure registry module for " + registryUrl.getProtocol() + " is in classpath!");
        }
        return registryFactory.getRegistry(registryUrl);
    }
}
