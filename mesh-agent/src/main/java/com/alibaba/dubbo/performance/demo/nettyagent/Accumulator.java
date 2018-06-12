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
    CompositeByteBuf accu;
    final int sendOnce;
org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);


    public Accumulator(int sendOnce) {
        this.sendOnce = sendOnce;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        if (accu == null) {
            accu = ctx.alloc().compositeBuffer(this.sendOnce);
        }
        accu.addComponent(true, buf);
        if (accu.numComponents() == this.sendOnce) {
            ctx.writeAndFlush(accu);
            accu = null;
        }
        logger.debug("{}  count {} ", ctx.channel(), accu.numComponents());

    }
}
