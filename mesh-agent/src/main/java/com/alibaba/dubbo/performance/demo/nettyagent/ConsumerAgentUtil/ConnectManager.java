package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.Endpoint;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gexinjie on 2018/6/4.
 */
/*
 ConnectManager 负责创建从 CA 到 3个 PA 的所有连接
 */
public class ConnectManager {
    private final EventLoopGroup eventLoopGroup;
    private Logger logger = LoggerFactory.getLogger(ConnectManager.class);
    private List<Endpoint> weightedEndpoints = null;
    static final Random random = new Random();
    private final List<Endpoint> endpoints;
    private HashMap<Endpoint, List<Channel>> endpoint2Channel = new HashMap<>();
    private List<Channel> PAChannels = new ArrayList<>();
//    final private Map<Long, Channel> request2CAChannel= Collections.synchronizedMap(new HashMap<>());
    // TODO  request2CAChannel 可以进行优化
    // 使用桶装的容器，桶内设n 个板子，对 requestID 进行分流，减少修改时的阻塞
    final private ConcurrentHashMap<Long, Channel> request2CAChannel= new ConcurrentHashMap<>();


    /*
    语义上讲这两份 cache 是属于 CA 的
     */
    private HashMap<Endpoint, ConcurrentHashMap<FuncType, Integer>> endpointMethodsIDs = null;
    private HashMap<Endpoint, ConcurrentHashMap<Integer, FuncType>> endpointMethods = null;

    public ConnectManager(EventLoopGroup eventLoopGroup, List<Endpoint> endpoints) {
        this.eventLoopGroup = eventLoopGroup;
        this.endpoints = endpoints;
        initVariables();
        initConnectToPA();
    }

    private void initVariables() {
        /*
         初始化 endpoint2Channel
         */
        for (Endpoint endpoint : this.endpoints) {
            this.endpoint2Channel.put(endpoint, new ArrayList<>());
        }


        List<Integer> weight = Arrays.asList(24, 16, 8);
        assert endpoints.size() == weight.size();
        weightedEndpoints = new ArrayList<>();
        for (int i = 0; i < endpoints.size(); ++i) {
            for (int j = 0; j < weight.get(i); j++) {
                weightedEndpoints.add(endpoints.get(i));
            }
        }
        weightedEndpoints = Collections.unmodifiableList(weightedEndpoints);

        endpointMethodsIDs = new HashMap<>();
        for (Endpoint endpoint : endpoints) {
            endpointMethodsIDs.put(endpoint, new ConcurrentHashMap<>());
        }

        endpointMethods = new HashMap<>();
        for (Endpoint endpoint : endpoints) {
            endpointMethods.put(endpoint, new ConcurrentHashMap<>());
        }
    }

    /**
     * 一次性创建所有到 PA 的连接, 总个数和权重和决定
     * CA 右侧的连接们
     * 负责收到 PA 的回应后，编码为 invocation后发回给 consumer
     *
     *
     *
     *
     * option:
     * ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK 和 channelWritabilityChanged() 配合进行对发送数据的聚集，而不是每次收到一条 invocation 就发送，减少 socket send 的调用次数
     */
    private void initConnectToPA() {
        HashMap<Endpoint, List<ChannelFuture>> PAChannelFutures = new HashMap<>();
        for (Endpoint endpoint :
                this.endpoints) {
            PAChannelFutures.put(endpoint, new ArrayList<>());
        }
        for (Endpoint endpoint : weightedEndpoints) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class)
                    .group(this.eventLoopGroup)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 1200)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 由于只有在 response 端会用 FirstTime Cache 这里传入了 null，表示不需要
                            pipeline.addLast("CacheEncoder", new CacheEncoder(endpointMethodsIDs.get(endpoint), null));
                            pipeline.addLast("CacheDecoder", new CacheDecoder(endpointMethods.get(endpoint), null));
                            // 当读入 PA 的返回结果时，继续引发 CA 写结果回 consumer      C <-- CA <-- PA （时间开始事件为 CA 读入PA的返回结果）
                            pipeline.addLast("WriteToConsumer", new ChannelInboundHandlerAdapter() {
                                /*
                                dubbo 返回的response，能使用的包括：返回值，request ID
                                todo 是哪里 consumerChannel 需要在运行channelRead 这个方法时决定，而不是在初始化时就决定。
                                 */
                                @Override
                                public void channelRead(ChannelHandlerContext ctx_, Object msg) throws Exception {
                                    Invocation invocation = (Invocation) msg;
                                    // getAccordingConsumerChannel 将会返回对应于 reqeustID 的 consumerChannel （ps *****requestID 和 consumerChannel有对应关系）
                                    Channel consumerChannel = getAccordingConsumerChannel(invocation.getRequestID());
                                    if (consumerChannel != null) {
                                        logger.info("received result from PA， find the right consumer channel for request " + invocation.getRequestID() + ": " + consumerChannel.toString());
                                        // 将来自 PA 的 response 发回给 Consumer
                                        consumerChannel.write(invocation);
                                    } else {
                                        logger.error("request ID: {}  is duplicated! 肯定还有问题", invocation.getRequestID());
                                    }
                                }

                                @Override
                                public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
                                    ctx.flush();
                                }
                            });
                        }
                    });

//            logger.info("connecting to " + endpoint.getHost() + ":" + endpoint.getPort());
            PAChannelFutures.get(endpoint).add(bootstrap.connect(endpoint.getHost(), endpoint.getPort()));
        }

        /*
        等待所有 channel 都连接上，并加进 this.endpoint2Channel, this.PAChannels
         */
        for (Map.Entry<Endpoint, List<ChannelFuture>> pair : PAChannelFutures.entrySet()) {
            for (ChannelFuture future: pair.getValue())
            try {
                future.sync();
                if (! future.isSuccess()) {
                    logger.error("connection to " + future.toString() + " not established");
                } else {
                    logger.info("connection to " + pair.getKey().toString() + " is established");
                    this.endpoint2Channel.get(pair.getKey()).add(future.channel());
                    this.PAChannels.add(future.channel());
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }


    }

    private Channel getAccordingConsumerChannel(long requestID) {
        Channel consumerChannel = this.request2CAChannel.get(requestID);
        if (consumerChannel == null) {
            logger.error("request not in request table, maybe already processed? requestID is :" + requestID);
        }
        this.request2CAChannel.remove(requestID);
        return consumerChannel;
    }

    /**
     *
     * @param requestID 利用这个传入的 requestID 记录下 requestID 和 consumer Channel的对应关系
     * @return
     */
    public Channel getProviderChannel(Channel consumerChannel, long requestID) {
        this.request2CAChannel.put(requestID, consumerChannel);
        Channel selected = PAChannels.get(random.nextInt(PAChannels.size()));
        return selected;
    }

    // todo for debug
    private Endpoint getEndpoint(Channel channel) {
        for (Map.Entry<Endpoint, List<Channel>> pair:
            this.endpoint2Channel.entrySet()
             ) {
            for (Channel c :
                    pair.getValue()) {
                if (c.equals(channel)) {
                    return pair.getKey();
                }
            }
        }
        logger.error("missing according endpoint " + channel.toString());
        return null;
    }

//
//    private Endpoint selectEndpoint() {
//        return weightedEndpoints.get(random.nextInt(endpoints.size()));
//    }

//    public static void main(String[] args) {
//        List<Endpoint> endpoints = Collections.unmodifiableList(Arrays.asList(
//                new Endpoint("provider-large",30000),
//                new Endpoint("provider-medium",30000),
//                new Endpoint("provider-small",30000)));
//
////        // 原来的想法：带权重的 weighted endpoints 整个类共享， 还是尽量避免使用 static 变量
////        if (weightedEndpoints == null) {
//        List<Integer> weight = Arrays.asList(6, 4, 2);
//        assert endpoints.size() == weight.size();
//        List<Endpoint> weightedEndpoints = new ArrayList<>();
//        for (int i = 0; i < endpoints.size(); ++i) {
//            for (int j = 0; j < weight.get(i); j++) {
//                weightedEndpoints.add(endpoints.get(i));
//            }
//        }
//        weightedEndpoints = Collections.unmodifiableList(weightedEndpoints);
//    }

}
