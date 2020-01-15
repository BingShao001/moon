# moon
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![Release Version](https://img.shields.io/badge/release-1.0.1-red.svg)](https://github.com/TFdream/moon/releases) [![Build Status](https://travis-ci.org/TFdream/moon.svg?branch=master)](https://travis-ci.org/TFdream/moon)

## Overview
moon is a high-performance, open-source java RPC framework. 

## Features
* Supports various serialization protocol, like [protostuff](http://protostuff.io), Kryo, Hessian, msgpack, Jackson, Fastjson.
* Supports advanced features like load-balance(random, Round-Robin), HA strategy(Failfast, Failover).
* Supports service discovery services like ZooKeeper or Consul.
* Supports oneway, synchronous or asynchronous invoking.
* Supports SPI extension.
* Easy integrated with Spring Framework 4.x.

## Requirements
The minimum requirements to run the quick start are:
* JDK 1.7 or above
* A java-based project management software like [Maven](https://maven.apache.org/) or [Gradle](http://gradle.org/)

## Quick Start

### 1. Synchronous calls
1. Add dependencies to pom.
```
    <dependency>
        <groupId>com.mindflow</groupId>
        <artifactId>moon-core</artifactId>
        <version>1.0.1</version>
    </dependency>

    <dependency>
        <groupId>com.mindflow</groupId>
        <artifactId>moon-registry-zk</artifactId>
        <version>1.0.1</version>
    </dependency>
    
    <!-- dependencies blow were only needed for spring integrated -->
    <dependency>
        <groupId>com.mindflow</groupId>
        <artifactId>moon-springsupport</artifactId>
        <version>1.0.1</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>4.3.6</version>
    </dependency>
```

2. Create an interface for both service provider and consumer.
```
public interface DemoService {

    void hello(String msg);

    String echo(String msg);

    Map<String, String> introduce(String name, List<String> hobbies);
}
```

3. Write an implementation, create and start RPC Server.
```
@Service("demoService")
public class DemoServiceImpl implements DemoService {

    @Override
    public void hello(String msg) {
        System.out.println(msg);
    }

    @Override
    public String echo(String msg) {
        return "hello, "+msg;
    }

    @Override
    public Map<String, String> introduce(String name, List<String> hobbies) {
        System.out.println("name:"+name + ", hobbies:"+hobbies);
        Map<String, String> map = new HashMap<>();
        map.put("name", name);
        return map;
    }

}
```

moon-server.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:moon="http://code.mindflow.com/schema/moon"
       xmlns:util="http://www.springframework.org/schema/util"
       xsi:schemaLocation="
       http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://code.mindflow.com/schema/moon http://code.mindflow.com/schema/moon/moon.xsd
       http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util.xsd
       http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

    <context:annotation-config/>
    <context:component-scan base-package="moon.demo"/>

    <moon:application name="moon-server" />
    
    <moon:protocol name="moon" port="21918"/>

    <moon:registry protocol="zookeeper" address="localhost:2181"/>

    <!--export services-->
    <moon:service interface="moon.demo.service.DemoService" ref="demoService" group="group1" version="1.0.0" />
    <moon:service interface="moon.demo.service.UserService" ref="userService" version="1.0.0" />

</beans>
```

ServerApp.java
```
public class ServerApp {

    public static void main( String[] args ) {

        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:moon-server.xml");
        System.out.println("server start...");
    }
}
```

4. Create and start RPC Client.
moon-client.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:util="http://www.springframework.org/schema/util"
       xmlns:moon="http://code.mindflow.com/schema/moon"
       xsi:schemaLocation="
       http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://code.mindflow.com/schema/moon http://code.mindflow.com/schema/moon/moon.xsd
       http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util.xsd
       http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

    <context:annotation-config/>
    <context:component-scan base-package="moon.demo.client"/>

    <moon:application name="moon-client" />
    
    <moon:protocol name="moon" port="21918"/>

    <moon:registry protocol="zookeeper" address="localhost:2181"/>

    <!--refer services-->
    <moon:reference id="demoService" interface="moon.demo.service.DemoService" group="group1" />
    <moon:reference id="userService" interface="moon.demo.service.UserService"/>

</beans>
```

ClientApp.java
```
public class ClientApp {

    public static void main( String[] args ) {

        ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:moon-client.xml");

        DemoService service = (DemoService) ctx.getBean("demoService");

        service.hello("rpc");
        System.out.println("echo:"+service.echo("rpc"));

        List<String> hobbies = new ArrayList<>();
        hobbies.add("NBA");
        hobbies.add("Reading");
        Map<String, String> map = service.introduce("hh", hobbies);
        System.out.println("map:"+map);
    }
}
```

### 2. Asynchronous calls
In developing.
