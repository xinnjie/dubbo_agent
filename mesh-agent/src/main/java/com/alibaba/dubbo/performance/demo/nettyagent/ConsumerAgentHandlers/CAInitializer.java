package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentHandlers;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.Endpoint;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by gexinjie on 2018/5/28.
 */
public class CAInitializer extends ChannelInitializer<SocketChannel> {
    private Logger logger = LoggerFactory.getLogger(CAInitializer.class);
    static private List<Endpoint> weightedEndpoints = null;
    static final Random random = new Random();

    private final List<Endpoint> endpoints;

    /*
    语义上讲这两份 cache 是属于 CA 的
     */
    static private HashMap<Endpoint, HashMap<FuncType, Integer>> endpointMethodsIDs = null;
    static private HashMap<Endpoint, HashMap<Integer, FuncType>> endpointMethods = null;

    public CAInitializer(List<Endpoint> endpoints) {
        super();
        this.endpoints = endpoints;
        // 带权重的 weighted endpoints 整个类共享
        if (weightedEndpoints == null) {
            List<Integer> weight = Arrays.asList(3, 2, 1);
            assert endpoints.size() == weight.size();
            weightedEndpoints = new ArrayList<>();
            for (int i = 0; i < endpoints.size(); ++i) {
                for (int j = 0; j < weight.get(i); j++) {
                    weightedEndpoints.add(endpoints.get(i));
                }
            }
            weightedEndpoints = Collections.unmodifiableList(weightedEndpoints);
            }

        // 因为 CA 只有一个，对每个单个 Provider 的缓存都是一样的，所以两部分关于 FuncType 的缓存整个类共享
        if (endpointMethodsIDs == null) {
            endpointMethodsIDs = new HashMap<>();
            for (Endpoint endpoint : endpoints) {
                endpointMethodsIDs.put(endpoint, new HashMap<>());
            }
        }

        if (endpointMethods == null) {
            endpointMethods = new HashMap<>();
            for (Endpoint endpoint : endpoints) {
                endpointMethods.put(endpoint, new HashMap<>());
            }
        }
    }

    /*
   consumer agent 接收到 consumer 的连接后，需要选择一个 provider agent 进行连接
   CA 在接收到 consumer 的请求后
   1. 将 HTTP 形式的请求解码为 Invocation 对象
   2. 编码为用合适的形式发送给 PA
   3. PA 以合适的形式返回结果
   4. CA 将结果编码为 HTTP 返回给 consumer
 */

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        ChannelPipeline p = ch.pipeline();
        ChannelFuture providerChannelFuture = bootStrapProviderChannel(ch);

        p.addLast("httpCodec", new HttpServerCodec());
        p.addLast("aggregator", new HttpObjectAggregator(100000));

        /*
        这个当 consumer 传来请求时，将 HTTP POST 中内容提取出来构造一个 invocation，并写入到 PA 中
         */
        p.addLast("http2invocation", new Http2RequestInvocation());
        p.addLast("transmit2PA", new Transmit2PA(providerChannelFuture));

        /*
           当写入 Invocation 到 Consumer 时，
           下面的这个 handler将 invocation 的 result 提取出来，构造出一个 POST 请求，传递个 http encoder
        */
        p.addLast("sendToConsumer", new InvocationResult2Http());

    }


    /*
      创建一个连接到 provider agent 的连接
     */
    private ChannelFuture bootStrapProviderChannel(Channel channel) {
        /*
         从 CA 到 PA 的连接部分，属于负载均衡
         现在使用了很多的连接数（CA 到 PA），并使用加权随机方式连接到 PA
         */
        Endpoint selectedProvider = selectEndpoint();
        Bootstrap bootstrap = new Bootstrap();

        bootstrap.channel(NioSocketChannel.class)
                .group(channel.eventLoop())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 由于只有在 response 端会用 FirstTime Cache 这里传入了 null，表示不需要
                        pipeline.addLast("CacheEncoder", new CacheEncoder(endpointMethodsIDs.get(selectedProvider), null));
                        pipeline.addLast("CacheDecoder", new CacheDecoder(endpointMethods.get(selectedProvider), null));
                        // 当读入 PA 的返回结果时，继续引发 CA 写结果回 consumer      C <-- CA <-- PA （时间开始事件为 CA 读入PA的返回结果）
                        pipeline.addLast("WriteToConsumer", new ChannelInboundHandlerAdapter(){
                            final Channel consumerChannel = channel;
                            @Override
                            public void channelRead(ChannelHandlerContext ctx_, Object msg) throws Exception {
                                Invocation invocation = (Invocation) msg;
                                    consumerChannel.writeAndFlush(invocation);
                            }
                        });
                    }
                });

        logger.info("connecting to " + selectedProvider.getHost() + ":" + selectedProvider.getPort());
        return bootstrap.connect(selectedProvider.getHost(), selectedProvider.getPort());
    }

    private Endpoint selectEndpoint() {
        return weightedEndpoints.get(random.nextInt(endpoints.size()));
    }
}
