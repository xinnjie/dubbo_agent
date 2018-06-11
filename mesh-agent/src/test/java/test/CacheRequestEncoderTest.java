package test;

import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheRequestDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheRequestEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/11.
 */
public class CacheRequestEncoderTest {
    @Test
    public void encode() throws Exception {
        InvocationRequest request = new InvocationRequest();
        FuncType type = new FuncType();
        type.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        type.setParameterTypes("Ljava/lang/String;");
        type.setMethodName("hash");
        request.setRequestID(335);
        request.setArgument("lsx7sktGyG586Wte");
//        request.setArgument(null);
        request.setFuncType(type);
        CacheContext cacheContext = new CacheContext();
//        cacheContext.put(type.shallowCopy(), 23);


        EmbeddedChannel CAEncoder = new EmbeddedChannel(
                new CacheRequestEncoder(cacheContext)
        );

        assertTrue(CAEncoder.writeOutbound(request));
        ByteBuf out = CAEncoder.readOutbound();
        System.out.println(ByteBufUtil.hexDump(out));
    }

}