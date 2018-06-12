package test;

import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheRequestDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheResponseDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/12.
 */
public class CacheResponseDecoderTest {
    @Test
    public void decode() throws Exception {
        String hexdump = "dacc400000000000000000270000001b323031393437323137390adacc400000000000000000250000001c2d323133363132383137350adacc400000000000000000260000001c2d323134313432333936360adacc400000000000000000240000001b2d3631323237373232320a";

        byte[] requestByte = ByteBufUtil.decodeHexDump(hexdump);
        ByteBuf requestBuf = Unpooled.wrappedBuffer(requestByte);

        FuncType type = new FuncType();
        type.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        type.setParameterTypes("Ljava/lang/String;");
        type.setMethodName("hash");
        CacheContext cacheContext = new CacheContext();
        cacheContext.put(type, 23);


        EmbeddedChannel PADecodeChannel = new EmbeddedChannel(
                new CacheResponseDecoder(cacheContext)
        );

        PADecodeChannel.writeInbound(requestBuf);
    }

}