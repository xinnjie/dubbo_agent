package test;

import com.alibaba.dubbo.performance.demo.nettyagent.codec.DubboRpcDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.codec.DubboRpcEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
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
    String arguments = "hk2313123hdkfhakjf";
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
        request.setMethodID(methodID);

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
                "\""+ arguments +"\"\n" +
                "{\"path\":\"com.alibaba.dubbo.performance.demo.provider.IHelloService\"}\n", new String(bodyBytes));
    }

//    @Test
//    public void dubboNormalDecode() throws Exception {
//        String dubboResponseHex = "dabb0664000000000009426c0000015d2253657276657220736964652831302e31302e31302e352c32303838302920746872656164706f6f6c20697320657868617573746564202c64657461696c206d73673a54687265616420706f6f6c206973204558484155535445442120546872656164204e616d653a20447562626f53657276657248616e646c65722d31302e31302e31302e353a32303838302c20506f6f6c2053697a653a2032303020286163746976653a203139352c20636f72653a203230302c206d61783a203230302c206c6172676573743a20323030292c205461736b3a203234323731352028636f6d706c657465643a20323432353139292c204578656375746f72207374617475733a28697353687574646f776e3a66616c73652c2069735465726d696e617465643a66616c73652c2069735465726d696e6174696e673a66616c7365292c20696e20647562626f3a2f2f31302e31302e31302e353a323038383021220a";
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