package test;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/2.
 */
public class CacheRequestTest {
    @Test
    public void testCacheRequest() throws Exception {
        HashMap<FuncType, Integer> methodsCache = new HashMap<>();
        HashMap<Integer, FuncType> methodIDsCache = new HashMap<>();
        EmbeddedChannel PADecodeChannel = new EmbeddedChannel(
                new CacheDecoder(methodIDsCache, new HashMap<>())
        );

        EmbeddedChannel CAEncodeChannel = new EmbeddedChannel(
                new ChannelInitializer<EmbeddedChannel>() {
                    @Override
                    protected void initChannel(EmbeddedChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ChannelOutboundHandlerAdapter() {
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                PADecodeChannel.writeInbound(buf);
                            }
                        });
                        pipeline.addLast(new CacheEncoder(methodsCache, null));

                    }
                }
        );
//        ByteBuf buf = Unpooled.buffer();
        Invocation invocation = new Invocation();
        invocation.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setParameterTypes("Ljava/lang/String;");
        invocation.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setArguments("adsadjknjkstrange");
        invocation.setMethodName("hash");

        assertFalse(CAEncodeChannel.writeOutbound(invocation));

        Invocation request = PADecodeChannel.readInbound();
        assertEquals(invocation.getArguments(), request.getArguments());
        assertEquals(invocation.getMethodName(), request.getMethodName());
        assertEquals(invocation.getInterfaceName(), request.getInterfaceName());
        assertEquals(invocation.getParameterTypes(), request.getParameterTypes());

        assertTrue(request.getRequestID() != -1);
        // 因为 invocation 是经过 encoder(request side) 后才被赋予  requestID 的，所以 invocation ID 和 request ID 不应该相同
        assertNotEquals(invocation.getRequestID(), request.getRequestID());



    }

}