package com.alibaba.dubbo.performance.demo.nettyagent.codec;

import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.logging.log4j.LogManager;

public class DubboRpcEncoder extends ChannelOutboundHandlerAdapter{

    protected static final int INTIAL_METHODID = -1;

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
    private ByteBuf beforeArgument;
    private ByteBuf afterArgument;
    private int lastCachedMethod = INTIAL_METHODID;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        InvocationRequest request = (InvocationRequest) msg;
        assert request.getMethodID() != INTIAL_METHODID;
        ByteBuf header = ctx.alloc().directBuffer(HEADER_LENGTH, HEADER_LENGTH);

        // set magic number.
        header.writeShort(MAGIC);
        // set request and serialization flag.
        // 6 是 fastjson
        byte flag = (byte) (FLAG_REQUEST | 6);
        flag |= FLAG_TWOWAY;
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

//        logger.debug("PA sending to Provider, request: {}\nhexdump: {}",request, ByteBufUtil.hexDump(requestBuff));
        ctx.write(requestBuff, promise);
    }

    public ByteBuf encodeRequestData(ByteBufAllocator allocator, InvocationRequest request) throws Exception {

        CompositeByteBuf dubboBuf = allocator.compositeBuffer(3);


        getBeforeAfter(allocator, request);

        afterArgument.retain();
        beforeArgument.retain();

        dubboBuf.addComponent(true, beforeArgument);
        dubboBuf.addComponent(true, request.getArgument());
        dubboBuf.addComponent(true, afterArgument);
        return dubboBuf;
    }

    /*
    用来获取 argument 前面后后面的部分，结果保存在变量 beforeArgument和afterArgument中
     */
    private void getBeforeAfter(ByteBufAllocator allocator, InvocationRequest request) throws  Exception{
        if (lastCachedMethod != INTIAL_METHODID && lastCachedMethod == request.getMethodID()) {
            return;
        }

         /*
         before argument
         */
        if (beforeArgument != null) {
            beforeArgument.release();
        }
        if (afterArgument != null) {
            afterArgument.release();
        }
        lastCachedMethod = request.getMethodID();
        beforeArgument = allocator.directBuffer(200);
        ByteBufOutputStream byteBufBeforeStream = new ByteBufOutputStream(beforeArgument);

        JSON.writeJSONString(byteBufBeforeStream, "2.0.1");
        byteBufBeforeStream.writeByte('\n');

        FuncType funcType = request.getFuncType();
        JSON.writeJSONString(byteBufBeforeStream, funcType.getInterfaceName());
        byteBufBeforeStream.writeByte('\n');

        JSON.writeJSONString(byteBufBeforeStream, null);
        byteBufBeforeStream.writeByte('\n');
        JSON.writeJSONString(byteBufBeforeStream, funcType.getMethodName());
        byteBufBeforeStream.writeByte('\n');
        JSON.writeJSONString(byteBufBeforeStream, funcType.getParameterTypes());
        byteBufBeforeStream.writeByte('\n');
        byteBufBeforeStream.writeByte('"');
         /*
        after argument
         */
        afterArgument = allocator.directBuffer(50);
        ByteBufOutputStream byteBufAfterStream = new ByteBufOutputStream(afterArgument);
        byteBufAfterStream.writeByte('"');
        byteBufAfterStream.writeByte('\n');
        JSON.writeJSONString(byteBufAfterStream, request.getAttachments());
        byteBufAfterStream.writeByte('\n');
    }

}
