package com.alibaba.dubbo.performance.demo.nettyagent;

/**
 * Created by gexinjie on 2018/6/1.
 */
public class AgentApp {
    public static void main(String[] args) {
        String type = System.getProperty("type");   // 获取type参数
        if ("consumer".equals(type)){
            new NettyConsumerAgent().run();
        }
        else if ("provider".equals(type)){
            new NettyProviderAgent().run();
        }else {
            System.err.println("Environment variable type is needed to set to provider or consumer.");
        }
    }
}
