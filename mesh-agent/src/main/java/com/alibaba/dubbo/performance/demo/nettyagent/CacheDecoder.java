package com.alibaba.dubbo.performance.demo.nettyagent;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by gexinjie on 2018/5/29.
 */
public class CacheDecoder extends ByteToMessageDecoder{
//    public static final String NAME = "cache";
    private Logger logger = LoggerFactory.getLogger(CacheDecoder.class);

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
    public CacheDecoder(CacheContext cacheContext, ConcurrentHashMap<Long, Integer> requestToMethodFirstCache) {
        super();
        this.cacheContext = cacheContext;
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

        if (readable < HEADER_LENGTH_MIN) {
            return DecodeResult.NEED_MORE_INPUT;
        }
        final short code = byteBuf.getShort(0);
        // todo 这个时候服务器应该停下
        if (code != MAGIC) {
            return DecodeResult.DECODE_ERROR;
        }
        final boolean isRequest = (byteBuf.getByte(2) & FLAG_REQUEST) != 0 ;
        final boolean isCache = (byteBuf.getByte(2) & FLAG_CACHE) != 0;
        final boolean isValid = (byteBuf.getByte(2) & FLAG_VALID) != 0;
        final int dataLength = byteBuf.getInt(startIndex+DATA_LENGTH_INDEX);
        //todo 错误处理 返回 NULL
        boolean isNull = (byteBuf.getByte(2) & FLAG_NULL) != 0;



        final int HeaderLength = isCache ? HEADER_LENGTH_MAX : HEADER_LENGTH_MIN;

//        byte[] dataLen = Arrays.copyOfRange(header,12,16);
//        int len = Bytes.bytes2int(dataLen);
        final int bodyLength = dataLength - HeaderLength;
        if (readable < dataLength) {
//            byteBuf.readerIndex(startIndex);
            return DecodeResult.NEED_MORE_INPUT;
        }


        // todo todo  这个坑了我两天时间！！！！！
        // byteBuf.readerIndex(REQEUST_ID_INDEX);
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
            // todo 目前认为方法词典缓存足够，所以从 PA 接受到的 response 都是 cache 过的
            assert isCache;
            if (isCache) {
                invocation.setMethodID(byteBuf.readInt());
                if (isValid) {
                    //当 Valid 有效时，说明 response 第一次缓存 method id
                    // 在 invocation 的方法属性读取完毕后放入缓存

                    String body = byteBuf.readCharSequence(bodyLength, Charset.forName("utf-8")).toString();
                    String[] parts = body.split("\n");
                    assert parts.length >= 4;
                    if (parts.length >= 4) {
                        invocation.setMethodName(parts[0]);
                        invocation.setParameterTypes(parts[1]);
                        invocation.setInterfaceName(parts[2]);
                        if (!cacheContext.contains(invocation.getMethodID())) {
                            logger.info("add new cache method: {}", invocation.toString());
                        } else {
                            logger.warn("method is already in cache, before: {}, after: {}", cacheContext.get(invocation.getMethodID()), invocation);
                        }
                        invocation.setResult(parts[3]);
                        cacheContext.put(invocation.getMethodID(), invocation.shallowCopy());
                    } else {
                        logger.error("can not decode. to few parts (should be 4 parts): {}", parts);
                        return DecodeResult.DECODE_ERROR;
                    }
                } else {
                    String body = byteBuf.readCharSequence(bodyLength, Charset.forName("utf-8")).toString();
                    String[] parts = body.split("\n");
                    assert parts.length >= 1;
                    if (parts.length < 1) {
                        logger.error("can not decode. to few parts (should be 1 parts): {}", parts);
                        return DecodeResult.DECODE_ERROR;
                    }
                    invocation.setResult(parts[0]);
                }
            }
            logger.info("CA received response from PA: " + invocation.toString());
            return invocation;

        }
        /* ************ request decode ************************
         这部分代码负责收到 request 的 decode 逻辑
         注意，对  方法信息进行缓存的操作发生这里(request 的 decode 阶段)。
         除了单纯地对方法信息进行缓存外，缓存后还要通知 CA 这个方法已经进行了缓存，在 encode response 的阶段，需要将新缓存的 method 的告知给 CA
          Q:为什么把 request 的缓存操作放在这里?
          A:从 dubbo 接收到的结果中(也就是``` decode dubbo 信息之后)，只包含结果，不含有关于方法的信息。
             如果这里不进行缓存，在 response 的encode 阶段就会丢失方法信息。
          ******************************************************/
        else {
            Invocation invocation = new Invocation();
            invocation.setRequestID(requestId);
            if (isCache) {
                invocation.setMethodID(byteBuf.readInt());
                // 从缓存中取出有关方法信息：methodName, interfaceName, parameterTypes
                logger.info("current methods is cached, methodID is {}", invocation.getMethodID());
                cacheContext.get(invocation.getMethodID()).shallowCopyInPlace(invocation);
                // todo 现在假设只有一个参数，以后改进
                String body = byteBuf.readCharSequence(bodyLength, Charset.forName("utf-8")).toString();
                invocation.setArguments(body.trim());

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
                invocation.setMethodName(parts[0]);
                invocation.setParameterTypes(parts[1]);
                invocation.setInterfaceName(parts[2]);
                invocation.setArguments(parts[3]);
                // TODO 最好在添加缓存时先查看一下缓存中是否含有这个方法的缓存，这里先不做是因为只缓存了 methodID -> funcType 的缓存，如果要找就需要遍历一次 hashMap
                //                if (!this.methods.containsKey(invocation))

                logger.info("PA current methods size: {}, first request size: {}", cacheContext.size(), requestToMethodFirstCache.size());
                // 这里没把 methodID 写入到 invocation 中是因为没必要，invocation 马上会发给 dubbo，debbo 并不需要 methodID 信息
                if (!this.cacheContext.contains(invocation)) {
                    int newMethodID = Invocation.getUniqueMethodID();
                    invocation.setMethodID(newMethodID);
                    this.cacheContext.put(newMethodID, invocation.shallowCopy());
                    this.requestToMethodFirstCache.put(invocation.getRequestID(), newMethodID);
                    logger.info("new functype insert into PA cache: {}", invocation.getMethodName());
                }
            }
            logger.info("received from CA: " + invocation.toString());
            return invocation;
        }

    }

}
