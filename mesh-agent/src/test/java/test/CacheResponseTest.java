package test;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/2.
 */
public class CacheResponseTest {
    @Test
    public void testResponse() throws Exception {
        Invocation invocation = new Invocation();
        invocation.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setParameterTypes("Ljava/lang/String;");
        invocation.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setRequestID(5);
        invocation.setMethodName("hash");
        invocation.setMethodID(10);
        invocation.setResult("hello");
        invocation.setArguments("hkhdkfhakjf");

        /*
        构建出一个给 PA response encoder 用的缓存，这个缓存原本应该在 decode request 时被构建
         */
        HashMap<FuncType, Integer> methodsCache = new HashMap<>();
        methodsCache.put(invocation.shallowCopy(), invocation.getMethodID());
        HashMap<Long, Integer> requestToMethodFirstCache = new HashMap<>();
        requestToMethodFirstCache.put(invocation.getRequestID(), invocation.getMethodID());


        HashMap<Integer, FuncType> methodIDsCache = new HashMap<>();
        EmbeddedChannel CADecodeChannel = new EmbeddedChannel(
                new CacheDecoder(methodIDsCache, null)
        );
        EmbeddedChannel PAEncodeChannel = new EmbeddedChannel(
                new ChannelInitializer<EmbeddedChannel>() {
                    @Override
                    protected void initChannel(EmbeddedChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ChannelOutboundHandlerAdapter() {
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                CADecodeChannel.writeInbound(buf);
                            }
                        });
                        pipeline.addLast(new CacheEncoder(methodsCache,requestToMethodFirstCache));
                    }
                }
        );
        /*
        构建一个 invocation 模仿 一个 response
         */
        Invocation dubboResponse =  new Invocation();
        dubboResponse.setRequestID(invocation.getRequestID());;
        dubboResponse.setResult(invocation.getResult());

        assertFalse(PAEncodeChannel.writeOutbound(dubboResponse));

        Invocation response = CADecodeChannel.readInbound();
        assertNull(response.getArguments());
        assertEquals(response.getMethodName(), invocation.getMethodName());
        assertEquals(response.getInterfaceName(), invocation.getInterfaceName());
        assertEquals(response.getParameterTypes(), invocation.getParameterTypes());
        assertEquals(response.getResult(), invocation.getResult());
        assertEquals(response.getRequestID(), invocation.getRequestID());

    }

}