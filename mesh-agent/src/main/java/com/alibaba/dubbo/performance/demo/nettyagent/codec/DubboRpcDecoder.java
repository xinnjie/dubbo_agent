package com.alibaba.dubbo.performance.demo.nettyagent.codec;

import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationResponse;
import com.alibaba.dubbo.performance.demo.nettyagent.util.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;


import java.util.Arrays;
import java.util.List;

public class DubboRpcDecoder extends ByteToMessageDecoder {

    protected static final short MAGIC = (short) 0xdabb;

    // header length.
    protected static final int HEADER_LENGTH = 16;
    protected static final int STATUS_INDEX = 3;

    protected static final byte FLAG_EVENT = (byte) 0x20;


    protected  static final int REQEUST_ID_INDEX = 4;
    protected  static final int DATA_LENGTH_INDEX = 12;
org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);


    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        try {
            do {
                int savedReaderIndex = byteBuf.readerIndex();
                Object msg = null;
                try {
                    msg = doDecode(byteBuf);
                } catch (Exception e) {
                    throw e;
                }
                // 如果还没有传输完成，将 bytebuf 恢复原状
                if (msg == DecodeResult.NEED_MORE_INPUT) {
                    byteBuf.readerIndex(savedReaderIndex);
                    break;
                } else if (msg == DecodeResult.SKIP_INPUT) {
                    byteBuf.readerIndex(savedReaderIndex);
                    logger.error("error debbo response, hexdump :{}", ByteBufUtil.hexDump(byteBuf));
                    byteBuf.clear();
                }
                else {
                    list.add(msg);
                }
            } while (byteBuf.isReadable());
        } finally {
            if (byteBuf.isReadable()) {
                byteBuf.discardReadBytes();
            }
        }


        //list.add(doDecode(byteBuf));
    }

    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_INPUT, DECODE_ERROR
    }

    /**
     * Demo为简单起见，直接从特定字节位开始读取了的返回值，demo未做：
     * 1. 请求头判断
     * 2. 返回值类型判断
     *
     * @param byteBuf
     * @return
     */
    private Object doDecode(ByteBuf byteBuf){

        int savedReaderIndex = byteBuf.readerIndex();
        int readable = byteBuf.readableBytes();

        if (readable < HEADER_LENGTH) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        short magic = byteBuf.readShort();
        if (magic != MAGIC) {
            logger.error("not dubbo MAGIC");
            return DecodeResult.SKIP_INPUT;
        }
        byteBuf.readByte();
        byte status = byteBuf.readByte();
        byteBuf.readerIndex(savedReaderIndex + REQEUST_ID_INDEX);
        long requestID = byteBuf.readLong();
        int bodyLength = byteBuf.readInt();
        int totalLength = bodyLength + HEADER_LENGTH;
        if (readable < totalLength) {
            return DecodeResult.NEED_MORE_INPUT;
        }
//        logger.debug("hexdumping dubbo response: {} ",  ByteBufUtil.hexDump(byteBuf));
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

        //byte[] data = new byte[byteBuf.readableBytes()];
        //byteBuf.readBytes(data);

        // HEADER_LENGTH + 1，忽略header & Response value type的读取，直接读取实际Return value
        // dubbo返回的body中，前后各有一个换行，去掉
        /*
         q: 前面去掉一个换行为什么要 +2， 而不是 +1
         a: 还要额外去掉一个代表返回值类型的字节， RESPONSE_NULL_VALUE - 2, RESPONSE_VALUE - 1, RESPONSE_WITH_EXCEPTION - 0
        */

        ByteBuf result = byteBuf.retainedSlice(byteBuf.readerIndex() + 2, bodyLength-2);
        byteBuf.readerIndex(savedReaderIndex + totalLength);
        InvocationResponse response = new InvocationResponse(result);
        response.setRequestID(requestID);
        logger.debug("PA received response from provider: {}", response);
        if (status == 20) {
            return response;
        }
        logger.error("dubbo response received {} , error message from dubbo: {}",  getStatusMessage(status), response.getResult());
        response.setResult(null);
        return response;
    }

    private String getStatusMessage(byte status) {
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
        return statusMessage;
    }
}
