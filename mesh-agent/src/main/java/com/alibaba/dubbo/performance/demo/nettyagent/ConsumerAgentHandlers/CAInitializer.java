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

    static List<Endpoint> endpoints = Collections.unmodifiableList(Arrays.asList(
            new Endpoint("provider-large",30000),
            new Endpoint("provider-medium",30000),
            new Endpoint("provider-small",30000)));

    private HashMap<Endpoint, HashMap<FuncType, Integer>> endpointMethodsIDs = null;
    private HashMap<Endpoint, HashMap<Integer, FuncType>> endpointMethods = null;


    public CAInitializer() {
        super();
        // 带权重的 weighted endpoints 整个类共享
        if (weightedEndpoints == null) {
            List<Integer> weight = Arrays.asList(3, 2, 1);
            assert endpoints.size() == weight.size();
            weightedEndpoints = new ArrayList<Endpoint>();
            for (int i = 0; i < weight.size(); ++i) {
                for (int j = 0; j < weight.get(i); j++) {
                    weightedEndpoints.add(endpoints.get(i));
                }
            }
            weightedEndpoints = Collections.unmodifiableList(weightedEndpoints);
            }

        // 因为 CA 只有一个，对每个单个 Provider 的缓存都是一样的，所以两部分关于 FuncType 的缓存整个类共享
        if (endpointMethodsIDs == null) {
            endpointMethodsIDs = new HashMap<Endpoint, HashMap<FuncType, Integer>>();
            for (Endpoint endpoint : endpoints) {
                endpointMethodsIDs.put(endpoint, new HashMap<FuncType, Integer>());
            }
        }

        if (endpointMethods == null) {
            endpointMethods = new HashMap<Endpoint, HashMap<Integer, FuncType>>();
            for (Endpoint endpoint : endpoints) {
                endpointMethods.put(endpoint, new HashMap<Integer, FuncType>());
            }
        }
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        ChannelPipeline p = ch.pipeline();
        ChannelFuture providerChannelFuture = bootStrapProviderChannel(ch);

        p.addLast("httpCodc", new HttpServerCodec());
        p.addLast("aggregator", new HttpObjectAggregator(100000));

        /*
        这个当 consumer 传来请求时，将 HTTP POST 中内容提取出来构造一个 invocation，并写入到 PA 中
         */
        p.addLast("ReadFromConsumer", new ReceiveConsumerHandler(providerChannelFuture));

        /*
           当写入 Invocation 到 Consumer 时，
           下面的这个 handler将 invocation 的 result 提取出来，构造出一个 POST 请求，传递个 http encoder
        */
        p.addLast("sendToConsumer", new SendConsumerHandler());

    }


    /*
      创建一个连接到 provider agent 的连接
     */
    private ChannelFuture bootStrapProviderChannel(Channel channel) {
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
                        pipeline.addLast("CacheEncoder", new CacheEncoder(endpointMethodsIDs.get(selectedProvider)));
                        pipeline.addLast("CacheDecoder", new CacheDecoder(endpointMethods.get(selectedProvider)));
                        // 当读入 PA 的返回结果时，继续引发 CA 写结果回 consumer      C <-- CA <-- PA （时间开始事件为 CA 读入PA的返回结果）
                        pipeline.addLast("WriteToConsumer", new ChannelInboundHandlerAdapter(){
                            final Channel consumerChannel = channel;
                            @Override
                            public void channelRead(ChannelHandlerContext ctx_, Object msg) throws Exception {
                                Invocation invocation = (Invocation) msg;
                                    consumerChannel.write(invocation);
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
