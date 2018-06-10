package com.alibaba.dubbo.performance.demo.nettyagent.garage;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.Bytes;
import io.netty.buffer.ByteBuf;
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
public class CacheEncoder0 extends MessageToByteEncoder{
    private final ConcurrentHashMap<Long, Integer> requestToMethodFirstCache;
    private final ConcurrentHashMap<FuncType, Integer> methodIDs;

    //    public static final String NAME = "cache";
    private Logger logger = LoggerFactory.getLogger(CacheEncoder0.class);
    protected static final short MAGIC = (short) 0xdacc;
    protected  static final int HEADER_LENGTH = 16;

    protected  static final int REQEUST_ID_INDEX = 4;
    protected  static final int DATA_LENGTH_INDEX = 12;





    protected static final byte FLAG_REQUEST = (byte) 0x80;


    public CacheEncoder0(ConcurrentHashMap<FuncType, Integer> methodIDsCache, ConcurrentHashMap<Long, Integer> requestToMethodFirstCache) {
        super();
        this.methodIDs = methodIDsCache;
        this.requestToMethodFirstCache = requestToMethodFirstCache;

    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        assert  this.methodIDs != null;
        Invocation invocation = (Invocation)msg;

        int startWriteIndex = out.writerIndex();

        boolean isRequest = invocation.getResult() == null;

        /* ************ request encode ************************
         这部分代码负责收到 request 的 encode 逻辑
          ******************************************************/
        if (isRequest) {
            byte[] header = new byte[HEADER_LENGTH];
            Bytes.short2bytes(MAGIC, header);
            header[2] |= FLAG_REQUEST;
            Bytes.long2bytes(invocation.getRequestID(), header, 4);

            // 写入 body, 先更改 bytebuf 的 index
            out.writerIndex(startWriteIndex + HEADER_LENGTH);
            out.writeCharSequence(invocation.getMethodName() + "\n", Charset.forName("utf-8"));
            out.writeCharSequence(invocation.getParameterTypes() + "\n", Charset.forName("utf-8"));
            out.writeCharSequence(invocation.getInterfaceName() + "\n", Charset.forName("utf-8"));

//            logger.info("sending request to PA: " + invocation.toString());
            out.writeCharSequence(invocation.getArguments() + "\n", Charset.forName("utf-8"));
            int totalIndex = out.writerIndex();
            Bytes.int2bytes(totalIndex-startWriteIndex, header, DATA_LENGTH_INDEX);
            out.writerIndex(startWriteIndex);
            out.writeBytes(header);
            out.writerIndex(totalIndex);
            return;
        }
        /* ************ response encode ************************
         这部分代码负责收到 response 的 encode 逻辑

         此处的 invocation 要看成 dubbo 返回的一个 response，所以invocation不包含方法信息
          ******************************************************/
        long requestID = invocation.getRequestID();

        //headers
        byte[] header = new byte[HEADER_LENGTH];
        Bytes.short2bytes(MAGIC, header);
        if (invocation.getRequestID() == -1) {
            logger.error("request ID should not be -1: {}", invocation);
        }
        Bytes.long2bytes(invocation.getRequestID(), header, REQEUST_ID_INDEX);
        out.writerIndex(startWriteIndex + HEADER_LENGTH);
        out.writeCharSequence(invocation.getResult()+"\n", Charset.forName("utf-8"));
        int totalIndex = out.writerIndex();
        Bytes.int2bytes(totalIndex-startWriteIndex, header, DATA_LENGTH_INDEX);
        out.writerIndex(startWriteIndex);
        out.writeBytes(header);
        out.writerIndex(totalIndex);

    }

}
