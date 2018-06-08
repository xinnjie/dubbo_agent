package test;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.DubboRpcDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.DubboRpcEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/2.
 */


/*
 测试从 PA 发送 invocation 到 CA 的过程（即发送 response 的过程）
 */
public class CacheResponseTest {
    Invocation invocation;
    Invocation dubboRequest = new Invocation();

    @Before
    public void setUp() throws Exception {
        long requestID = 32;
        String arguments = "hkhdkfhakjf";
        invocation = new Invocation();
        invocation.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setParameterTypes("Ljava/lang/String;");
        invocation.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setRequestID(requestID);
        invocation.setMethodName("hash");
        invocation.setMethodID(10);
        invocation.setResult("hello");
        invocation.setArguments(arguments);

        invocation.shallowCopyInPlace(dubboRequest);
        dubboRequest.setRequestID(requestID);
        dubboRequest.setArguments(arguments);

    }


    /*
    模拟 PA 向 CA 发送了一个刚被缓存的 response，PA 需要将对应的方法号和方法信息登记到自己的缓存中，以供下次发送相同方法使用
     */
    @Test
    public void testResponse() throws Exception {

        /*
        构建出一个给 PA response encoder 用的缓存，这个缓存应该在 decode request 时已经将相应的方法名、方法号信息登记到了缓存中
        并且在 requestToMethodFirstCache 加进了相应的 request ID
         */
        CacheContext cacheContextPA = new CacheContext();
        CacheContext cacheContextCA = new CacheContext();
//        cacheContextCA.put(invocation.shallowCopy(), invocation.getMethodID());
        ConcurrentHashMap<Long, Integer> requestToMethodFirstCache = new ConcurrentHashMap<>();
        requestToMethodFirstCache.put(invocation.getRequestID(), invocation.getMethodID());


        cacheContextPA.put(invocation.getMethodID(), invocation.shallowCopy());

        EmbeddedChannel CADecodeChannel = new EmbeddedChannel(
                new CacheDecoder(cacheContextCA, null)
        );
        EmbeddedChannel PAEncodeChannel = new EmbeddedChannel(
                new CacheEncoder(cacheContextPA, requestToMethodFirstCache)
        );

        assertEquals(1, requestToMethodFirstCache.size());
        assertEquals(1, cacheContextPA.size());


        /*
        构建一个 invocation 模仿 一个 response
         */
        Invocation dubboResponse = new Invocation();
        dubboResponse.setRequestID(invocation.getRequestID());
        dubboResponse.setResult(invocation.getResult());

        assertTrue(PAEncodeChannel.writeOutbound(dubboResponse));
        assertEquals(1, cacheContextPA.size());
        assertEquals(0, requestToMethodFirstCache.size());


        ByteBuf responseBytes = PAEncodeChannel.readOutbound();
        assertEquals(0, cacheContextCA.size());
        assertTrue(CADecodeChannel.writeInbound(responseBytes));
        assertEquals(1, cacheContextCA.size());


        Invocation response = CADecodeChannel.readInbound();
        assertNull(response.getArguments());
        assertEquals(response.getMethodName(), invocation.getMethodName());
        assertEquals(response.getInterfaceName(), invocation.getInterfaceName());
        assertEquals(response.getParameterTypes(), invocation.getParameterTypes());
        assertEquals(response.getResult(), invocation.getResult());
        assertEquals(response.getRequestID(), invocation.getRequestID());

    }

}