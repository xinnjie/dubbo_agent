package com.alibaba.dubbo.performance.demo.nettyagent.registry;

import java.util.List;
import java.util.Map;

public interface IRegistry {

    // 注册服务
    void register(String serviceName, int port, int maxConnectionNum) throws Exception;

    Map<Endpoint, Integer> find(String serviceName) throws Exception;
}
