package moon.config.springsupport;

import moon.common.URLParam;
import moon.config.ProtocolConfig;
import moon.config.ReferenceConfig;
import moon.config.RegistryConfig;
import moon.util.CollectionUtil;
import moon.util.FrameworkUtils;
import moon.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class ReferenceConfigBean<T> extends ReferenceConfig<T> implements
        FactoryBean<T>, BeanFactoryAware,
        InitializingBean, DisposableBean {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private transient BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * BeanDefinition初始化的以"userService"为id，类型为ReferenceConfigBean
     * ReferenceBean实现了FactoryBean接口，
     * 所以当我们context.getBean("userService")时，
     * 得到并不是ReferenceBean对象，而是getObject()返回的对象
     * @return
     * @throws Exception
     */
    @Override
    public T getObject() throws Exception {
        return get();
    }

    @Override
    public Class<?> getObjectType() {
        return getInterfaceClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        logger.debug("check reference interface:%s config", getInterfaceName());
        //检查application依赖的配置，并赋默认值name
        checkApplication();
        //协议类型及端口号
        checkProtocolConfig();
        //注册中心
        checkRegistryConfig();
        //分组赋值
        if(StringUtils.isEmpty(getGroup())) {
            setGroup(URLParam.group.getValue());
        }
        //版本赋值
        if(StringUtils.isEmpty(getVersion())) {
            setVersion(URLParam.version.getValue());
        }
        //超时时间
        if(getTimeout()==null) {
            setTimeout(URLParam.requestTimeout.getIntValue());
        }
        //失败重试
        if(getRetries()==null) {
            setRetries(URLParam.retries.getIntValue());
        }
    }

    @Override
    public void destroy() throws Exception {
        super.destroy0();
    }

    private void checkRegistryConfig() {
        if (CollectionUtil.isEmpty(getRegistries())) {
            for (String name : MoonNamespaceHandler.registryDefineNames) {
                RegistryConfig rc = beanFactory.getBean(name, RegistryConfig.class);
                if (rc == null) {
                    continue;
                }
                if (MoonNamespaceHandler.registryDefineNames.size() == 1) {
                    setRegistry(rc);
                } else if (rc.isDefault() != null && rc.isDefault().booleanValue()) {
                    setRegistry(rc);
                }
            }
        }
        if (CollectionUtil.isEmpty(getRegistries())) {
            setRegistry(FrameworkUtils.getDefaultRegistryConfig());
        }
    }

    private void checkProtocolConfig() {
        if (CollectionUtil.isEmpty(getProtocols())) {
            for (String name : MoonNamespaceHandler.protocolDefineNames) {
                ProtocolConfig pc = beanFactory.getBean(name, ProtocolConfig.class);
                if (pc == null) {
                    continue;
                }
                if (MoonNamespaceHandler.protocolDefineNames.size() == 1) {
                    setProtocol(pc);
                } else if (pc.isDefault() != null && pc.isDefault().booleanValue()) {
                    setProtocol(pc);
                }
            }
        }
        if (CollectionUtil.isEmpty(getProtocols())) {
            setProtocol(FrameworkUtils.getDefaultProtocolConfig());
        }
    }
}
