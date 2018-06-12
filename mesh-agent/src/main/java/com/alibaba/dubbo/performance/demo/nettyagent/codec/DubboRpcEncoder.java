package com.alibaba.dubbo.performance.demo.nettyagent.codec;

import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import com.alibaba.dubbo.performance.demo.nettyagent.util.Bytes;
import com.alibaba.dubbo.performance.demo.nettyagent.util.JsonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class DubboRpcEncoder extends MessageToByteEncoder{
    // header length.
    protected static final int HEADER_LENGTH = 16;
    // magic header.
    protected static final short MAGIC = (short) 0xdabb;
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;
org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);


    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception {
        InvocationRequest request = (InvocationRequest) msg;

        // header.
        byte[] header = new byte[HEADER_LENGTH];
        // set magic number.
        Bytes.short2bytes(MAGIC, header);

        // set request and serialization flag.
        // 6 是 fastjson
        header[2] = (byte) (FLAG_REQUEST | 6);

//        if (req.isTwoWay()) header[2] |= FLAG_TWOWAY;   // true
        header[2] |= FLAG_TWOWAY;

//        if (req.isEvent()) header[2] |= FLAG_EVENT;    // false

        // set request id.
        Bytes.long2bytes(request.getRequestID(), header, 4);

        // encode request data.
        int savedWriteIndex = buffer.writerIndex();
        // 因为header 中需要知道 data length, 所以跳过头部写入 request
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        encodeRequestData(bos, request);

        int len = bos.size();
        buffer.writeBytes(bos.toByteArray());
        Bytes.int2bytes(len, header, 12);

        // write
        buffer.writerIndex(savedWriteIndex);
        buffer.writeBytes(header); // write header.
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
        logger.debug("sending request to provider: {}, hexdump: {}",  request.toString(), ByteBufUtil.hexDump(buffer));
    }

    public void encodeRequestData(OutputStream out, InvocationRequest request) throws Exception {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));

//        JsonUtils.writeObject(request.getAttachment("dubbo", "2.0.1"), writer);
        JsonUtils.writeObject("2.0.1", writer);
//        JsonUtils.writeObject(request.getAttachment("path"), writer);
        FuncType funcType = request.getFuncType();
        JsonUtils.writeObject(funcType.getInterfaceName(), writer);
//        JsonUtils.writeObject(request.getAttachment("version"), writer);
        JsonUtils.writeObject(null, writer);
        JsonUtils.writeObject(funcType.getMethodName(), writer);
        JsonUtils.writeObject(funcType.getParameterTypes(), writer);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        PrintWriter jsonwriter = new PrintWriter(new OutputStreamWriter(byteOut));
        JsonUtils.writeObject(request.getArgument(), jsonwriter);

        JsonUtils.writeBytes(byteOut.toByteArray(), writer);

        JsonUtils.writeObject(request.getAttachments(), writer);
//        writer.print(String.format("{\"path\":\"%s\"\n}", funcType.getInterfaceName()));
//        writer.flush();


    }

}
