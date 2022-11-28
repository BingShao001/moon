package moon.config;

import moon.cluster.Cluster;
import moon.common.URL;
import moon.common.URLParam;
import moon.core.extension.ExtensionLoader;
import moon.rpc.ConfigHandler;
import moon.util.Constants;
import moon.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class ReferenceConfig<T> extends AbstractInterfaceConfig {

    private static final long serialVersionUID = 3259358868568571457L;
    private Class<T> interfaceClass;
    protected transient volatile T proxy;

    private transient volatile boolean initialized;
    private List<Cluster<T>> clusters;

    //获取代理，@Resource和@Autowired不会有性能问题；
    public T get() {
        if (proxy == null) {
            init();
        }
        return proxy;
    }

    private synchronized void init() {
        if (initialized) {
            return;
        }

        if (interfaceName == null || interfaceName.length() == 0) {
            throw new IllegalStateException("<moon:reference interface=\"\" /> interface not allow null!");
        }
        try {
            //反射构造接口
            interfaceClass = (Class<T>) Class.forName(interfaceName, true, Thread.currentThread()
                    .getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("reference class not found", e);
        }
        //触发动态代理
        initProxy();

        initialized = true;
    }

    private void initProxy() {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("<moon:reference interface=\"\" /> is not interface!");
        }
        //加载注册中心资源列表
        List<URL> registryUrls = loadRegistryUrls();
        if (registryUrls == null || registryUrls.size() == 0) {
            throw new IllegalStateException("Should set registry config for reference:" + interfaceClass.getName());
        }
        //获取默认路由配置
        ConfigHandler configHandler = ExtensionLoader.getExtensionLoader(ConfigHandler.class).getExtension(Constants.DEFAULT_VALUE);

        clusters = new ArrayList<>(protocols.size());
        String proxyType = null;
        //有种协议就拼多少个cluster对象，默认1种
        for (ProtocolConfig protocol : protocols) {

            Map<String, String> map = new HashMap<>();
            map.put(URLParam.application.getName(), StringUtils.isNotEmpty(application.getName()) ? application.getName() : URLParam.application.getValue());
            map.put(URLParam.serialization.getName(), StringUtils.isNotEmpty(protocol.getSerialization()) ? protocol.getSerialization() : URLParam.serialization.getValue());
            map.put(URLParam.version.getName(), StringUtils.isNotEmpty(version) ? version : URLParam.version.getValue());
            map.put(URLParam.group.getName(), StringUtils.isNotEmpty(group) ? group : URLParam.group.getValue());
            map.put(URLParam.side.getName(), Constants.CONSUMER);
            map.put(URLParam.requestTimeout.getName(), String.valueOf(getTimeout()));
            map.put(URLParam.timestamp.getName(), String.valueOf(System.currentTimeMillis()));
            map.put(URLParam.check.getName(), isCheck().toString());
            //获取reference通讯协议的地址和端口
            String hostAddress = getLocalHostAddress(protocol);
            Integer port = getProtocolPort(protocol);

            URL refUrl = new URL(protocol.getName(), hostAddress, port, interfaceClass.getName(), map);
            // 构建默认路由信息，封装reference《——》service通讯对象
            clusters.add(configHandler.buildCluster(interfaceClass, refUrl, registryUrls));
            //配置动态代理的方式
            proxyType = refUrl.getParameter(URLParam.proxyType.getName(), URLParam.proxyType.getValue());
        }
        //根据依赖的interface创建代理对象（moon.proxy.ReferenceInvocationHandler.invoke）
        this.proxy = configHandler.refer(interfaceClass, clusters, proxyType);
    }

    public T getProxy() {
        return proxy;
    }

    public void setProxy(T proxy) {
        this.proxy = proxy;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    protected void destroy0() throws Exception {
        if (clusters != null) {
            for (Cluster<T> cluster : clusters) {
                cluster.destroy();
            }
        }
        proxy = null;
    }
}
