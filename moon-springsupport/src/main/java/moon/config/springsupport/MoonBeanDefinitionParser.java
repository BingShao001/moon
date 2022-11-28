package moon.config.springsupport;

import moon.config.ApplicationConfig;
import moon.config.ProtocolConfig;
import moon.config.RegistryConfig;
import moon.util.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class MoonBeanDefinitionParser implements BeanDefinitionParser {

    private final Class<?> beanClass;

    private final boolean required;

    public MoonBeanDefinitionParser(Class<?> beanClass, boolean required) {
        this.beanClass = beanClass;
        this.required = required;
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        try {
            return parse(element, parserContext, beanClass, required);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings( {"rawtypes", "unchecked"})
    private static BeanDefinition parse(Element element, ParserContext parserContext, Class<?> beanClass, boolean required)
            throws ClassNotFoundException {
        RootBeanDefinition bd = new RootBeanDefinition();
        bd.setBeanClass(beanClass);
        // 不允许lazy init
        bd.setLazyInit(false);

        // 如果没有id则按照规则生成一个id,注册id到context中
        /**注册类型到bean池里，并设置bean id start**/
        /**
         * 1, 获取xml节点设置的id值
         * 2，如果没有设置，获取xml节点设置的name值
         * 3，如果没有设置，获取xml节点设置的interface值
         * 4，如果没有设置，获取对应nameSpaceHandler中映射的Class类型的name
         * 5, 如果bean中已经本注册，那么就从2开始累加和第4部的name拼接起来。保证唯一。
         */
        String id = element.getAttribute("id");
        if ((id == null || id.length() == 0) && required) {
            String generatedBeanName = element.getAttribute("name");
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                generatedBeanName = element.getAttribute("interface");
            }
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                generatedBeanName = beanClass.getName();
            }
            id = generatedBeanName;
            int counter = 2;
            //去重
            while (parserContext.getRegistry().containsBeanDefinition(id)) {
                id = generatedBeanName + (counter++);
            }
        }
        //如果累加后的beanId，在ParserContext中还有重复异常拦截
        if (id != null && id.length() > 0) {
            if (parserContext.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
            parserContext.getRegistry().registerBeanDefinition(id, bd);
        }
        bd.getPropertyValues().addPropertyValue("id", id);
        /**注册类型到bean池里，并设置bean id end**/

        //按类型，解析属性
        if (ApplicationConfig.class.equals(beanClass)) {
            /**
             * <moon:application name="${application.name}" />
             */
            MoonNamespaceHandler.applicationConfigDefineNames.add(id);
            //将element节点对应的类的属性进行赋值
            parseCommonProperty("name", null, element, bd, parserContext);
            parseCommonProperty("manager", null, element, bd, parserContext);
            parseCommonProperty("organization", null, element, bd, parserContext);
            parseCommonProperty("version", null, element, bd, parserContext);
            parseCommonProperty("env", null, element, bd, parserContext);
            parseCommonProperty("default", "isDefault", element, bd, parserContext);

        } else if (ProtocolConfig.class.equals(beanClass)) {
            /**
             * <moon:protocol name="moon" port="${protocol.port}" />
             */
            MoonNamespaceHandler.protocolDefineNames.add(id);

            parseCommonProperty("name", null, element, bd, parserContext);
            parseCommonProperty("host", null, element, bd, parserContext);
            parseCommonProperty("port", null, element, bd, parserContext);
            parseCommonProperty("codec", null, element, bd, parserContext);
            parseCommonProperty("serialization", null, element, bd, parserContext);
            //在xml中配置前面pool-type，在映射实体中使用别名poolType对应属性
            parseCommonProperty("pool-type", "poolType", element, bd, parserContext);
            parseCommonProperty("min-pool-size", "minPoolSize", element, bd, parserContext);
            parseCommonProperty("max-pool-size", "maxPoolSize", element, bd, parserContext);
            parseCommonProperty("charset", null, element, bd, parserContext);
            parseCommonProperty("buffer-size", "bufferSize", element, bd, parserContext);
            parseCommonProperty("payload", null, element, bd, parserContext);
            parseCommonProperty("heartbeat", null, element, bd, parserContext);
            parseCommonProperty("default", "isDefault", element, bd, parserContext);
        } else if (RegistryConfig.class.equals(beanClass)) {
            /**
             * <moon:registry id="zookeeper" protocol="zookeeper" address="${registry.address}" connect-timeout="5000" />
             */
            MoonNamespaceHandler.registryDefineNames.add(id);

            parseCommonProperty("protocol", null, element, bd, parserContext);
            parseCommonProperty("address", null, element, bd, parserContext);
            parseCommonProperty("connect-timeout", "connectTimeout", element, bd, parserContext);
            parseCommonProperty("session-timeout", "sessionTimeout", element, bd, parserContext);
            parseCommonProperty("username", null, element, bd, parserContext);
            parseCommonProperty("password", null, element, bd, parserContext);
            parseCommonProperty("default", "isDefault", element, bd, parserContext);
        } else if (ReferenceConfigBean.class.equals(beanClass)) {
            /**
             * <moon:reference id="demoService"  interface="moon.demo.service.DemoService"  group="group1" registry="zookeeper" />
             */
            MoonNamespaceHandler.referenceConfigDefineNames.add(id);

            parseCommonProperty("interface", "interfaceName", element, bd, parserContext);
            /**
             * 注册中心的配置列表，解析
             * registry
             */
            String registry = element.getAttribute("registry");
            if (StringUtils.isNotBlank(registry)) {
                parseMultiRef("registries", registry, bd, parserContext);
            }

            parseCommonProperty("group", null, element, bd, parserContext);
            parseCommonProperty("version", null, element, bd, parserContext);

            parseCommonProperty("timeout", null, element, bd, parserContext);
            parseCommonProperty("retries", null, element, bd, parserContext);
            parseCommonProperty("check", null, element, bd, parserContext);

        } else if (ServiceConfigBean.class.equals(beanClass)) {
            /**
             * <moon:service interface="moon.demo.service.DemoService" ref="demoService" protocol="moon" group="group1" version="1.0.0"/>
             */
            MoonNamespaceHandler.serviceConfigDefineNames.add(id);

            parseCommonProperty("interface", "interfaceName", element, bd, parserContext);

            parseSingleRef("ref", element, bd, parserContext);

            String registry = element.getAttribute("registry");
            if (StringUtils.isNotBlank(registry)) {
                parseMultiRef("registries", registry, bd, parserContext);
            }

            String protocol = element.getAttribute("protocol");
            if (StringUtils.isNotBlank(protocol)) {
                parseMultiRef("protocols", protocol, bd, parserContext);
            }

            parseCommonProperty("timeout", null, element, bd, parserContext);
            parseCommonProperty("retries", null, element, bd, parserContext);

            parseCommonProperty("group", null, element, bd, parserContext);
            parseCommonProperty("version", null, element, bd, parserContext);
        }
        return bd;
    }

    /**
     * 将element节点对应的类的属性进行赋值
     *
     * @param name
     * @param alias
     * @param element
     * @param bd
     * @param parserContext
     */
    private static void parseCommonProperty(String name, String alias, Element element, BeanDefinition bd,
                                            ParserContext parserContext) {

        String value = element.getAttribute(name);
        if (StringUtils.isNotBlank(value)) {
            String property = alias != null ? alias : name;
            bd.getPropertyValues().addPropertyValue(property, value);
        }
    }

    /**
     * 单个依赖，single拦截验证
     *
     * @param property
     * @param element
     * @param bd
     * @param parserContext
     */
    private static void parseSingleRef(String property, Element element, BeanDefinition bd,
                                       ParserContext parserContext) {

        String value = element.getAttribute(property);
        if (StringUtils.isNotBlank(value)) {
            if (parserContext.getRegistry().containsBeanDefinition(value)) {
                BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                if (!refBean.isSingleton()) {
                    throw new IllegalStateException("The exported service ref " + value + " must be singleton! Please set the " + value
                            + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                }
            }
            bd.getPropertyValues().addPropertyValue(property, new RuntimeBeanReference(value));
        }
    }

    /**
     * 批量依赖属性
     *
     * @param property
     * @param value
     * @param bd
     * @param parserContext
     */
    private static void parseMultiRef(String property, String value, BeanDefinition bd, ParserContext parserContext) {
        /**
         * <moon:reference id="demoService"  interface="moon.demo.service.DemoService"  group="group1" registry="zookeeper" />
         */
        String[] values = value.split("\\s*[,]+\\s*");
        ManagedList list = null;
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (v != null && v.length() > 0) {
                if (list == null) {
                    list = new ManagedList();
                }
                list.add(new RuntimeBeanReference(v));
            }
        }
        bd.getPropertyValues().addPropertyValue(property, list);
        /**
         * 在Spring的解析段，其它容器中是没有依赖的Bean的实例的，因此这个被依赖的Bean需要表示成RuntimeBeanReference对象，
         * 并将它放到BeanDefinition的MutablePropertyValues中。
         * 在创建Bean时，需要将依赖解析成真正的Spring容器中存在的Bean。
         */
    }
}