package com.alibaba.dubbo.performance.demo.agent.dubbo;

import com.alibaba.dubbo.performance.demo.agent.dubbo.model.JsonUtils;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Request;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcFuture;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcInvocation;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcRequestHolder;

import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class RpcClient {
    private Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private ConnectManager connectManager;

    public RpcClient(IRegistry registry){
        this.connectManager = new ConnectManager();
    }

    public Object invoke(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {

        /*

         channel 连接到 127.0.0.1：20880
         下面是 dubbo 的配置文件
            <dubbo:application name="demo-provider"/>
            <dubbo:registry address="zookeeper://127.0.0.1:2181" register="false" subscribe="false"/>
            <dubbo:protocol serialization="fastjson" name="dubbo" />
            <dubbo:provider server="netty4" />
          todo ？？  127.0.0.1:2181 上运行的是 dubbo 的 registry?
         因此 channel 连接到 同一个 docker 中的 provider 上
        */
        Channel channel = connectManager.getChannel();

        // Invocation 表示调用一次方法所要的所有信息，方法名、接口名、参数类型、参数
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName(method);
        invocation.setAttachment("path", interfaceName);
        invocation.setParameterTypes(parameterTypesString);    // Dubbo内部用"Ljava/lang/String"来表示参数类型是String


        // 只是将 parameter jsonfy
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        JsonUtils.writeObject(parameter, writer);
        invocation.setArguments(out.toByteArray());

        Request request = new Request();
        request.setVersion("2.0.0");
        request.setTwoWay(true);
        request.setData(invocation);

        logger.info("requestId=" + request.getId());

        RpcFuture future = new RpcFuture();
        RpcRequestHolder.put(String.valueOf(request.getId()),future);

        /*
         向 channel 写入一个 request，request 沿着 pipeline 被处理，
         从一个 request 对象，自身已经有了调用所需信息，methodname,args...
         但是又用 mData（类型为 object） 字段包含了 Invocation，是有冗余的
         另外还包括：
            requestID   通过这个字段和 request holder 可以访问到 future, future 能访问到 response

          requestid & requestholder
            request ---------> future -------> response

        todo 写于看 DubboRpcDecoder, Encoder 之前，
        todo response 的 byte 字段应该会在从 provider 接受数据时， DubboDecoder 或者 RpcClienHandler 时被更新为 provider返回的结果
        */
        channel.writeAndFlush(request);

        Object result = null;
        try {
            result = future.get();
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
}
