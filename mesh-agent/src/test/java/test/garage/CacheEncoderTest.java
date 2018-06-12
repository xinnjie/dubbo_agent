package test.garage;

import com.alibaba.dubbo.performance.demo.nettyagent.garage.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
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

    /*
    这个测试用来检查一个运行时的错误
    Invocation{methodName='hash', parameterTypes='Ljava/lang/String;', arguments='lsx7sktGyG586Wte', result='null', requestID=335, methodID=-1},
     被 encode request 成了：
    hexdump: daccc000000000000000014f0000001100 , current methodsID size: 1
    dacc c000
    0000 0000
    0000 014f  // reqeustID 335
    0000 0011   // data length 17
    00

     */

    @Test
    public void test() throws Exception {
        Invocation invocation = new Invocation();
        invocation.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setParameterTypes("Ljava/lang/String;");
        invocation.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setRequestID(335);
        invocation.setMethodName("hash");
        invocation.setArguments("lsx7sktGyG586Wte");
        CacheContext cacheContext = new CacheContext();
        cacheContext.put(invocation.shallowCopy(), 23);


        EmbeddedChannel PAEncodeChannel = new EmbeddedChannel(
                new CacheEncoder(cacheContext, null)
        );

        assertTrue(PAEncodeChannel.writeOutbound(invocation));
        ByteBuf out = PAEncodeChannel.readOutbound();
        System.out.println(ByteBufUtil.hexDump(out));
    }
}