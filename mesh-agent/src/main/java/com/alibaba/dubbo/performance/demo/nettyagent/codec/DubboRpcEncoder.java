package com.alibaba.dubbo.performance.demo.nettyagent.codec;

import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import com.alibaba.dubbo.performance.demo.nettyagent.util.Bytes;
import com.alibaba.dubbo.performance.demo.nettyagent.util.JsonUtils;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;

public class DubboRpcEncoder extends ChannelOutboundHandlerAdapter{
    // header length.
    protected static final int HEADER_LENGTH = 16;
    // magic header.
    protected static final short MAGIC = (short) 0xdabb;
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;

    protected  static final int REQEUST_ID_INDEX = 4;
    protected  static final int DATA_LENGTH_INDEX = 12;
    org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        InvocationRequest request = (InvocationRequest) msg;

        ByteBuf header = ctx.alloc().directBuffer(HEADER_LENGTH, HEADER_LENGTH);

        // set magic number.
        header.writeShort(MAGIC);
        // set request and serialization flag.
        // 6 是 fastjson
        byte flag = (byte) (FLAG_REQUEST | 6);

//        if (req.isTwoWay()) header[2] |= FLAG_TWOWAY;   // true
        flag |= FLAG_TWOWAY;

//        if (req.isEvent()) header[2] |= FLAG_EVENT;    // false
        header.writeByte(flag);
        header.writeByte(0);
        // set request id.
        header.writeLong(request.getRequestID());
        // encode request data.

        ByteBuf body = encodeRequestData(ctx.alloc(), request);

        CompositeByteBuf requestBuff = ctx.alloc().compositeBuffer();

        // 写入data length
        header.writeInt(body.readableBytes());
        requestBuff.addComponent(true, header);
        requestBuff.addComponent(true, body );

        logger.debug("PA sending to Provider, request: {}\nhexdump: {}",request, ByteBufUtil.hexDump(requestBuff));
        ctx.write(requestBuff, promise);
    }

    public ByteBuf encodeRequestData(ByteBufAllocator allocator, InvocationRequest request) throws Exception {

        CompositeByteBuf dubboBuf = allocator.compositeBuffer(3);

        ByteBuf beforeArgument = allocator.directBuffer(200);
        ByteBufOutputStream byteBufBeforeStream = new ByteBufOutputStream(beforeArgument);

        /*
         before argument
         */
        JSON.writeJSONString(byteBufBeforeStream, "2.0.1");
        byteBufBeforeStream.writeByte('\n');

        FuncType funcType = request.getFuncType();
        JSON.writeJSONString(byteBufBeforeStream, funcType.getInterfaceName());
        byteBufBeforeStream.writeByte('\n');

        JSON.writeJSONString(byteBufBeforeStream,null);
        byteBufBeforeStream.writeByte('\n');
        JSON.writeJSONString(byteBufBeforeStream,funcType.getMethodName());
        byteBufBeforeStream.writeByte('\n');
        JSON.writeJSONString(byteBufBeforeStream,funcType.getParameterTypes());
        byteBufBeforeStream.writeByte('\n');
        byteBufBeforeStream.writeByte('"');
         /*
        after argument
         */
        ByteBuf afterArgument = allocator.directBuffer(50);
        ByteBufOutputStream byteBufAfterStream = new ByteBufOutputStream(afterArgument);
        byteBufAfterStream.writeByte('"');
        byteBufAfterStream.writeByte('\n');
        JSON.writeJSONString(byteBufAfterStream,request.getAttachments());
        byteBufAfterStream.writeByte('\n');

        dubboBuf.addComponent(true, beforeArgument);
        dubboBuf.addComponent(true, request.getArgument());
        dubboBuf.addComponent(true, afterArgument);
        return dubboBuf;
    }


}
