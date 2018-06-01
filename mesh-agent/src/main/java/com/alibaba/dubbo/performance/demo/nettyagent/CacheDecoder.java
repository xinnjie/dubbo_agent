package com.alibaba.dubbo.performance.demo.nettyagent;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;


/**
 * Created by gexinjie on 2018/5/29.
 */
public class CacheDecoder extends ByteToMessageDecoder{
//    public static final String NAME = "cache";
    protected static final short MAGIC = (short) 0xdacc;
    private HashMap<Integer, FuncType> methods = null;
    protected  static final int HEADER_LENGTH_MIN = 16;
    protected  static final int HEADER_LENGTH_MAX = 20;

    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_CACHE = (byte) 0x40;
    protected static final byte FLAG_VALID = (byte) 0x20;
    protected static final byte FLAG_NULL = (byte) 0x10;

    protected  static final int REQEUST_ID_INDEX = 4;
    protected  static final int DATA_LENGTH_INDEX = 12;
    protected  static final int METHOD_ID_INDEX = 16;


    public CacheDecoder(HashMap<Integer, FuncType> methodsCache) {
        this.methods = methodsCache;
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
                    //todo msg
                    assert  msg != null;
                } catch (Exception e) {
                    throw e;
                }
                // 如果还没有传输完成，将 bytebuf 恢复原状
                if (msg == DecodeResult.NEED_MORE_INPUT) {
                    byteBuf.readerIndex(savedReaderIndex);
                    break;
                }

                list.add(msg);
            } while (byteBuf.isReadable());
        } finally {
            if (byteBuf.isReadable()) {
                byteBuf.discardReadBytes();
            }
        }


    }
    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_INPUT
    }
    private Object doDecode(ByteBuf byteBuf){

        final int startIndex = byteBuf.readerIndex();
        int readable = byteBuf.readableBytes();

        if (readable < HEADER_LENGTH_MIN) {
            return DecodeResult.NEED_MORE_INPUT;
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
        int totalLength = dataLength + HeaderLength;
        if (readable < totalLength) {
//            byteBuf.readerIndex(startIndex);
            return DecodeResult.NEED_MORE_INPUT;
        }
        byteBuf.readerIndex(REQEUST_ID_INDEX);
        final long requestId = byteBuf.readLong();
        // 跳过 data length
        byteBuf.readInt();

        // 收到 response
        if (!isRequest) {
            Invocation invocation = new Invocation();
            invocation.setRequestID(requestId);
            // todo 目前认为方法词典缓存足够，所以从 PA 接受到的 response 都是 cache 过的
            assert isCache;
            if (isCache) {
                invocation.setMethodID(byteBuf.readInt());;
                if (isValid) {
                    //todo 当 Valid 有效时，说明 response 第一次第一次缓存 method id
                    assert !methods.containsKey(invocation.getMethodID());
                    // 在 invocation 的方法属性读取完毕后放入缓存

                    String body = byteBuf.readCharSequence(dataLength, Charset.forName("utf-8")).toString();
                    String[] parts = body.split("\n");
                    assert parts.length >= 3;
                    invocation.setMethodName(parts[0]);
                    invocation.setParameterTypes(parts[1]);
                    invocation.setResult(parts[2]);
                    synchronized (methods) {
                        methods.put(invocation.getMethodID(), invocation.shallowCopy());
                    }
                }else {
                    String body = byteBuf.readCharSequence(dataLength, Charset.forName("utf-8")).toString();
                    String[] parts = body.split("\n");
                    assert parts.length >= 1;
                    invocation.setResult(parts[0]);
                }
                return invocation;
            }

        }
        // 收到 request
        else {
            Invocation invocation = new Invocation();
            if (isCache) {
                invocation.setMethodID(byteBuf.readInt());
                methods.get(invocation.getMethodID()).shallowCopyInPlace(invocation);
                invocation.setRequestID(requestId);
                // todo 现在假设只有一个参数，以后改进
                String body = byteBuf.readCharSequence(dataLength, Charset.forName("utf-8")).toString();
                invocation.setArguments(body.trim());
                return invocation;
            }
            // 假如没有 cache 过，将现在这个 method 进行 cache
            else {
                invocation = new Invocation();
                /*
                 TODO 忽略 attachment
                 todo interface name??
                 attachment 包括
                      * Dubbo version
                      * Service name
                      * Service version
                 */
                String body = byteBuf.readCharSequence(dataLength, Charset.forName("utf-8")).toString();
                String[] parts = body.split("\n");
                assert parts.length >= 2;
                invocation.setMethodName(parts[0]);
                invocation.setParameterTypes(parts[1]);
                return invocation;
            }
        }
        return null;
    }

}
