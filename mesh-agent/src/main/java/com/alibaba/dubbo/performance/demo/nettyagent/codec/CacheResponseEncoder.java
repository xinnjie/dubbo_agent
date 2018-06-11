package com.alibaba.dubbo.performance.demo.nettyagent.codec;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationResponse;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gexinjie on 2018/5/30.
 */
public class CacheResponseEncoder extends MessageToByteEncoder {
    private final ConcurrentHashMap<Long, Integer> requestToMethodFirstCache;
    private final CacheContext cacheContext;


    private Logger logger = LoggerFactory.getLogger(CacheResponseEncoder.class);
    protected  static final short MAGIC = (short) 0xdacc;
    protected  static final int HEADER_LENGTH_MIN = 16;
    protected  static final int HEADER_LENGTH_MAX = 20;

    protected  static final int REQEUST_ID_INDEX = 4;
    protected  static final int DATA_LENGTH_INDEX = 12;
    protected  static final int METHOD_ID_INDEX = 16;





    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_CACHE = (byte) 0x40;
    protected static final byte FLAG_VALID = (byte) 0x20;
    protected static final byte FLAG_NULL = (byte) 0x10;


    public CacheResponseEncoder(CacheContext cacheContext, ConcurrentHashMap<Long, Integer> requestToMethodFirstCache) {
        super();
        this.cacheContext = cacheContext;
        this.requestToMethodFirstCache = requestToMethodFirstCache;

    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        InvocationResponse response = (InvocationResponse)msg;

        int startWriteIndex = out.writerIndex();

        boolean isCache = true;

        /* ************ response encode ************************
         这部分代码负责收到 response 的 encode 逻辑

         此处的 invocation 要看成 dubbo 返回的一个 response，所以invocation不包含方法信息
          ******************************************************/
        Boolean isValid = false;
        long requestID = response.getRequestID();
        /*
        如果 encode 的 response 方法类型是第一次缓存的，需要将方法信息传回给 CA（告诉 CA 这个方法被缓存了）
        如何知道是第一次缓存：decode request 是会将第一次遇到的方法做缓存，把对应的 requestID 记录下来。encode 时通过比对 requestID 可以知道方法是否刚被缓存过
         */
        int cachedMethodID = -1;
        if (this.requestToMethodFirstCache.containsKey(requestID)) {
            isValid = true;
            cachedMethodID = this.requestToMethodFirstCache.get(requestID);
            this.requestToMethodFirstCache.remove(requestID);

            // 从 dubbo 的 request ID 找到对应的 methodID，用 methodID 找到对应的 funcType 信息
            response.setFuncType(cacheContext.get(cachedMethodID));
            if (response.getFuncType() == null) {
                logger.error("current methodID {} not in cache table {}", cachedMethodID, cacheContext.getMethodIDs());
            }
            logger.info("sending cached functype to CA: {}", response.getFuncType());
        }


        out.writeShort(MAGIC);
        assert response.getRequestID() != -1;
        if (response.getRequestID() == -1) {
            logger.error("request ID should not be -1: {}", response);
        }

        byte flag = 0;
        if (isCache) {
            flag |= FLAG_CACHE;
        }
        if (isValid) {
            flag |= FLAG_VALID;
        }
        out.writeByte(flag);
        out.writeByte(0);
        out.writeLong(response.getRequestID());
        out.writerIndex(startWriteIndex + METHOD_ID_INDEX);

        if (isValid) {
            out.writeInt(cachedMethodID);
            FuncType funcType = response.getFuncType();
            out.writeCharSequence(funcType.getMethodName()+ "\n", Charset.forName("utf-8"));
            out.writeCharSequence(funcType.getParameterTypes()+ "\n", Charset.forName("utf-8"));
            out.writeCharSequence(funcType.getInterfaceName()+ "\n", Charset.forName("utf-8"));

        }
        out.writeCharSequence(response.getResult()+"\n", Charset.forName("utf-8"));
        int totalIndex = out.writerIndex();
        out.writerIndex(startWriteIndex + DATA_LENGTH_INDEX);
        out.writeInt(totalIndex-startWriteIndex);
        out.writerIndex(totalIndex);
        logger.info("sending response to CA: {} , hexdump: {}" , response, ByteBufUtil.hexDump(out));

    }


}
