package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil;

import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;

/**
 * Created by gexinjie on 2018/5/31.
 */
public class Result2Http extends ChannelOutboundHandlerAdapter{
    private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
    private static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        InvocationResponse invocation = (InvocationResponse) msg;
        // todo 这里用了 unpooled 的 heap buffer
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(invocation.getResult().getBytes()));
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
        ctx.write(response);
    }

}
