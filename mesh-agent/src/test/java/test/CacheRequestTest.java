package test;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/2.
 */
/*
 测试从 CA 发送 invocation 到 PA 的过程（即发送 request 的过程）
 */
public class CacheRequestTest {
    @Test
    public void testCacheRequest() throws Exception {
        ConcurrentHashMap<FuncType, Integer> methodsCache = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, FuncType> methodIDsCache = new ConcurrentHashMap<>();
        EmbeddedChannel PADecodeChannel = new EmbeddedChannel(
                new CacheDecoder(methodIDsCache, new ConcurrentHashMap<>())
        );

        EmbeddedChannel CAEncodeChannel = new EmbeddedChannel(
                new CacheEncoder(methodsCache, null)
        );
//        ByteBuf buf = Unpooled.buffer();
        Invocation invocation = new Invocation();
        invocation.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setParameterTypes("Ljava/lang/String;");
        invocation.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setArguments("adsadjknjkstrange");
        invocation.setMethodName("hash");
        invocation.setRequestID(31);

        assertTrue(CAEncodeChannel.writeOutbound(invocation));
        ByteBuf requestBytes = CAEncodeChannel.readOutbound();
        assertTrue(PADecodeChannel.writeInbound(requestBytes));
        Invocation request = PADecodeChannel.readInbound();
        assertEquals(invocation.getArguments(), request.getArguments());
        assertEquals(invocation.getMethodName(), request.getMethodName());
        assertEquals(invocation.getInterfaceName(), request.getInterfaceName());
        assertEquals(invocation.getParameterTypes(), request.getParameterTypes());

        assertTrue(request.getRequestID() != -1);
        assertEquals(invocation.getRequestID(), request.getRequestID());



    }

}