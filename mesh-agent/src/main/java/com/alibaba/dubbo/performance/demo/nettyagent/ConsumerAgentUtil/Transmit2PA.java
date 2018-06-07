package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil;

/**
 * Created by gexinjie on 2018/6/4.
 */

import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/*
 将这个 handler 标记为 sharable 达到共享count的目的
 */
@ChannelHandler.Sharable
public class Transmit2PA extends ChannelInboundHandlerAdapter {
    private final ConnectManager connectManger;
    private Logger logger = LoggerFactory.getLogger(Http2RequestInvocation.class);


    final AtomicInteger count;
    public Transmit2PA(ConnectManager manager, AtomicInteger count) {
        this.connectManger = manager;
        this.count = count;
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
        long requestID = Invocation.getUniqueRequestID();
        requestInvocation.setRequestID(requestID);
        Channel providerChannel = this.connectManger.getProviderChannel(ctx.channel(), requestID);
//        logger.info("sending to Channel " + providerChannel.toString());
        logger.info("从 CA 传向 PA count: {}", count.get());
        if (count.incrementAndGet() % 10 != 0) {
            providerChannel.write(requestInvocation);
        } else {
            // todo 按照发送数量 flush
            logger.info("flush, send to Provider");
            providerChannel.writeAndFlush(requestInvocation);
        }

    }

}
