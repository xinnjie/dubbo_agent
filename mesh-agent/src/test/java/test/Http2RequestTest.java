package test;

import com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil.Http2Request;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
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
public class Http2RequestTest {
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
                        p.addLast(new Http2Request());
                    }
                }
        );

        assertTrue(channel.writeInbound(buf));
        InvocationRequest request = (InvocationRequest) channel.readInbound();
        FuncType requestType = request.getFuncType();
        System.out.println(request);
        assertEquals("com.alibaba.dubbo.performance.demo.provider.IHelloService", requestType.getInterfaceName());
        assertEquals("hash", requestType.getMethodName());
        assertEquals("Ljava/lang/String;", requestType.getParameterTypes()) ;
        assertEquals("adsadjknjkstrange", request.getArgument());
    }

}