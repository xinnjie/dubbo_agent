package com.alibaba.dubbo.performance.demo.nettyagent.codec;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;


import java.nio.charset.Charset;

/**
 * Created by gexinjie on 2018/5/30.
 */
public class CacheRequestEncoder extends ChannelOutboundHandlerAdapter {
    private final CacheContext cacheContext;


org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
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
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        InvocationRequest request = (InvocationRequest) msg;
        CompositeByteBuf requestMessage = ctx.alloc().compositeBuffer();

        boolean isCache = cacheContext.contains(request.getFuncType());
        int headerLength = isCache ? HEADER_LENGTH_MAX : HEADER_LENGTH_MIN;
        ByteBuf header = ctx.alloc().directBuffer(headerLength, headerLength),
                funcInfo = null;


        header.writeShort(MAGIC);
        byte flag = 0;
        flag |= FLAG_REQUEST;
        if (isCache) {
            flag |= FLAG_CACHE;
        }
        header.writeByte(flag);
        header.writeByte(0);
        assert request.getRequestID() != -1;
        header.writeLong(request.getRequestID());
        // 跳过 data length 部分，最后回填
        // FIXME: 2018/6/10 有可能出现问题
        header.writerIndex(HEADER_LENGTH_MIN);
        if (isCache) {
            Integer methodID = cacheContext.get(request.getFuncType());
            assert methodID != null;
            header.writeInt(methodID);
        } else {
            funcInfo = ctx.alloc().directBuffer(200);
            FuncType funcType = request.getFuncType();
            funcInfo.writeCharSequence(funcType.getMethodName() + "\n", Charset.forName("utf-8"));
            funcInfo.writeCharSequence(funcType.getParameterTypes() + "\n", Charset.forName("utf-8"));
            funcInfo.writeCharSequence(funcType.getInterfaceName() + "\n", Charset.forName("utf-8"));
        }

        int funcInfoLength = isCache ? 0 : funcInfo.readableBytes();
        int totalLength = headerLength + funcInfoLength + request.getArgument().readableBytes();
        header.writerIndex(DATA_LENGTH_INDEX);
        header.writeInt(totalLength);
        header.writerIndex(headerLength);

        requestMessage.addComponent(true, header);
        if (!isCache && funcInfo != null) {
            requestMessage.addComponent(true, funcInfo);
        }
        requestMessage.addComponent(true, request.getArgument());
//        logger.debug("sending request to PA: {}\n hexdump: {} , current methodsID cache: {}", request, ByteBufUtil.hexDump(requestMessage), cacheContext.getMethodIDs());
        ctx.write(requestMessage, promise);
    }

}
