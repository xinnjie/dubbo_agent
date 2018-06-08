package test;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gexinjie on 2018/6/8.
 */
public class PipeLineTest {
    @Test
    public void testInboundHandlerAfterEncoder() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(
                new ChannelInitializer<EmbeddedChannel>() {
                    @Override
                    protected void initChannel(EmbeddedChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new ChannelOutboundHandlerAdapter() {
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                System.out.println(ByteBufUtil.hexDump(buf));
                                ctx.write(buf);
                            }
                        });
                        p.addLast(new CacheEncoder(new CacheContext(), new ConcurrentHashMap<>()));
                    }
                }
        );

        Invocation response = TestUtil.getFullInvocation();
        channel.writeOutbound(response);
    }
}
