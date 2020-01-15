package moon.registry.zookeeper;

import moon.common.URL;
import moon.util.Constants;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class ZkUtils {
    // /moon/default_rpc
    public static String toGroupPath(URL url) {
        return Constants.ZOOKEEPER_REGISTRY_NAMESPACE + Constants.PATH_SEPARATOR + url.getGroup();
    }

    public static String toServicePath(URL url) {
        return toGroupPath(url) + Constants.PATH_SEPARATOR + url.getPath();
    }
    // /moon/default_rpc/providers
    public static String toNodeTypePath(URL url, ZkNodeType nodeType) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + nodeType.getValue();
    }

    public static String toNodePath(URL url, ZkNodeType nodeType) {
        return toNodeTypePath(url, nodeType) + Constants.PATH_SEPARATOR + url.getServerAndPort();
    }
}
