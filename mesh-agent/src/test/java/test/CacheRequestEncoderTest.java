package test;

import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheRequestEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/11.
 */
public class CacheRequestEncoderTest {
    private InvocationRequest request;
    private CacheContext emptyCache;

    @Before
    public void setUp() throws Exception {
        request = new InvocationRequest();
        FuncType type = new FuncType();
        type.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        type.setParameterTypes("Ljava/lang/String;");
        type.setMethodName("hash");
        request.setRequestID(335);
        ByteBuf argument = Unpooled.wrappedBuffer("lsx7sktGyG586Wte".getBytes());
        request.setArgument(argument);
        request.setFuncType(type);
        emptyCache = new CacheContext();
    }

    @Test
    public void encode() throws Exception {
        EmbeddedChannel CAEncoder = new EmbeddedChannel(
                new CacheRequestEncoder(emptyCache)
        );

        assertTrue(CAEncoder.writeOutbound(request));
        ByteBuf out = CAEncoder.readOutbound();
    }

}