package test;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/6.
 */
public class CacheEncoderTest {
    /*
    模拟 encode Cache 过的 response 过程
     */
    @Test
    public void encode() throws Exception {
        Invocation invocation = new Invocation();
        invocation.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setParameterTypes("Ljava/lang/String;");
        invocation.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setRequestID(35);
        invocation.setMethodName("hash");
        invocation.setMethodID(10);
        invocation.setResult("hello");


        CacheContext cacheContext = new CacheContext();
        cacheContext.put(invocation.shallowCopy(), invocation.getMethodID());

        ConcurrentHashMap<Long, Integer> requestToMethodFirstCache = new ConcurrentHashMap<>();
        requestToMethodFirstCache.put(invocation.getRequestID(), invocation.getMethodID());

        EmbeddedChannel PAEncodeChannel = new EmbeddedChannel(
                new CacheEncoder(cacheContext, requestToMethodFirstCache)
        );

        assertTrue(PAEncodeChannel.writeOutbound(invocation));
        assertEquals(0, requestToMethodFirstCache.size());
        ByteBuf buf = PAEncodeChannel.readOutbound();
        ByteBuf copy = Unpooled.copiedBuffer(buf);
        System.out.println(new String(copy.array()));

        invocation.setRequestID(45);
        assertTrue(PAEncodeChannel.writeOutbound(invocation));
        ByteBuf buf2 = PAEncodeChannel.readOutbound();
        ByteBuf copy2 = Unpooled.copiedBuffer(buf2);
        System.out.println(new String(copy2.array()));

        // 因为第一次 encode response 包含方法信息，所以字节数应该比第二次大
        assertTrue(copy.readableBytes() > copy2.readableBytes());

    }

}