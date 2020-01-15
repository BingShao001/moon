package moon.rpc;

import com.google.common.collect.ArrayListMultimap;
import moon.cluster.Cluster;
import moon.cluster.DefaultCluster;
import moon.cluster.HaStrategy;
import moon.cluster.LoadBalance;
import moon.common.URL;
import moon.common.URLParam;
import moon.core.extension.ExtensionLoader;
import moon.exception.RpcFrameworkException;
import moon.protocol.ProtocolFilterWrapper;
import moon.proxy.ProxyFactory;
import moon.proxy.ReferenceInvocationHandler;
import moon.registry.Registry;
import moon.registry.RegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class DefaultConfigHandler implements ConfigHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public <T> Cluster<T> buildCluster(Class<T> interfaceClass, URL refUrl, List<URL> registryUrls) {
        DefaultCluster<T> cluster = new DefaultCluster(interfaceClass, refUrl, registryUrls);
        String loadBalanceName = refUrl.getParameter(URLParam.loadBalance.getName(), URLParam.loadBalance.getValue());
        String haStrategyName = refUrl.getParameter(URLParam.haStrategy.getName(), URLParam.haStrategy.getValue());
        LoadBalance<T> loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(loadBalanceName);
        HaStrategy<T> ha = ExtensionLoader.getExtensionLoader(HaStrategy.class).getExtension(haStrategyName);
        cluster.setLoadBalance(loadBalance);
        cluster.setHaStrategy(ha);

        cluster.init();
        return cluster;
    }

    @Override
    public <T> T refer(Class<T> interfaceClass, List<Cluster<T>> cluster, String proxyType) {
        //JdkProxyFactory
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension(proxyType);
        return (T) proxyFactory.getProxy(interfaceClass, new ReferenceInvocationHandler<>(interfaceClass, cluster));
    }

    /**
     * 组织xml节点数据
     * @param interfaceClass
     * @param ref
     * @param serviceUrl
     * @param registryUrls
     * @param <T>
     * @return
     */
    @Override
    public <T> Exporter<T> export(Class<T> interfaceClass, T ref, URL serviceUrl, List<URL> registryUrls) {
        //<moon:protocol name="moon"
        String protocolName = serviceUrl.getParameter(URLParam.protocol.getName(), URLParam.protocol.getValue());
        Provider<T> provider = new DefaultProvider<T>(ref, serviceUrl, interfaceClass);
        //DefaultRpcProtocol
        Protocol protocol = new ProtocolFilterWrapper(ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolName));
        // 通过protocol暴露服务
        Exporter<T> exporter = protocol.export(provider, serviceUrl);

        // register service
        register(registryUrls, serviceUrl);

        return exporter;
    }

    @Override
    public <T> void unexport(List<Exporter<T>> exporters, ArrayListMultimap<URL, URL> registryUrls) {
        try {
            unRegister(registryUrls);
        } catch (Exception e){
            logger.warn("Exception when unexport registryUrls:{}", registryUrls);
        }

        for (Exporter<T> exporter : exporters) {
            try {
                exporter.unexport();
            } catch (Exception e) {
                logger.warn("Exception when unexport exporters:{}", exporters);
            }
        }
    }

    private void unRegister(ArrayListMultimap<URL, URL> registryUrls) {

        for (URL serviceUrl : registryUrls.keySet()) {
            for (URL url : registryUrls.get(serviceUrl)) {
                try {
                    RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(url.getProtocol());
                    Registry registry = registryFactory.getRegistry(url);
                    registry.unregister(serviceUrl);
                } catch (Exception e) {
                    logger.warn(String.format("unregister url false:%s", url), e);
                }
            }
        }
    }

    private void register(List<URL> registryUrls, URL serviceUrl) {

        for (URL registryUrl : registryUrls) {
            // 根据check参数的设置，register失败可能会抛异常，上层应该知晓
            //spi ZookeeperRegistryFactory
            RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(registryUrl.getProtocol());
            if (registryFactory == null) {
                throw new RpcFrameworkException("register error! Could not find extension for registry protocol:" + registryUrl.getProtocol()
                                + ", make sure registry module for " + registryUrl.getProtocol() + " is in classpath!");
            }
            try {
                //zk注册 创建注册表节点
                Registry registry = registryFactory.getRegistry(registryUrl);
                registry.register(serviceUrl);
            } catch (Exception e) {
                throw new RpcFrameworkException("register error! Could not registry service:" + serviceUrl.getPath()
                        + " for " + registryUrl.getProtocol(), e);
            }
        }
    }
}
