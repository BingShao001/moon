package moon.registry;

import moon.common.URL;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public interface Registry extends RegistryService, DiscoveryService {

    URL getUrl();

    void close();
}
