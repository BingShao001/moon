package moon.config.springsupport;

import moon.config.ProtocolConfig;
import moon.config.RegistryConfig;
import moon.config.ServiceConfig;
import moon.util.CollectionUtil;
import moon.util.FrameworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class ServiceConfigBean<T> extends ServiceConfig<T> implements BeanFactoryAware,
        InitializingBean,
        ApplicationListener<ContextRefreshedEvent>,
        DisposableBean {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private transient BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * 监听上下文事件
     * @param contextRefreshedEvent
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (!isExported()) {
            export();
        }
    }

    /**
     * 解析完环境配置文件后，给service对象复制
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        logger.debug("check service interface:%s config", getInterfaceName());
        //检查application并赋值name
        checkApplication();
        //给<moon:service 节点的registry属性赋值
        checkRegistryConfig();
        //给<moon:service 节点的protocol属性赋值
        checkProtocolConfig();
    }

    @Override
    public void destroy() throws Exception {
        super.destroy0();
    }

    /**
     * 如果<moon:service 节点的registry属性没有赋值，那么取<moon:registry 节点的值
     */
    private void checkRegistryConfig() {
        if (CollectionUtil.isEmpty(getRegistries())) {
            //遍历registry节点的id（多注册中心）
            for (String name : MoonNamespaceHandler.registryDefineNames) {
                RegistryConfig rc = beanFactory.getBean(name, RegistryConfig.class);
                if (rc == null) {
                    continue;
                }
                if (MoonNamespaceHandler.registryDefineNames.size() == 1) {
                    setRegistry(rc);
                    //<moon:registry 节点 default属性默认值true:支持注册，false:不注册
                } else if (rc.isDefault() != null && rc.isDefault().booleanValue()) {
                    setRegistry(rc);
                }
            }
        }
        //如果为空，赋值local
        if (CollectionUtil.isEmpty(getRegistries())) {
            setRegistry(FrameworkUtils.getDefaultRegistryConfig());
        }
    }

    /**
     * 如果<moon:service 节点的protocol属性没有赋值，那么取<moon:protocol 节点的值
     */
    private void checkProtocolConfig() {
        if (CollectionUtil.isEmpty(getProtocols())) {
            //遍历protocol节点id（多协议）
            for (String name : MoonNamespaceHandler.protocolDefineNames) {
                ProtocolConfig pc = beanFactory.getBean(name, ProtocolConfig.class);
                if (pc == null) {
                    continue;
                }
                if (MoonNamespaceHandler.protocolDefineNames.size() == 1) {
                    setProtocol(pc);
                    //<moon:protocol 节点 default属性默认值true:赋值协议，false:不赋值协议
                } else if (pc.isDefault() != null && pc.isDefault().booleanValue()) {
                    setProtocol(pc);
                }
            }
        }
        //设置默认的协议和端口
        if (CollectionUtil.isEmpty(getProtocols())) {
            setProtocol(FrameworkUtils.getDefaultProtocolConfig());
        }
    }
}
