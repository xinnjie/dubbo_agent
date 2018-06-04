package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentHandlers;

import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;

/**
 * Created by gexinjie on 2018/5/31.
 */
public class SendConsumerHandler extends ChannelOutboundHandlerAdapter{
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Invocation invocation = (Invocation) msg;
        FullHttpRequest httpReq=new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,"CA");
        httpReq.content().writeBytes(invocation.getResult().getBytes());
        ctx.writeAndFlush(httpReq);
    }
}
