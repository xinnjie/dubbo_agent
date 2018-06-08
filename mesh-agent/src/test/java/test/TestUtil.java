package test;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.channel.embedded.EmbeddedChannel;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gexinjie on 2018/6/8.
 */
public class TestUtil {
    static public EmbeddedChannel getInitialCacheEncoder() {
        ConcurrentHashMap<Long, Integer> requestToMethodFirstCache = new ConcurrentHashMap<>();
        CacheContext cacheContext = new CacheContext();

        EmbeddedChannel encodeChannel = new EmbeddedChannel(
                new CacheEncoder(cacheContext, requestToMethodFirstCache)
        );
        return encodeChannel;
    }

    static public EmbeddedChannel getInitialCacheDecoder() {
        ConcurrentHashMap<Long, Integer> requestToMethodFirstCache = new ConcurrentHashMap<>();
        CacheContext cacheContext = new CacheContext();

        EmbeddedChannel decodeChannel = new EmbeddedChannel(
                new CacheDecoder(cacheContext, requestToMethodFirstCache)
        );
        return decodeChannel;
    }

    static public Invocation getFullInvocation() {
        Invocation invocation = new Invocation();
        invocation.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setParameterTypes("Ljava/lang/String;");
        invocation.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setRequestID(31);
        invocation.setMethodName("hash");
        invocation.setMethodID(10);
        invocation.setResult("hello");
        invocation.setArguments("hi, this is arguments");
        return invocation;
    }
}
