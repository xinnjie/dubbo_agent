package com.alibaba.dubbo.performance.demo.nettyagent;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import com.alibaba.dubbo.performance.demo.nettyagent.util.Bytes;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * Created by gexinjie on 2018/5/30.
 */
public class CacheRequestEncoder extends MessageToByteEncoder {
    private final CacheContext cacheContext;


    private Logger logger = LoggerFactory.getLogger(CacheRequestEncoder.class);
    protected  static final short MAGIC = (short) 0xdacc;
    protected  static final int HEADER_LENGTH_MIN = 16;
    protected  static final int HEADER_LENGTH_MAX = 20;

    protected  static final int REQEUST_ID_INDEX = 4;
    protected  static final int DATA_LENGTH_INDEX = 12;
    protected  static final int METHOD_ID_INDEX = 16;





    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_CACHE = (byte) 0x40;
    protected static final byte FLAG_VALID = (byte) 0x20;
//    protected static final byte FLAG_NULL = (byte) 0x10;


    public CacheRequestEncoder(CacheContext cacheContext) {
        super();
        this.cacheContext = cacheContext;
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        InvocationRequest request = (InvocationRequest) msg;

        int startWriteIndex = out.writerIndex();

        boolean isCache = cacheContext.contains(request.getFuncType());
//        int headerLength = isCache ? HEADER_LENGTH_MAX : HEADER_LENGTH_MIN;

        out.writeShort(MAGIC);
        byte flag = 0;
        flag |= FLAG_REQUEST;
        if (isCache) {
            flag |= FLAG_CACHE;
        }
        out.writeByte(flag);
        out.writeByte(0);
        assert request.getRequestID() != -1;
        out.writeLong(request.getRequestID());
        // 跳过 data length 部分，最会回填
        // FIXME: 2018/6/10 有可能出现问题
        out.writerIndex(startWriteIndex + HEADER_LENGTH_MIN);
        if (isCache) {
            Integer methodID = cacheContext.get(request.getFuncType());
            assert methodID != null;
            out.writeInt(methodID);
        } else {
            // 写入 body, 先更改 bytebuf 的 index
            FuncType funcType = request.getFuncType();
            out.writeCharSequence(funcType.getMethodName() + "\n", Charset.forName("utf-8"));
            out.writeCharSequence(funcType.getParameterTypes() + "\n", Charset.forName("utf-8"));
            out.writeCharSequence(funcType.getInterfaceName() + "\n", Charset.forName("utf-8"));
        }
        out.writeCharSequence(request.getArgument(), Charset.forName("utf-8"));
        int totalIndex = out.writerIndex();
        out.writerIndex(startWriteIndex + DATA_LENGTH_INDEX);
        out.writeInt(totalIndex - startWriteIndex);
        out.writerIndex(totalIndex);
        logger.info("sending request to PA: {}, hexdump: {} , current methodsID cache: {}", request, ByteBufUtil.hexDump(out), cacheContext.getMethodIDs());
    }
}
