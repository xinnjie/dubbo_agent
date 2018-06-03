package test;

import com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentHandlers.ReceiveConsumerHandler;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.junit.Test;
import sun.awt.EmbeddedFrame;

import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/2.
 */
public class ReceiveConsumerHandlerTest {
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
        ByteBuf input = buf.duplicate();

        EmbeddedChannel dummy = new EmbeddedChannel(new SimpleChannelInboundHandler<Invocation>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Invocation msg) throws Exception {
                ctx.write(msg);
//                ctx.fireChannelRead(msg);
            }
        });
//        EmbeddedChannel dummy = new EmbeddedChannel();
        ChannelFuture future = dummy.newSucceededFuture();
        EmbeddedChannel channel = new EmbeddedChannel(
            new ChannelInitializer<EmbeddedChannel>() {
                @Override
                protected void initChannel(EmbeddedChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast("httpCodec", new HttpServerCodec());
                    p.addLast("aggregator", new HttpObjectAggregator(100000));

                    /*
                    这个当 consumer 传来请求时，将 HTTP POST 中内容提取出来构造一个 invocation，并写入到 PA 中
                     */
                    p.addLast("ReadFromConsumer", new ReceiveConsumerHandler(future));

                }
            }
        );
        // 确认 postStr 和 写入的 bytebuf 是一样的
        assertEquals(postStr, input.getCharSequence(0, input.writerIndex(), Charset.defaultCharset()));
        input.readerIndex(0);

        // ReceiveConsumerHandler 不会向后面的 inboundhandler 传递数据，所以 Inbound 不应该有数据
        assertFalse(channel.writeInbound(input));
        Invocation invocation = (Invocation) dummy.readOutbound();
        System.out.println(invocation);
        assertEquals(invocation.getInterfaceName(), "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        assertEquals(invocation.getMethodName(), "hash");
        assertEquals(invocation.getParameterTypes(), "Ljava/lang/String;");
        assertEquals(invocation.getArguments(), "adsadjknjkstrange");
    }

}