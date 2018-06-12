package test;

import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheRequestDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
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
//        String hexdump = "dacc800000000000000000fe00000062686173680a4c6a6176612f6c616e672f537472696e673b0a636f6d2e616c69626162612e647562626f2e706572666f726d616e63652e64656d6f2e70726f76696465722e4948656c6c6f536572766963650adacc800000000000000000ff00000325686173680a4c6a6176612f6c616e672f537472696e673b0a636f6d2e616c69626162612e647562626f2e706572666f726d616e63652e64656d6f2e70726f76696465722e4948656c6c6f536572766963650a55676559337a736a714a366d54307a6b6a7962584b7732647467463151615835345772396e6646567a44356d6e58704775355a4e566a5131435569474c695277564230537035506c4b654e484c646d6e3179747a334d764d61396359304e78696a5447415a3450796f4c4f54355648306f724b3649315470796b656c6c6351584a4b354b4b676d4d524d51494f5436427946624552";
        String hexdump = "daccc000000000000000030c0000023b000000175a4a755348734c516e3242734176656475766b39516865566649647473734133694f5030486357545a754737337179527473706d646b4f5a5a69305a726d457833556478727030344f476f594a4f626d4f3466504a4257795a525462534a514b633142455a446c6632445734783361624e5245524f64724d3057726f674b6b50504961316f545166394f306b394470413442675654454a5a4739615932797379745670653566764e3861425649433670636c69616d634d63766c354a715339694f486a4d5477516f7446503155514969486f304138697264734f563431664e674a4178707530414f6b5156355841436d7a51703465506155366231617448787848724d4d35513642576f5851324f6f7a334672314d4c376e4553623842437944596c4d54644738534c6c584d46476f63776c3775756c3852497a4f336d49345a4d41774e753958756a684c3139726e42633446733837454b384b5357636c786f363551487436476670657851516b314d746c387353756d424451636732656f466b4f3046626e6f484936797358744669484a494e617037474e70433557354e6f4b4152636c654744446d4d4b337263665a38424778656564426e4d584e375633634a7746456e623955544f6148464165336d7838303730587755344f795733316136374f6b77546c3558667130376e7142776c6746346f66364a6246464930427746417548775a3535454948697835637a31364c3741324676496a7437774c6c4c434537506d69";
        byte[] requestByte = ByteBufUtil.decodeHexDump(hexdump);
        ByteBuf requestBuf = Unpooled.wrappedBuffer(requestByte);

        FuncType type = new FuncType();
        type.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        type.setParameterTypes("Ljava/lang/String;");
        type.setMethodName("hash");
        CacheContext cacheContext = new CacheContext();
        cacheContext.put(type, 23);
        ConcurrentHashMap<Long, Integer> requestToMethodFirstCache = new ConcurrentHashMap<>();


        EmbeddedChannel PADecodeChannel = new EmbeddedChannel(
                new CacheRequestDecoder(cacheContext, requestToMethodFirstCache)
        );

        PADecodeChannel.writeInbound(requestBuf);

    }

}