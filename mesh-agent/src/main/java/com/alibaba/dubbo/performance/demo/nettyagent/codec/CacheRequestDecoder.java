package com.alibaba.dubbo.performance.demo.nettyagent.codec;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import com.alibaba.dubbo.performance.demo.nettyagent.util.GetTraceString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by gexinjie on 2018/5/29.
 */
public class CacheRequestDecoder extends ByteToMessageDecoder {
    //    public static final String NAME = "cache";
    org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    protected static final short MAGIC = (short) 0xdacc;
    private final ConcurrentHashMap<Long, Integer> requestToMethodFirstCache;
    private final CacheContext cacheContext;
    protected  static final int HEADER_LENGTH_MIN = 16;
    protected  static final int HEADER_LENGTH_MAX = 20;

    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_CACHE = (byte) 0x40;
    protected static final byte FLAG_VALID = (byte) 0x20;
    protected static final byte FLAG_NULL = (byte) 0x10;

    protected  static final int REQEUST_ID_INDEX = 4;
    protected  static final int DATA_LENGTH_INDEX = 12;
    protected  static final int METHOD_ID_INDEX = 16;


    /**
     *
     * @param cacheContext
     * @param requestToMethodFirstCache **只在 decode request时会被用到** 需要对应的encoder decoder 共享使用
     *                                  这个 map 将一个 requestID 映射到 methodID.
     *                                  当一次 request 的 method信息 第一次在 PA 上被缓存时，
     *                                  这个 requestID 和新缓存的 methodID 将被加入到这个 map 中，方便 encode response 时，
     *                                  将方法信息和 method 的对应关系传回给 CA 的 response decoder
     */
    public CacheRequestDecoder(CacheContext cacheContext, ConcurrentHashMap<Long, Integer> requestToMethodFirstCache) {
        super();
        this.cacheContext = cacheContext;
        this.requestToMethodFirstCache = requestToMethodFirstCache;
    }

    /*
     输出是 Invocation
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        do {
            int savedReaderIndex = byteBuf.readerIndex();
            Object msg = null;
            try {
                msg = doDecode(byteBuf);
                assert  msg != null;
            } catch (IndexOutOfBoundsException e) {
                handleError(byteBuf, savedReaderIndex, e);
            } catch (Exception e) {
                throw e;
            }
            // 如果还没有传输完成，将 bytebuf 恢复原状
            if (msg == DecodeResult.NEED_MORE_INPUT) {
                byteBuf.readerIndex(savedReaderIndex);
                break;
            } else if (msg == DecodeResult.DECODE_ERROR) {
                handleError("decode error", byteBuf, savedReaderIndex);
            }else{
                list.add(msg);
            }
        } while (byteBuf.isReadable());


    }
    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_INPUT, DECODE_ERROR
    }
    private Object doDecode(ByteBuf byteBuf){
        logger.debug("received hexdump: {}", ByteBufUtil.hexDump(byteBuf));


        final int startIndex = byteBuf.readerIndex();
        int readable = byteBuf.readableBytes();

        if (readable < HEADER_LENGTH_MIN) {
            return DecodeResult.NEED_MORE_INPUT;
        }
        final short code = byteBuf.getShort(0);
        // todo 这个时候服务器应该停下
        if (code != MAGIC) {
            logger.error("not correct code");
            return DecodeResult.DECODE_ERROR;
        }
        final boolean isRequest = (byteBuf.getByte(2) & FLAG_REQUEST) != 0 ;
        final boolean isCache = (byteBuf.getByte(2) & FLAG_CACHE) != 0;
        final boolean isValid = (byteBuf.getByte(2) & FLAG_VALID) != 0;
        final int dataLength = byteBuf.getInt(startIndex+DATA_LENGTH_INDEX);
        //todo 错误处理 返回 NULL
        boolean isNull = (byteBuf.getByte(2) & FLAG_NULL) != 0;



        final int HeaderLength = isCache ? HEADER_LENGTH_MAX : HEADER_LENGTH_MIN;

        final int bodyLength = dataLength - HeaderLength;
        if (readable < dataLength) {
//            byteBuf.readerIndex(startIndex);
            return DecodeResult.NEED_MORE_INPUT;
        }

        byteBuf.readerIndex(startIndex + REQEUST_ID_INDEX);
        final long requestId = byteBuf.readLong();
        // 跳过 data length
        byteBuf.readInt();

        /* ************ request decode ************************
         这部分代码负责收到 request 的 decode 逻辑
         注意，对  方法信息进行缓存的操作发生这里(request 的 decode 阶段)。
         除了单纯地对方法信息进行缓存外，缓存后还要通知 CA 这个方法已经进行了缓存，在 encode response 的阶段，需要将新缓存的 method 的告知给 CA
          Q:为什么把 request 的缓存操作放在这里?
          A:从 dubbo 接收到的结果中(也就是``` decode dubbo 信息之后)，只包含结果，不含有关于方法的信息。
             如果这里不进行缓存，在 response 的encode 阶段就会丢失方法信息。
          ******************************************************/
        InvocationRequest request = new InvocationRequest();
        request.setRequestID(requestId);
        int methodID;
        if (isCache) {
            methodID = byteBuf.readInt();
            // 从缓存中取出有关方法信息：methodName, interfaceName, parameterTypes
            FuncType funcType = cacheContext.get(methodID);
            if (funcType == null) {
                logger.error("method id {} is not cached, current cacheTable is {}, body hexdump(starts after methodiD): {}", methodID, cacheContext.getMethodIDs(), ByteBufUtil.hexDump(byteBuf));
                return DecodeResult.DECODE_ERROR;
            }
            request.setFuncType(funcType);
            // todo 现在假设只有一个参数，以后改进
            if (byteBuf.readableBytes() < bodyLength) {
                logger.error("body data length is not right: {}, body hexdum(starts after methodiD) : {}", bodyLength, ByteBufUtil.hexDump(byteBuf));
            }
            String body = byteBuf.readCharSequence(bodyLength, Charset.forName("utf-8")).toString();
            request.setArgument(body.trim());

        }
        /*
        假如没有 cache 过，将现在这个 method 进行 cache
         */
        else {
            /*
             TODO 忽略 attachment
             attachment 包括
                  * Dubbo version
                  * Service name
                  * Service version
             */
            String body = byteBuf.readCharSequence(bodyLength, Charset.forName("utf-8")).toString();
            String[] parts = body.split("\n");
            assert parts.length >= 4;
            if(parts.length < 4) {
                logger.error("request format is not right, to few parts(expected 4): {}");
            }
            FuncType newType = new FuncType();
            newType.setMethodName(parts[0]);
            newType.setParameterTypes(parts[1]);
            newType.setInterfaceName(parts[2]);
            request.setArgument(parts[3]);
            request.setFuncType(newType);

            logger.debug("PA current methods size: {}, first request size: {}", cacheContext.size(), requestToMethodFirstCache.size());
            if (!this.cacheContext.contains(newType)) {
                int newMethodID = Invocation.getUniqueMethodID();
                this.cacheContext.put(newMethodID, newType);
                this.requestToMethodFirstCache.put(request.getRequestID(), newMethodID);
                logger.debug("new functype insert into PA cache: {}", newType);
            }
        }
        logger.debug("received from CA: {}", request);
        return request;

    }

    private void handleError(ByteBuf byteBuf, int savedReaderIndex, Throwable e) {
        byteBuf.readerIndex(savedReaderIndex);
        boolean isCached = (byteBuf.getByte(2) & FLAG_CACHE) != 0,
                isValid = (byteBuf.getByte(2) & FLAG_VALID) != 0,
                isRequest = (byteBuf.getByte(2) & FLAG_REQUEST) != 0;
        long requestID = byteBuf.getLong(REQEUST_ID_INDEX);
        final int dataLength = byteBuf.getInt(savedReaderIndex+DATA_LENGTH_INDEX);
        logger.error("informations:\n" +
                        "request id: {}\n" +
                        "isRequest: {}\n" +
                        "isCached: {}\n" +
                        "isValid: {}\n" +
                        "dataLength: {}\n" +
                        "bytebuf readable: {}\n" +
                        "hexdump: {}\n" +
                        "IndexOutOfBoundsException: {}", requestID, isRequest,isCached, isValid, dataLength ,
                byteBuf.readableBytes(), ByteBufUtil.hexDump(byteBuf) , GetTraceString.get(e));
        byteBuf.clear();
    }
    private void handleError(String errorMessage, ByteBuf byteBuf, int savedReaderIndex) {
        byteBuf.readerIndex(savedReaderIndex);
        boolean isCached = (byteBuf.getByte(2) & FLAG_CACHE) != 0,
                isValid = (byteBuf.getByte(2) & FLAG_VALID) != 0,
                isRequest = (byteBuf.getByte(2) & FLAG_REQUEST) != 0;
        long requestID = byteBuf.getLong(REQEUST_ID_INDEX);
        final int dataLength = byteBuf.getInt(savedReaderIndex+DATA_LENGTH_INDEX);
        logger.error("error message: {}:\n" +
                        "request id: {}\n" +
                        "isRequest: {}\n" +
                        "isCached: {}\n" +
                        "isValid: {}\n" +
                        "dataLength: {}\n" +
                        "bytebuf readable: {}\n" +
                        "hexdump: {}\n" , errorMessage, requestID, isRequest,isCached, isValid, dataLength ,
                byteBuf.readableBytes(), ByteBufUtil.hexDump(byteBuf));
        byteBuf.clear();
    }
}
