package test;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.DubboRpcDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.DubboRpcEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

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

    @Test
    public void testResponse() throws Exception {

        /*
        构建出一个给 PA response encoder 用的缓存，这个缓存原本应该在 decode request 时被构建
         */
        HashMap<FuncType, Integer> methodsCache = new HashMap<>();
        methodsCache.put(invocation.shallowCopy(), invocation.getMethodID());
        HashMap<Long, Integer> requestToMethodFirstCache = new HashMap<>();
        requestToMethodFirstCache.put(invocation.getRequestID(), invocation.getMethodID());


        HashMap<Integer, FuncType> methodIDsCache = new HashMap<>();
        methodIDsCache.put(invocation.getMethodID(), invocation.shallowCopy());

        EmbeddedChannel CADecodeChannel = new EmbeddedChannel(
                new CacheDecoder(methodIDsCache, null)
        );
        EmbeddedChannel PAEncodeChannel = new EmbeddedChannel(
                new CacheEncoder(methodsCache, requestToMethodFirstCache)
        );

        /*
        构建一个 invocation 模仿 一个 response
         */
        Invocation dubboResponse = new Invocation();
        dubboResponse.setRequestID(invocation.getRequestID());
        dubboResponse.setResult(invocation.getResult());

        assertTrue(PAEncodeChannel.writeOutbound(dubboResponse));
        ByteBuf responseBytes = PAEncodeChannel.readOutbound();
        assertTrue(CADecodeChannel.writeInbound(responseBytes));

        Invocation response = CADecodeChannel.readInbound();
        assertNull(response.getArguments());
        assertEquals(response.getMethodName(), invocation.getMethodName());
        assertEquals(response.getInterfaceName(), invocation.getInterfaceName());
        assertEquals(response.getParameterTypes(), invocation.getParameterTypes());
        assertEquals(response.getResult(), invocation.getResult());
        assertEquals(response.getRequestID(), invocation.getRequestID());

    }

}