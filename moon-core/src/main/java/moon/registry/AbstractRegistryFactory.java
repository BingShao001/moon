package moon.registry;

import moon.common.URL;
import moon.exception.RpcFrameworkException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public abstract class AbstractRegistryFactory implements RegistryFactory {

    private final ConcurrentHashMap<String, Registry> registries = new ConcurrentHashMap<String, Registry>();
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public Registry getRegistry(URL url) {
        //consumer://10.141.5.49/moon.example.UserService?
        // application=moon-consumer&category=consumers&check=false&moon=2.8.3&interface=moon.example.UserService&pid=29465&side=consumer
        // &timeout=120000&timestamp=1480648755499&version=1.0.0
        String registryUri = getRegistryUri(url);
        try {
            lock.lock();
            Registry registry = registries.get(registryUri);
            if (registry != null) {
                return registry;
            }
            //触发zk注册  ZookeeperRegistry
            registry = createRegistry(url);
            if (registry == null) {
                throw new RpcFrameworkException("Create registry false for url:" + url);
            }
            registries.put(registryUri, registry);
            return registry;
        } catch (Exception e) {
            throw new RpcFrameworkException("Create registry false for url:" + url, e);
        } finally {
            lock.unlock();
        }
    }

    protected String getRegistryUri(URL url) {
        String registryUri = url.getUri();
        return registryUri;
    }

    protected abstract Registry createRegistry(URL url);
}
