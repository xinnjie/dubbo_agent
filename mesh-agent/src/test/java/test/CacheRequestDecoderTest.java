package test;

import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheRequestDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by gexinjie on 2018/6/11.
 */
public class CacheRequestDecoderTest {
    final String argumentStr = "lsx7sktGyG586Wte";

    private InvocationRequest request;
    private CacheContext emptyCache;
    private ByteBuf notCachedRequestBuf;
    private CacheContext oneCache;
    private ConcurrentHashMap<Long, Integer> oneRequestToMethodFirstCache;
    private ConcurrentHashMap<Long, Integer> emptyRequestToMethodFirstCache;
    private FuncType type;

    @Before
    public void setUp() throws Exception {
        request = new InvocationRequest();
        type = new FuncType();
        type.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        type.setParameterTypes("Ljava/lang/String;");
        type.setMethodName("hash");
        request.setRequestID(335);
        ByteBuf argument = Unpooled.wrappedBuffer(argumentStr.getBytes());
        request.setArgument(argument);
        request.setFuncType(type);
        emptyCache = new CacheContext();
        emptyRequestToMethodFirstCache = new ConcurrentHashMap<Long, Integer> ();



        oneCache = new CacheContext();
        oneCache.put(type, 23);
        oneRequestToMethodFirstCache = new ConcurrentHashMap<Long, Integer> ();
        oneRequestToMethodFirstCache.put(request.getRequestID(), 23);


        String hexdump = "dacc8000000000000000014f00000072686173680a4c6a6176612f6c616e672f537472696e673b0a636f6d2e616c69626162612e647562626f2e706572666f726d616e63652e64656d6f2e70726f76696465722e4948656c6c6f536572766963650a6c737837736b74477947353836577465";
        byte[] requestByte = ByteBufUtil.decodeHexDump(hexdump);
        notCachedRequestBuf = Unpooled.wrappedBuffer(requestByte);
    }

    @Test
    public void decode() throws Exception {

        EmbeddedChannel PADecodeChannel = new EmbeddedChannel(
                new CacheRequestDecoder(emptyCache, emptyRequestToMethodFirstCache)
        );

        assertTrue(PADecodeChannel.writeInbound(notCachedRequestBuf));
        InvocationRequest result = PADecodeChannel.readInbound();
        assertEquals(type, result.getFuncType());
        ByteBuf argument = result.getArgument();
        assertEquals(argumentStr, argument.readCharSequence(argument.readableBytes(), Charset.forName("utf-8")));

    }

}