package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentHandlers;

/**
 * Created by gexinjie on 2018/6/4.
 */

import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Transmit2PA extends ChannelInboundHandlerAdapter {
//    private Logger logger = LoggerFactory.getLogger(Http2RequestInvocation.class);

    ChannelFuture providerChannelFuture = null;

    public Transmit2PA(ChannelFuture providerChannelFuture) {
        this.providerChannelFuture = providerChannelFuture;
    }

    /**
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Invocation requestInvocation = (Invocation) msg;
        if (providerChannelFuture.isDone()) {
            Channel providerChannel = providerChannelFuture.channel();
            providerChannel.writeAndFlush(requestInvocation);
        } else {
            providerChannelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    future.channel().writeAndFlush(requestInvocation);
                }
            });
        }
    }


}
