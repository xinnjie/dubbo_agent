package test;

import com.alibaba.dubbo.performance.demo.nettyagent.codec.DubboRpcEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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


    private InvocationRequest request = new InvocationRequest();
    private FuncType funcType = new FuncType();
//    private Invocation dubboRequest = new Invocation();
//    private Invocation dubboReponse = new Invocation();
    long requestID = 5;
    String arguments = "hkhdkfhakjf";
    int methodID = 23;
    byte[] responseHeader = new byte[16];
    private EmbeddedChannel dubboEncodeChannel;

    @Before
    public void setUp() throws Exception {

        funcType.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        funcType.setParameterTypes("Ljava/lang/String;");
        request.setRequestID(requestID);
        funcType.setMethodName("hash");
        request.setArgument(Unpooled.wrappedBuffer(arguments.getBytes()));
        request.setFuncType(funcType);

        dubboEncodeChannel = new EmbeddedChannel(
                new DubboRpcEncoder()
        );
    }

    @Test
    public void dubboEncode() throws Exception {

        assertTrue(dubboEncodeChannel.writeOutbound(request));
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
        assertEquals("\"2.0.1\"\n" +
                "\"com.alibaba.dubbo.performance.demo.provider.IHelloService\"\n" +
                "null\n" +
                "\"hash\"\n" +
                "\"Ljava/lang/String;\"\n" +
                "\"hkhdkfhakjf\"\n" +
                "{\"path\":\"com.alibaba.dubbo.performance.demo.provider.IHelloService\"}\n", new String(bodyBytes));
    }

//    @Test
//    public void dubboNormalDecode() throws Exception {
//        String dubboResponseHex = "dabb061400000000000000200000000d310a2d3131343331323830310a";
//        // Invocation{methodName='null', parameterTypes='null', arguments='null', result='-114312801', requestID=32, methodID=-1}
//        byte[] responseBytes = ByteBufUtil.decodeHexDump(dubboResponseHex);
//        EmbeddedChannel dubboDecodeChannel = new EmbeddedChannel(
//                new DubboRpcDecoder()
//        );
//        ByteBuf input = Unpooled.copiedBuffer(responseBytes);
//        assertTrue(dubboDecodeChannel.writeInbound(input));
//        Invocation response = dubboDecodeChannel.readInbound();
//        assertEquals(response.getResult(), "-114312801");
//        assertEquals(32, response.getRequestID());
//    }
//
//    @Test
//    public void dubboNullDecode() throws Exception {
//        String dubboResponseHex = "dabbe6000000000000000007000000056e756c6c0a";
//        // Invocation{methodName='null', parameterTypes='null', arguments='null', result='-114312801', requestID=32, methodID=-1}
//        byte[] responseBytes = ByteBufUtil.decodeHexDump(dubboResponseHex);
//        EmbeddedChannel dubboDecodeChannel = new EmbeddedChannel(
//                new DubboRpcDecoder()
//        );
//        ByteBuf input = Unpooled.copiedBuffer(responseBytes);
//        assertFalse(dubboDecodeChannel.writeInbound(input));
//    }
}