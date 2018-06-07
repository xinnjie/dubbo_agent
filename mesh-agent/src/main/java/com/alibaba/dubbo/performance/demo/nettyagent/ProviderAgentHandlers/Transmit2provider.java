package com.alibaba.dubbo.performance.demo.nettyagent.ProviderAgentHandlers;

import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gexinjie on 2018/6/7.
 */
public class Transmit2provider extends ChannelInboundHandlerAdapter{
    private Logger logger = LoggerFactory.getLogger(Transmit2provider.class);

    ChannelFuture providerChannelFuture;

    public Transmit2provider(ChannelFuture providerChannelFuture) {
        this.providerChannelFuture = providerChannelFuture;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Invocation invocation = (Invocation) msg;

        if (providerChannelFuture.isSuccess()) {
            Channel providerChannel = providerChannelFuture.channel();
            if (providerChannel.isActive()) {
//                        logger.info("PA 向 provider 写入了 invocation，观察  cache encode 是否被调用了");
                // 将收到的 request 发给 provider
                providerChannel.write(invocation);
            } else {
                logger.error("connection to provider down! 并且还没有被恢复");
            }
        }
        else {
            logger.info("connection to provider is not established yet, add a listener");
            providerChannelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        logger.info("connection to provider established，and listener is called");
                        Channel providerChannel = future.channel();
                        providerChannel.write(invocation);
                    } else {
                        logger.error("connection to provider failed error message: " + future.cause().getMessage());
                    }
                }
            });
        }

    }
}
