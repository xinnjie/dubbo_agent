//package test;
//
//import com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil.Result2Http;
//import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.ChannelPipeline;
//import io.netty.channel.embedded.EmbeddedChannel;
//import io.netty.handler.codec.http.*;
//import org.junit.Before;
//import org.junit.Test;
//
//import static org.junit.Assert.*;
//
///**
// * Created by gexinjie on 2018/6/4.
// */
//public class InvocationResult2HttpTest {
//
//    private Invocation invocation = new Invocation();
//    private EmbeddedChannel channel;
//    private EmbeddedChannel httpChannel;
//
//    @Before
//    public void setUp() throws Exception {
//        invocation.setResult("12342");
//        channel = new EmbeddedChannel(
//                new Result2Http()
//        );
//        httpChannel = new EmbeddedChannel(
//                new ChannelInitializer<EmbeddedChannel>() {
//                    @Override
//                    protected void initChannel(EmbeddedChannel ch) throws Exception {
//                        ChannelPipeline p = ch.pipeline();
//                        p.addLast(new HttpServerCodec());
////                        p.addLast(new )
//                    }
//                }
//        );
//    }
//
//    @Test
//    public void responseInvocation2http() throws Exception {
//        assertTrue(channel.writeOutbound(invocation));
//        FullHttpResponse response = channel.readOutbound();
//        byte[] result = new byte[response.content().readableBytes()];
//        response.content().readBytes(result);
//        assertEquals("12342", new String(result));
//    }
//
//    @Test
//    public void httpPost() throws Exception {
//        FullHttpRequest httpReq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,"/");
//        httpReq.content().writeBytes(invocation.getResult().getBytes());
//        httpChannel.writeOutbound(httpReq);
//    }
//}