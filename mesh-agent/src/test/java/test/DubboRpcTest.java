package test;

import com.alibaba.dubbo.performance.demo.nettyagent.DubboRpcDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.DubboRpcEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/3.
 */
public class DubboRpcTest {
    //copy from DubboRpcEncoder,Decoder
    // header length.
    protected static final int HEADER_LENGTH = 16;
    // magic header.
    protected static final short MAGIC = (short) 0xdabb;
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;


    private Invocation invocation = new Invocation();
    private Invocation dubboRequest = new Invocation();
    private Invocation dubboReponse = new Invocation();
    long requestID = 5;
    String arguments = "hkhdkfhakjf";
    byte[] responseHeader = new byte[16];
    @Before
    public void setUp() throws Exception {
        invocation.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setParameterTypes("Ljava/lang/String;");
        invocation.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setRequestID(requestID);
        invocation.setMethodName("hash");
        invocation.setMethodID(10);
        invocation.setResult("hello");
        invocation.setArguments(arguments);


        invocation.shallowCopyInPlace(dubboRequest);
        dubboRequest.setRequestID(requestID);
        dubboRequest.setArguments(arguments);


        dubboReponse.setRequestID(requestID);
        dubboReponse.setResult("100");

    }

    @Test
    public void dubboEncode() throws Exception {
        EmbeddedChannel dubboEncodeChannel = new EmbeddedChannel(
                new DubboRpcEncoder()
        );
        assertTrue(dubboEncodeChannel.writeOutbound(dubboRequest));
        ByteBuf requestByte = dubboEncodeChannel.readOutbound();
        int startIndex = requestByte.readerIndex();

        ByteBuf headerByte = requestByte.readBytes(16);
        // 检查 request flag 位
        assertTrue((headerByte.getByte(2) & FLAG_REQUEST) != 0);
        requestByte.readerIndex(startIndex + 4);
        //检查 requestID
        assertEquals(requestByte.readLong(), requestID);
        requestByte.readerIndex(16);
        byte[] bodyBytes = new byte[requestByte.readableBytes()];
        requestByte.readBytes(bodyBytes);
        assertEquals(new String(bodyBytes), "\"2.0.1\"\n" +
                "\"com.alibaba.dubbo.performance.demo.provider.IHelloService\"\n" +
                "\"2.0.0\"\n" +
                "\"hash\"\n" +
                "\"Ljava/lang/String;\"\n" +
                "\"hkhdkfhakjf\"\n" +
                "{\"path\":\"com.alibaba.dubbo.performance.demo.provider.IHelloService\"}\n");

//        assertTrue(dubboChannel.writeInbound(dubboRequest));
//        ByteBuf dubboBytes = dubboChannel.readInbound();
//        assertTrue(dubboBytes.isReadable());
//        assertTrue(dubboChannel.writeOutbound(dubboBytes));
//        Invocation dubboResponse = dubboChannel.readOutbound();
//        dubboResponse.setResult("1234");
    }

    @Test
    public void dubboDecode() throws Exception {
        EmbeddedChannel dubboDecodeChannel = new EmbeddedChannel(
                new DubboRpcDecoder()
        );

    }
}