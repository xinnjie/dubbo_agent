package test;

import com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentHandlers.Http2RequestInvocation;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/2.
 */
public class Http2RequestInvocationTest {
    @Test
    public void testPostRead() throws Exception {
        String postStr = "POST / HTTP/1.1\n" +
                "HOST: www.hello.com\n" +
                "content-type: application/x-www-form-urlencoded\n" +
                "content-length: 147\n" +
                "\n" +
                "interface=com.alibaba.dubbo.performance.demo.provider.IHelloService&method=hash&parameterTypesString=Ljava/lang/String;&parameter=adsadjknjkstrange";
        ByteBuf buf = Unpooled.buffer();
        buf.writeCharSequence(postStr, Charset.forName("utf-8"));


        EmbeddedChannel channel = new EmbeddedChannel(
                new ChannelInitializer<EmbeddedChannel>() {
                    @Override
                    protected void initChannel(EmbeddedChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpServerCodec());
                        p.addLast( new HttpObjectAggregator(100000));
                        p.addLast(new Http2RequestInvocation());
                    }
                }
        );

        assertTrue(channel.writeInbound(buf));
        Invocation invocation = (Invocation) channel.readInbound();
        System.out.println(invocation);
        assertEquals("com.alibaba.dubbo.performance.demo.provider.IHelloService", invocation.getInterfaceName());
        assertEquals("hash", invocation.getMethodName());
        assertEquals("Ljava/lang/String;", invocation.getParameterTypes()) ;
        assertEquals("adsadjknjkstrange", invocation.getArguments());
    }

}