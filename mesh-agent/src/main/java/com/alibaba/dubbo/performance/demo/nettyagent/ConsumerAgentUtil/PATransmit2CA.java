package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil;

import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;

/**
 * Created by gexinjie on 2018/6/16.
 */
public class PATransmit2CA extends ChannelInboundHandlerAdapter{
    private final ConnectManager connectManager;
    org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);


    public PATransmit2CA(ConnectManager connectManager) {
        this.connectManager = connectManager;
    }

    @Override
        public void channelRead(ChannelHandlerContext ctx_, Object msg) throws Exception {
            InvocationResponse response = (InvocationResponse) msg;
            // getAccordingConsumerChannel 将会返回对应于 reqeustID 的 consumerChannel （ps *****requestID 和 consumerChannel有对应关系）
            Channel consumerChannel = this.connectManager.getAccordingConsumerChannel(response.getRequestID());
            if (consumerChannel != null) {
                logger.debug("received result from PA， find the right consumer channel for request " + response.getRequestID() + ": " + consumerChannel.toString());
                // 将来自 PA 的 response 发回给 Consumer
                consumerChannel.writeAndFlush(response);
            } else {
                logger.error("request ID: {}  is duplicated! 肯定还有问题", response.getRequestID());
            }
        }
}
