package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil;

/**
 * Created by gexinjie on 2018/6/4.
 */

import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class Transmit2PA extends ChannelInboundHandlerAdapter {
    private final ConnectManager connectManger;
    private Logger logger = LoggerFactory.getLogger(Http2RequestInvocation.class);


    public Transmit2PA(ConnectManager manager) {
        this.connectManger = manager;
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
        providerChannel.writeAndFlush(requestInvocation);
    }


}
