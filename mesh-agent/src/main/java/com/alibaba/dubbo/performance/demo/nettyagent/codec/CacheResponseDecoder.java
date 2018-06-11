package com.alibaba.dubbo.performance.demo.nettyagent.codec;
import com.alibaba.dubbo.performance.demo.nettyagent.garage.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationResponse;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import com.alibaba.dubbo.performance.demo.nettyagent.util.GetTraceString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;


/**
 * Created by gexinjie on 2018/5/29.
 */
public class CacheResponseDecoder extends ByteToMessageDecoder {
    private Logger logger = LoggerFactory.getLogger(CacheDecoder.class);

    protected static final short MAGIC = (short) 0xdacc;
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



    public CacheResponseDecoder(CacheContext cacheContext) {
        super();
        this.cacheContext = cacheContext;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        do {
            int savedReaderIndex = byteBuf.readerIndex();
            Object msg = null;
            try {
                msg = doDecode(byteBuf);
                assert  msg != null;
            } catch (IndexOutOfBoundsException e) {
                byteBuf.readerIndex(savedReaderIndex);
                boolean isCached = (byteBuf.getByte(2) & FLAG_CACHE) != 0,
                        isValid = (byteBuf.getByte(2) & FLAG_VALID) != 0,
                        isRequest = (byteBuf.getByte(2) & FLAG_REQUEST) != 0;
                final int dataLength = byteBuf.getInt(savedReaderIndex+DATA_LENGTH_INDEX);
                logger.error("informations:\n" +
                                "isRequest: {}\n" +
                                "isCached: {}\n" +
                                "isValid: {}\n" +
                                "dataLength(协议中的域): {}\n" +
                                "hexdump: {}\n" +
                                "IndexOutOfBoundsException: {}", isRequest,isCached, isValid, dataLength ,
                        ByteBufUtil.hexDump(byteBuf) , GetTraceString.get(e));
                byteBuf.clear();
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


    }
    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_INPUT, DECODE_ERROR
    }
    private Object doDecode(ByteBuf byteBuf){
        logger.info("received hexdump: {}", ByteBufUtil.hexDump(byteBuf));


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

        final int HeaderLength = isValid ? HEADER_LENGTH_MAX : HEADER_LENGTH_MIN;
        final int bodyLength = dataLength - HeaderLength;
        if (readable < dataLength) {
//            byteBuf.readerIndex(startIndex);
            return DecodeResult.NEED_MORE_INPUT;
        }

        byteBuf.readerIndex(startIndex + REQEUST_ID_INDEX);
        final long requestId = byteBuf.readLong();
        // 跳过 data length
        byteBuf.readInt();

        /* ************ response decode ************************
        这部分负责收到 response 的 decode 逻辑
         ******************************************************/
        InvocationResponse response = new InvocationResponse();
        response.setRequestID(requestId);
        // todo 目前认为方法词典缓存足够，所以从 PA 接受到的 response 都是 cache 过的
        assert isCache;
        if (isCache) {
            if (isValid) {
                int methodID = byteBuf.readInt();
                logger.info("first time in cached, method id is  {}", methodID);
                //当 Valid 有效时，说明 response 第一次缓存 method id
                // 在 invocation 的方法属性读取完毕后放入缓存

                String body = byteBuf.readCharSequence(bodyLength, Charset.forName("utf-8")).toString();
                String[] parts = body.split("\n");
                assert parts.length >= 4;
                if (parts.length >= 4) {
                    FuncType newType = new FuncType();
                    newType.setMethodName(parts[0]);
                    newType.setParameterTypes(parts[1]);
                    newType.setInterfaceName(parts[2]);
                    if (!cacheContext.contains(newType)) {
                        logger.info("add new cache method: {}", newType);
                    } else {
                        logger.warn("method is already in cache, before: {}, after: {}", cacheContext.get(methodID), newType);
                    }
                    response.setResult(parts[3]);
                    cacheContext.put(methodID, newType);
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
                response.setResult(parts[0]);
            }
        }
        logger.info("CA received response from PA: " + response);
        return response;


    }

}
