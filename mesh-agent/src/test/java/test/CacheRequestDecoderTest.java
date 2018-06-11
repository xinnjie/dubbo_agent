package test;

import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheRequestDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gexinjie on 2018/6/11.
 */
public class CacheRequestDecoderTest {
    @Test
    public void decode() throws Exception {
        String hexdump = "dacc800000000000000000fe00000062686173680a4c6a6176612f6c616e672f537472696e673b0a636f6d2e616c69626162612e647562626f2e706572666f726d616e63652e64656d6f2e70726f76696465722e4948656c6c6f536572766963650adacc800000000000000000ff00000325686173680a4c6a6176612f6c616e672f537472696e673b0a636f6d2e616c69626162612e647562626f2e706572666f726d616e63652e64656d6f2e70726f76696465722e4948656c6c6f536572766963650a55676559337a736a714a366d54307a6b6a7962584b7732647467463151615835345772396e6646567a44356d6e58704775355a4e566a5131435569474c695277564230537035506c4b654e484c646d6e3179747a334d764d61396359304e78696a5447415a3450796f4c4f54355648306f724b3649315470796b656c6c6351584a4b354b4b676d4d524d51494f5436427946624552";
        byte[] requestByte = ByteBufUtil.decodeHexDump(hexdump);
        ByteBuf requestBuf = Unpooled.wrappedBuffer(requestByte);

        CacheContext cacheContext = new CacheContext();
        ConcurrentHashMap<Long, Integer> requestToMethodFirstCache = new ConcurrentHashMap<>();


        EmbeddedChannel PADecodeChannel = new EmbeddedChannel(
                new CacheRequestDecoder(cacheContext, requestToMethodFirstCache)
        );

//        PADecodeChannel.writeInbound(requestBuf);

    }

}