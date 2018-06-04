package test;

import com.alibaba.dubbo.performance.demo.nettyagent.DubboRpcDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.DubboRpcEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
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
                "null\n" +
                "\"hash\"\n" +
                "\"Ljava/lang/String;\"\n" +
                "\"hkhdkfhakjf\"\n" +
                "{\"path\":\"com.alibaba.dubbo.performance.demo.provider.IHelloService\"}\n");

        String dubborrr = "\"2.0.1\"\n" +
                "\"com.alibaba.dubbo.performance.demo.provider.IHelloService\"\n" +
                "null\n" +
                "\"hash\"\n" +
                "\"Ljava/lang/String;\"\n" +
                "\"mgtMrcbvL93R6vpa1c7CQ5vOiTR6RnXIIf39pITByp8lNcGONH7uddzDtvQrGLPpHwV73krS9TbE2vs7hFSgVAhj1bboZzi2fy0DzxUWNFQG9a8WwT3F03pMxXXNzSevp9rPi6wp8pQGF4naBYOhskgXSDXFC4GUXieiJMBWAmV7wqyqR0vj5obJPLA5LehSAzUb0RMGJXSwIemJZTgFGw2Rq0oXkAbLo1qHKlDBI6DgrPBaNae8PemzKthZhswvrtMsQh3w6SerLQ6exyFf9tEtG9pat3Bs17gqfh0QGE5SaTvw6n3OhAbmwjRwHQRMjS4RYk2OQ8FNW0owaNRL7sCi5oWL3dQYA3P4Mmrg70yGdSLGES5PDzJUpHd74Tg8nTgvbeDBMnNwtf5RjpjiNjxWX3DxgyRBO1WlH9N7Qmfx7RgiFo1EyUixBvW84SjSO9qGrfytylloemHfojw1RAmFAVvtxvDIizK6MIduRi8IJQnG9FazQjELmnGYI1JGYchBIa398jlEZ8ZvNrDekSAE4v52p8n3KPIvqJt8a2MQtx0jRowrP4D0e4oOI1pR1jra9DZJXTWh11b2z4c1RjmmCmJ2nYgKicY9QzYl6OLERELhtf6FrvclRC2qDUv9mNseT8cGurUId3R2gY3knlPdlVw5DQpBz3O5oRu92waS6VK67yBauCkMQN7hzN8PcL7IMAQ5wLSw6k9mHJ7BWWUe692rUpHpNcxfQ3xRbd9R\"\n" +
                "{\"path\":\"com.alibaba.dubbo.performance.demo.provider.IHelloService\"}\n";
//        assertTrue(dubboChannel.writeInbound(dubboRequest));
//        ByteBuf dubboBytes = dubboChannel.readInbound();
//        assertTrue(dubboBytes.isReadable());
//        assertTrue(dubboChannel.writeOutbound(dubboBytes));
//        Invocation dubboResponse = dubboChannel.readOutbound();
//        dubboResponse.setResult("1234");
    }

    @Test
    public void dubboNormalDecode() throws Exception {
        String dubboResponseHex = "dabb061400000000000000200000000d310a2d3131343331323830310a";
        // Invocation{methodName='null', parameterTypes='null', arguments='null', result='-114312801', requestID=32, methodID=-1}
        byte[] responseBytes = ByteBufUtil.decodeHexDump(dubboResponseHex);
        EmbeddedChannel dubboDecodeChannel = new EmbeddedChannel(
                new DubboRpcDecoder()
        );
        ByteBuf input = Unpooled.copiedBuffer(responseBytes);
        assertTrue(dubboDecodeChannel.writeInbound(input));
        Invocation response = dubboDecodeChannel.readInbound();
        assertEquals(response.getResult(), "-114312801");
        assertEquals(32, response.getRequestID());
    }

    @Test
    public void dubboNullDecode() throws Exception {
        String dubboResponseHex = "dabbe6000000000000000007000000056e756c6c0a";
        // Invocation{methodName='null', parameterTypes='null', arguments='null', result='-114312801', requestID=32, methodID=-1}
        byte[] responseBytes = ByteBufUtil.decodeHexDump(dubboResponseHex);
        EmbeddedChannel dubboDecodeChannel = new EmbeddedChannel(
                new DubboRpcDecoder()
        );
        ByteBuf input = Unpooled.copiedBuffer(responseBytes);
        assertFalse(dubboDecodeChannel.writeInbound(input));
//        Invocation response = dubboDecodeChannel.readInbound();
//        assertEquals(response.getResult(), "-114312801");
//        assertEquals(32, response.getRequestID());
    }
}