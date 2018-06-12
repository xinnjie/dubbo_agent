package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil;

/**
 * Created by gexinjie on 2018/6/4.
 */

import com.alibaba.dubbo.performance.demo.nettyagent.codec.Http2Request;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;


public class Transmit2PA extends ChannelInboundHandlerAdapter {
    private final ConnectManager connectManger;
org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);


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
        InvocationRequest request = (InvocationRequest) msg;
        long requestID = Invocation.getUniqueRequestID();
        request.setRequestID(requestID);
        Channel PAChannel = this.connectManger.getProviderChannel(ctx.channel(), requestID);
        PAChannel.write(request);
    }


}
