package com.alibaba.dubbo.performance.demo.nettyagent.garage;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;


import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by gexinjie on 2018/5/29.
 */
public class CacheDecoder0 extends ByteToMessageDecoder{
    //    public static final String NAME = "cache";
    static private Set<Long> requestsIDs = Collections.synchronizedSet(new HashSet<>());
org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    protected static final short MAGIC = (short) 0xdacc;
    private final ConcurrentHashMap<Long, Integer> requestToMethodFirstCache;
    private final ConcurrentHashMap<Integer, FuncType> methods;
    protected  static final int HEADER_LENGTH = 16;

    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_CACHE = (byte) 0x40;
    protected static final byte FLAG_VALID = (byte) 0x20;
    protected static final byte FLAG_NULL = (byte) 0x10;

    protected  static final int REQEUST_ID_INDEX = 4;
    protected  static final int DATA_LENGTH_INDEX = 12;


    public CacheDecoder0(ConcurrentHashMap<Integer, FuncType> methodsCache, ConcurrentHashMap<Long, Integer> requestToMethodFirstCache) {
        super();
        this.methods = methodsCache;
        this.requestToMethodFirstCache = requestToMethodFirstCache;
    }

    /*
     输出是 Invocation
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        try {
            do {
                int savedReaderIndex = byteBuf.readerIndex();
                Object msg = null;
                try {
                    msg = doDecode(byteBuf);
                    assert  msg != null;
                } catch (Exception e) {
                    throw e;
                }
                // 如果还没有传输完成，将 bytebuf 恢复原状
                if (msg == DecodeResult.NEED_MORE_INPUT) {
                    byteBuf.readerIndex(savedReaderIndex);
                    break;
                } else if (msg == DecodeResult.DECODE_ERROR) {
                    byteBuf.readerIndex(savedReaderIndex);
                    logger.error("Decode Error, the bytes received is {}",
                            ByteBufUtil.hexDump(byteBuf));
                    byteBuf.clear();
                }else{
                    list.add(msg);
                }
            } while (byteBuf.isReadable());
        } finally {
            if (byteBuf.isReadable()) {
                byteBuf.discardReadBytes();
            }
        }


    }
    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_INPUT, DECODE_ERROR
    }
    private Object doDecode(ByteBuf byteBuf){

        final int startIndex = byteBuf.readerIndex();
        int readable = byteBuf.readableBytes();

        if (readable < HEADER_LENGTH) {
            return DecodeResult.NEED_MORE_INPUT;
        }
        final short code = byteBuf.getShort(0);
        // todo 这个时候服务器应该停下
        if (code != MAGIC) {
            return DecodeResult.DECODE_ERROR;
        }
        final boolean isRequest = (byteBuf.getByte(2) & FLAG_REQUEST) != 0 ;
        final int dataLength = byteBuf.getInt(startIndex+DATA_LENGTH_INDEX);

        final int bodyLength = dataLength - HEADER_LENGTH;
        if (readable < dataLength) {
            return DecodeResult.NEED_MORE_INPUT;
        }
        byteBuf.readerIndex(startIndex + REQEUST_ID_INDEX);
        final long requestId = byteBuf.readLong();
        // 跳过 data length
        byteBuf.readInt();

        /* ************ response decode ************************
        这部分负责收到 response 的 decode 逻辑
         ******************************************************/
        if (!isRequest) {
            Invocation invocation = new Invocation();
            invocation.setRequestID(requestId);
            String body = byteBuf.readCharSequence(bodyLength, Charset.forName("utf-8")).toString();
            String[] parts = body.split("\n");
            assert parts.length >= 1;
            if (parts.length < 1) {
                logger.error("can not decode. to few parts (should be 1 parts): {}", parts);
                return DecodeResult.DECODE_ERROR;
            }
            invocation.setResult(parts[0]);
            return invocation;
        }
        /* ************ request decode ************************
         这部分代码负责收到 request 的 decode 逻辑
          ******************************************************/
        else {
            if (requestsIDs.contains(requestId)) {
                logger.error("request {} is already received! here received again, don't do that", requestId);
            } else {
                requestsIDs.add(requestId);
            }
            Invocation invocation = new Invocation();
            invocation.setRequestID(requestId);
            String body = byteBuf.readCharSequence(bodyLength, Charset.forName("utf-8")).toString();
            String[] parts = body.split("\n");
            assert parts.length >= 4;
            invocation.setMethodName(parts[0]);
            invocation.setParameterTypes(parts[1]);
            invocation.setInterfaceName(parts[2]);
            invocation.setArguments(parts[3]);
            int endIndex = byteBuf.readerIndex();
            byteBuf.readerIndex(startIndex);
//            logger.debug("received from CA: {}, byte raw: {}", invocation, ByteBufUtil.hexDump(byteBuf));
            byteBuf.readerIndex(endIndex);
            return invocation;
        }

    }

}
