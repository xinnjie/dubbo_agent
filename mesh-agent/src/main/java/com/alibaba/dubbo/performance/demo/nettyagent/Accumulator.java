package com.alibaba.dubbo.performance.demo.nettyagent;

import com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil.ConnectManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.logging.log4j.LogManager;

/**
 * Created by gexinjie on 2018/6/11.
 */
public class Accumulator extends ChannelOutboundHandlerAdapter{
    final int sendOnce;
    int count = 0;
    org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);


    public Accumulator(int sendOnce) {
        this.sendOnce = sendOnce;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        count++;
        if (count == sendOnce) {
            ctx.writeAndFlush(buf, promise);
            count = 0;
        } else {
            ctx.write(buf, promise);
        }

    }
}
