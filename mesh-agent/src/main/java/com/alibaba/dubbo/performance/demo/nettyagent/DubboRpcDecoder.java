package com.alibaba.dubbo.performance.demo.nettyagent;

import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class DubboRpcDecoder extends ByteToMessageDecoder {
    // header length.
    protected static final int HEADER_LENGTH = 16;
    protected static final int STATUS_INDEX = 3;

    protected static final byte FLAG_EVENT = (byte) 0x20;
    private Logger logger = LoggerFactory.getLogger(DubboRpcEncoder.class);


    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {

        try {
            do {
                int savedReaderIndex = byteBuf.readerIndex();
                Object msg = null;
                try {
                    msg = decode2(byteBuf);
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


        //list.add(decode2(byteBuf));
    }

    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_INPUT
    }

    /**
     * Demo为简单起见，直接从特定字节位开始读取了的返回值，demo未做：
     * 1. 请求头判断
     * 2. 返回值类型判断
     *
     * @param byteBuf
     * @return
     */
    private Object decode2(ByteBuf byteBuf){

        int savedReaderIndex = byteBuf.readerIndex();
        int readable = byteBuf.readableBytes();

        if (readable < HEADER_LENGTH) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        byte[] header = new byte[HEADER_LENGTH];
        byteBuf.readBytes(header);
        byte[] dataLen = Arrays.copyOfRange(header,12,16);
        int len = Bytes.bytes2int(dataLen);
        int totalLength = len + HEADER_LENGTH;
        if (readable < totalLength) {
            return DecodeResult.NEED_MORE_INPUT;
        }
        byteBuf.readerIndex(savedReaderIndex);
        byte[] data = new byte[totalLength];
        byteBuf.readBytes(data);

        //status
        /*
          20 - OK
          30 - CLIENT_TIMEOUT
          31 - SERVER_TIMEOUT
          40 - BAD_REQUEST
          50 - BAD_RESPONSE
          60 - SERVICE_NOT_FOUND
          70 - SERVICE_ERROR
          80 - SERVER_ERROR
          90 - CLIENT_ERROR
          100 - SERVER_THREADPOOL_EXHAUSTED_ERROR
         */
        byte status = data[STATUS_INDEX];
        String statusMessage;
        switch (status) {
            case 20: {
                statusMessage = "Ok";
                break;
            }
            case 30: {
                statusMessage = "CLIENT_TIMEOUT";
                break;
            }
            case 31: {
                statusMessage = "SERVER_TIMEOUT";
                break;
            }
            case 40: {
                statusMessage = "BAD_REQUEST";
                break;
            }
            case 50: {
                statusMessage = "BAD_RESPONSE";
                break;
            }
            case 60: {
                statusMessage = "SERVICE_NOT_FOUND";
                break;
            }
            case 70: {
                statusMessage = "SERVICE_ERROR";
                break;
            }
            case 80: {
                statusMessage = "SERVER_ERROR";
                break;
            }
            case 90: {
                statusMessage = "CLIENT_ERROR";
                break;
            }
            case 100: {
                statusMessage = "SERVER_THREADPOOL_EXHAUSTED_ERROR";
                break;
            }
            default:
                statusMessage = "UNKNOWN STATUS";
        }

        logger.info("dubbo response status message: " + statusMessage);
        //byte[] data = new byte[byteBuf.readableBytes()];
        //byteBuf.readBytes(data);

        // HEADER_LENGTH + 1，忽略header & Response value type的读取，直接读取实际Return value
        // dubbo返回的body中，前后各有一个换行，去掉
        /*
         q: 前面去掉一个换行为什么要 +2， 而不是 +1
         a: 还要额外去掉一个代表返回值类型的字节， RESPONSE_NULL_VALUE - 2, RESPONSE_VALUE - 1, RESPONSE_WITH_EXCEPTION - 0
        */
        // todo 记得改回来
//        byte[] subArray = Arrays.copyOfRange(data,HEADER_LENGTH + 2, data.length -1 );
        byte[] subArray = Arrays.copyOfRange(data,HEADER_LENGTH, data.length -1 );

        logger.info("receive dubbo protocal body {" + new String(subArray) + "}");

        byte[] requestIdBytes = Arrays.copyOfRange(data,4,12);
        long requestId = Bytes.bytes2long(requestIdBytes,0);

        Invocation invocation = new Invocation();
        invocation.setRequestID(requestId);
        invocation.setResult(new String(subArray));

        logger.info("received response from provider: " + invocation.toString());
        return invocation;
    }
}
