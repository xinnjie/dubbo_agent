package com.alibaba.dubbo.performance.demo.agent;

import com.alibaba.dubbo.performance.demo.agent.dubbo.RpcClient;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Random;

@RestController
public class HelloController {

    private Logger logger = LoggerFactory.getLogger(HelloController.class);
    
    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private RpcClient rpcClient = new RpcClient(registry);
    private Random random = new Random();
    private List<Endpoint> endpoints = null;
    private Object lock = new Object();
    private OkHttpClient httpClient = new OkHttpClient();



    /*
    agent 和 consumer 之间的交互：
    provider agent 会返回给 provider 一个 HTTP response, body 是一个自定义的RPC response
     */
    @RequestMapping(value = "")
    public Object invoke(@RequestParam("interface") String interfaceName,
                         @RequestParam("method") String method,
                         @RequestParam("parameterTypesString") String parameterTypesString,
                         @RequestParam("parameter") String parameter) throws Exception {
        String type = System.getProperty("type");   // 获取type参数
        if ("consumer".equals(type)){
            return consumer(interfaceName,method,parameterTypesString,parameter);
        }
        else if ("provider".equals(type)){
            return provider(interfaceName,method,parameterTypesString,parameter);
        }else {
            return "Environment variable type is needed to set to provider or consumer.";
        }
    }

    /*
        todo provider agent 和 consumer agent 的交互是什么样的？
     */

    /*
    由 consumer agent 向 provider agent 发送 HTTP 请求， provider() 是 provider agent 对 consumer agent 的应答
    HTTP response body 为自定义的一个 自定义的 RpcResponse 的一个字段byte[]
     */
    public byte[] provider(String interfaceName,String method,String parameterTypesString,String parameter) throws Exception {

        Object result = rpcClient.invoke(interfaceName,method,parameterTypesString,parameter);
        return (byte[]) result;
    }

    // q: consumer 处理了 consumer agent 获取到 consumer 请求，并通过 http 长连接转交请求给 provider agent， 由 consumer agent 返回应答给 consumer 的逻辑在哪里?
    // a: 返回的 integer 就是最终 consumer agent 返回给 consumer 的 response body
    public Integer consumer(String interfaceName,String method,String parameterTypesString,String parameter) throws Exception {

        if (null == endpoints){
            synchronized (lock){
                if (null == endpoints){
                    endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                }
            }
        }

        // 简单的负载均衡，随机取一个
        Endpoint endpoint = endpoints.get(random.nextInt(endpoints.size()));

        String url =  "http://" + endpoint.getHost() + ":" + endpoint.getPort();

        RequestBody requestBody = new FormBody.Builder()
                .add("interface",interfaceName)
                .add("method",method)
                .add("parameterTypesString",parameterTypesString)
                .add("parameter",parameter)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            byte[] bytes = response.body().bytes();
            String s = new String(bytes);
            return Integer.valueOf(s);
        }
    }
}
