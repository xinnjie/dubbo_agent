package test;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/6.
 */
public class CacheDecoderTest {
    @Test
    public void decode() throws Exception {
        String errorBytesStr = "0adacc600000000000000000250000002e000000006e756c6c0a6e756c6c0a6e756c6c0a313231353630313731360adacc4000000000000000002d0000001e000000003739353630383437370a";
//        String errorBytesStr = "dacc4000000000000000002c00000020000000002d313631383930353338370adacc600000000000000000240000002e000000006e756c6c0a6e756c6c0a6e756c6c0a2d3837313534373038380a";
        byte[] errResponse = ByteBufUtil.decodeHexDump(errorBytesStr);

        String rightBytesStr = "dacc600000000000000000250000002e000000006e756c6c0a6e756c6c0a6e756c6c0a313231353630313731360adacc4000000000000000002d0000001e000000003739353630383437370a";
        byte[] rightResponse = ByteBufUtil.decodeHexDump(rightBytesStr);

        CacheContext cacheContext = new CacheContext();

        EmbeddedChannel CADecodeChannel = new EmbeddedChannel(
                new CacheDecoder(cacheContext, null)
        );

        assertFalse(CADecodeChannel.writeInbound(Unpooled.wrappedBuffer(errResponse)));
//        assertTrue(CADecodeChannel.writeInbound(Unpooled.wrappedBuffer(rightResponse)));
//        Invocation  invocation = CADecodeChannel.readInbound();
//        System.out.println(invocation);






    }

}