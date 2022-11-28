package moon.config.springsupport;

import moon.common.URLParam;
import moon.config.ApplicationConfig;
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
import org.springframework.util.CollectionUtils;

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

    @Override
    public void checkApplication() {
        if (!CollectionUtils.isEmpty(MoonNamespaceHandler.applicationConfigDefineNames)) {
            for (String applicationConfigDefineName : MoonNamespaceHandler.applicationConfigDefineNames) {
                super.application =  beanFactory.getBean(applicationConfigDefineName, ApplicationConfig.class);
            }
        }
        super.checkApplication();
    }

    private void checkRegistryConfig() {
        //<moon:reference id="demoService" 节点的registry属性没有赋值，那么取<moon:registry 节点的值
        if (CollectionUtil.isEmpty(getRegistries())) {
            for (String name : MoonNamespaceHandler.registryDefineNames) {
                RegistryConfig rc = beanFactory.getBean(name, RegistryConfig.class);
                if (rc == null) {
                    continue;
                }
                if (MoonNamespaceHandler.registryDefineNames.size() == 1) {
                    setRegistry(rc);
                    //配置多个注册中心，注入默认的注册方式
                } else if (rc.isDefault() != null && rc.isDefault().booleanValue()) {
                    setRegistry(rc);
                }
            }
        }
        //如果没配置，将本地注册方式配置
        if (CollectionUtil.isEmpty(getRegistries())) {
            setRegistry(FrameworkUtils.getDefaultRegistryConfig());
        }
    }

    private void checkProtocolConfig() {
        //如果<moon:reference 节点的protocol属性没有赋值，那么取<moon:protocol 节点的值
        if (!CollectionUtil.isEmpty(getProtocols())) {
            for (String name : MoonNamespaceHandler.protocolDefineNames) {
                ProtocolConfig pc = beanFactory.getBean(name, ProtocolConfig.class);
                if (pc == null) {
                    continue;
                }
                if (MoonNamespaceHandler.protocolDefineNames.size() == 1) {
                    setProtocol(pc);
                    //配置多个协议，注入默认的协议
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
