package test;

import com.alibaba.dubbo.performance.demo.nettyagent.codec.Http2Request;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.ByteProcessor;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/2.
 */
public class Http2RequestTest {
    private ByteBuf inputBytes;
    private FullHttpRequest request;

    @Before
    public void setUp() throws Exception {
        String postStr = "POST / HTTP/1.1\n" +
                "HOST: www.hello.com\n" +
                "content-type: application/x-www-form-urlencoded\n" +
                "content-length: 147\n" +
                "\n" +
                "interface=com.alibaba.dubbo.performance.demo.provider.IHelloService&method=hash&parameterTypesString=Ljava/lang/String;&parameter=adsadjknjkstrange";
        inputBytes = Unpooled.buffer();
        inputBytes.writeCharSequence(postStr, Charset.forName("utf-8"));


        EmbeddedChannel channel = new EmbeddedChannel(
                new ChannelInitializer<EmbeddedChannel>() {
                    @Override
                    protected void initChannel(EmbeddedChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpServerCodec());
                        p.addLast( new HttpObjectAggregator(100000));
                    }
                }
        );

        assertTrue(channel.writeInbound(inputBytes));
        request = (FullHttpRequest) channel.readInbound();
    }

    @Test
    public void testPostRead() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(
                  new Http2Request()
        );

        assertTrue(channel.writeInbound(request));
        InvocationRequest request = (InvocationRequest) channel.readInbound();
        FuncType requestType = request.getFuncType();
        System.out.println(request);
        assertEquals("com.alibaba.dubbo.performance.demo.provider.IHelloService", requestType.getInterfaceName());
        assertEquals("hash", requestType.getMethodName());
        assertEquals("Ljava/lang/String;", requestType.getParameterTypes()) ;
        assertEquals("adsadjknjkstrange", request.getArgument());
    }

    @Test
    public void testRequest() throws Exception {
        ByteBuf content = request.content();
        int index = content.forEachByte(new ByteProcessor() {
            @Override
            public boolean process(byte value) throws Exception {
                return value != (byte)'&' ;
            }
        });
        index = content.forEachByte(new ByteProcessor() {
            @Override
            public boolean process(byte value) throws Exception {
                return value != (byte)'&' ;
            }
        });
    }
}