package test.garage;

import com.alibaba.dubbo.performance.demo.nettyagent.garage.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.garage.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

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
        CacheContext cacheContextPA = new CacheContext();
        CacheContext cacheContextCA = new CacheContext();


        EmbeddedChannel PADecodeChannel = new EmbeddedChannel(
                new CacheDecoder(cacheContextPA, new ConcurrentHashMap<>())
        );

        EmbeddedChannel CAEncodeChannel = new EmbeddedChannel(
                new CacheEncoder(cacheContextCA, null)
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