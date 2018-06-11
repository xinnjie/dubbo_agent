package com.alibaba.dubbo.performance.demo.nettyagent;

import com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil.ConnectManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gexinjie on 2018/6/11.
 */
public class Accumulator extends ChannelOutboundHandlerAdapter{
    CompositeByteBuf accu;
    final int sendOnce;
    private Logger logger = LoggerFactory.getLogger(Accumulator.class);


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
        logger.info("{}  count {} ", ctx.channel(), accu.numComponents());

    }
}
