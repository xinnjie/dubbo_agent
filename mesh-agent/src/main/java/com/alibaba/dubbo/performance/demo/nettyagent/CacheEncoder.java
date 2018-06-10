package com.alibaba.dubbo.performance.demo.nettyagent;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.Bytes;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gexinjie on 2018/5/30.
 */
public class CacheEncoder extends MessageToByteEncoder{
    private final ConcurrentHashMap<Long, Integer> requestToMethodFirstCache;
    private final CacheContext cacheContext;


    //    public static final String NAME = "cache";
    private Logger logger = LoggerFactory.getLogger(CacheEncoder.class);
    protected static final short MAGIC = (short) 0xdacc;
    protected  static final int HEADER_LENGTH_MIN = 16;
    protected  static final int HEADER_LENGTH_MAX = 20;

    protected  static final int REQEUST_ID_INDEX = 4;
    protected  static final int DATA_LENGTH_INDEX = 12;
    protected  static final int METHOD_ID_INDEX = 16;





    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_CACHE = (byte) 0x40;
    protected static final byte FLAG_VALID = (byte) 0x20;
    protected static final byte FLAG_NULL = (byte) 0x10;



    /*
    Q： 为什么不让 methodIDsCache 作为 CacheEncoder 的类变量(static)，而是选择将 methodIDsCache 从外部（其实是 Agent 构建时）传过来？
    A： 所有权的问题。关于这些方法的缓存的所有权更适合交给 Agent。这些 Cache 的声明周期是和 Agent 一致的，Agent 关闭 cache 也就要被清理了。
        考虑一个特别的情况：如果我关闭一个 Agent ，又打开一个 Agent，原来的 Cache 还能不能用呢？ 。。。好像还真能
        另一种情况：一台机器上同时运行两个 Agent（同一个进程），对于两台 Consumer Agent 或者两台 Provider Agent，情况还好点。但是最好额情况还是让它们互相隔离
                  如果是一台 Consumer Agent 和一台 Provider Agent，它们之间的 Cache 就会乱套的。

             我承认例子不是很好：只要保证一个进程只运行一个 Agent，Cache 作为类变量(static)也可以，而且使用还简便
             但是直观上我觉得 Cache 所属权更适合交给 Agent。
     */

    /**
     *
     * @param cacheContext
     * @param requestToMethodFirstCache  **只在 encode response 时会被用到**   需要对应的encoder decoder 共享使用
     *                                  这个 map 将一个 requestID 映射到 methodID.
     *                                  当encode 的 response 的方法信息第一次被缓存时，response encoder 可以通过 requestID 得到对应的被缓存的 methodID
     *                                  并将这个 methodID 发给 CA，告诉它这个方法被缓存了。
     */
    public CacheEncoder(CacheContext cacheContext, ConcurrentHashMap<Long, Integer> requestToMethodFirstCache) {
        super();
        this.cacheContext = cacheContext;
        this.requestToMethodFirstCache = requestToMethodFirstCache;

    }

//    private  void checkValidRequestEncode(int startIndex, ByteBuf buf) {
//        int endIndex = buf.readerIndex();
//        buf.readerIndex(startIndex);
//        ByteBuf slice = buf.slice();
//        buf.readerIndex(endIndex);
//
//
//
//    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        Invocation invocation = (Invocation)msg;

        int startWriteIndex = out.writerIndex();

        boolean isRequest = invocation.getResult() == null,
            isCache = false;
        int HeaderLength = 0;

        /* ************ request encode ************************
         这部分代码负责收到 request 的 encode 逻辑
          ******************************************************/
        if (isRequest) {
            if (cacheContext.contains(invocation)) {
                isCache = true;
            }
            HeaderLength = isCache ? HEADER_LENGTH_MAX : HEADER_LENGTH_MIN;

            byte[] header = new byte[HeaderLength];
            Bytes.short2bytes(MAGIC, header);
            header[2] |= FLAG_REQUEST;
            // set request id
            // request 放需要分配一个 reqeustID 给每次 encode
            // 启动方面只要使用这个 ID，不能再分配
            assert invocation.getRequestID() != -1;
            Bytes.long2bytes(invocation.getRequestID(), header, REQEUST_ID_INDEX);

            out.writerIndex(startWriteIndex + HeaderLength);
            if (isCache) {
                header[2] |= FLAG_CACHE;
                Integer methodID = cacheContext.get(invocation);
                if (methodID == null) {
                    logger.error("request is not cached {}, cuurent cache table is {}", invocation, cacheContext.getMethodIDs());
                } else {
                    logger.info("the method of this request is cached");
                }
                Bytes.int2bytes(methodID, header, METHOD_ID_INDEX);
            } else {
                // 写入 body, 先更改 bytebuf 的 index
                out.writeCharSequence(invocation.getMethodName() + "\n", Charset.forName("utf-8"));
                out.writeCharSequence(invocation.getParameterTypes() + "\n", Charset.forName("utf-8"));
                out.writeCharSequence(invocation.getInterfaceName() + "\n", Charset.forName("utf-8"));

            }
            out.writeCharSequence(invocation.getArguments() + "\n", Charset.forName("utf-8"));
            int totalIndex = out.writerIndex();
            Bytes.int2bytes(totalIndex-startWriteIndex, header, DATA_LENGTH_INDEX);
            out.writerIndex(startWriteIndex);
            out.writeBytes(header);
            out.writerIndex(totalIndex);
            logger.info("sending request to PA: {}, hexdump: {} , current methodsID cache: {}", invocation, ByteBufUtil.hexDump(out), cacheContext.getMethodIDs());

            return;
        }
        /* ************ response encode ************************
         这部分代码负责收到 response 的 encode 逻辑

         此处的 invocation 要看成 dubbo 返回的一个 response，所以invocation不包含方法信息
          ******************************************************/
        Boolean isValid = false;
        long requestID = invocation.getRequestID();
        /*
        如果 encode 的 response 方法类型是第一次缓存的，需要将方法信息传回给 CA（告诉 CA 这个方法被缓存了）
        如何知道是第一次缓存：decode request 是会将第一次遇到的方法做缓存，把对应的 requestID 记录下来。encode 时通过比对 requestID 可以知道方法是否刚被缓存过
         */
        if (this.requestToMethodFirstCache.containsKey(requestID)) {
            isValid = true;
            int cachedMethodID = this.requestToMethodFirstCache.get(requestID);
            invocation.setMethodID(cachedMethodID);
            this.requestToMethodFirstCache.remove(requestID);

            // 从 dubbo 的 request ID 找到对应的 methodID，用 methodID 找到对应的 funcType 信息
            FuncType functype = cacheContext.get(cachedMethodID);
            if (functype == null) {
                logger.error("current methodID {} not in cache table {}", cachedMethodID, cacheContext.getMethodIDs());
            }
            functype.shallowCopyInPlace(invocation);
            logger.info("sending cached functype to CA: {}", invocation);
        }
        isCache = true;

        //headers
        HeaderLength = HEADER_LENGTH_MAX;
        byte[] header = new byte[HeaderLength];
        Bytes.short2bytes(MAGIC, header);
        //todo remove assert
//        assert invocation.getRequestID() != -1;
        if (invocation.getRequestID() == -1) {
            logger.error("request ID should not be -1: {}", invocation);
        }
        Bytes.long2bytes(invocation.getRequestID(), header, REQEUST_ID_INDEX);
        if (isCache) {
            header[2] |= FLAG_CACHE;
            Bytes.int2bytes(invocation.getMethodID(), header, METHOD_ID_INDEX);
        }
        out.writerIndex(startWriteIndex + HeaderLength);
        if (isValid) {
            header[2] |= FLAG_VALID;
            out.writeCharSequence(invocation.getMethodName()+ "\n", Charset.forName("utf-8"));
            out.writeCharSequence(invocation.getParameterTypes()+ "\n", Charset.forName("utf-8"));
            out.writeCharSequence(invocation.getInterfaceName()+ "\n", Charset.forName("utf-8"));

        }
        out.writeCharSequence(invocation.getResult()+"\n", Charset.forName("utf-8"));
        int totalIndex = out.writerIndex();
        Bytes.int2bytes(totalIndex-startWriteIndex, header, DATA_LENGTH_INDEX);
        out.writerIndex(startWriteIndex);
        out.writeBytes(header);
        out.writerIndex(totalIndex);
        logger.info("sending response to CA: {} , hexdump: {}" , invocation, ByteBufUtil.hexDump(out));

    }


}
