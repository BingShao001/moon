package moon.config.springsupport;

import moon.config.ApplicationConfig;
import moon.config.ProtocolConfig;
import moon.config.RegistryConfig;
import moon.util.ConcurrentHashSet;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

import java.util.Set;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class MoonNamespaceHandler extends NamespaceHandlerSupport {
    public final static Set<String> protocolDefineNames = new ConcurrentHashSet<String>();
    public final static Set<String> registryDefineNames = new ConcurrentHashSet<String>();
    public final static Set<String> serviceConfigDefineNames = new ConcurrentHashSet<String>();
    public final static Set<String> referenceConfigDefineNames = new ConcurrentHashSet<String>();
    public final static Set<String> applicationConfigDefineNames = new ConcurrentHashSet<String>();

    @Override
    public void init() {
        //element节点和class类型映射，解析注入属性
        registerBeanDefinitionParser("reference", new MoonBeanDefinitionParser(ReferenceConfigBean.class, false));
        registerBeanDefinitionParser("service", new MoonBeanDefinitionParser(ServiceConfigBean.class, true));
        registerBeanDefinitionParser("registry", new MoonBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("protocol", new MoonBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser("application", new MoonBeanDefinitionParser(ApplicationConfig.class, true));
    }
}
