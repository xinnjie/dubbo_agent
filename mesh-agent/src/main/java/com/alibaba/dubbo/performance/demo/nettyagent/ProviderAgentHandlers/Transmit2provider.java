package com.alibaba.dubbo.performance.demo.nettyagent.ProviderAgentHandlers;

import com.alibaba.dubbo.performance.demo.nettyagent.AgentConfig;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;


import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gexinjie on 2018/6/7.
 */
public class Transmit2provider extends ChannelInboundHandlerAdapter{
org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
//    private AtomicInteger count = new AtomicInteger(0);

    ChannelFuture providerChannelFuture;

    public Transmit2provider(ChannelFuture providerChannelFuture) {
        this.providerChannelFuture = providerChannelFuture;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        InvocationRequest request = (InvocationRequest) msg;

        if (providerChannelFuture.isSuccess()) {
            Channel providerChannel = providerChannelFuture.channel();
            if (providerChannel.isActive()) {

//                        logger.debug("PA 向 provider 写入了 invocation，观察  cache encode 是否被调用了");
                // 将收到的 request 发给 provider
//                boolean needToFlush = this.count.incrementAndGet() % AgentConfig.SEND_ONCE == 0;
//                if (needToFlush) {
//                    logger.debug("PA to Provider flush");
//                    providerChannel.writeAndFlush(request);
//                } else {
                    providerChannel.writeAndFlush(request);
//                }

            } else {
                logger.error("connection to provider down! 并且还没有被恢复");
            }
        }
        else {
              logger.error("connection to provider is not established");

//            logger.debug("connection to provider is not established yet, add a listener");
//            providerChannelFuture.addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//                    if (future.isSuccess()) {
//                        logger.debug("connection to provider established，and listener is called");
//                        Channel providerChannel = future.channel();
//                        providerChannel.writeAndFlush(request);
//                    } else {
//                        logger.error("connection to provider failed error message: " + future.cause().getMessage());
//                    }
//                }
//            });
        }


    }
}
