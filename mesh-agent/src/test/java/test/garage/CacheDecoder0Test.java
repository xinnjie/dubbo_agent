package test.garage;

import com.alibaba.dubbo.performance.demo.nettyagent.garage.CacheDecoder0;
import com.alibaba.dubbo.performance.demo.nettyagent.garage.CacheEncoder0;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gexinjie on 2018/6/6.
 */
public class CacheDecoder0Test {
    @Test
    public void simpleDecode() throws Exception {
        String arguments = "khU795hdvyhj02qYs5I7XTvMZ9qj3CVarzdWUSmecwJyhh4MhbBaX1uAc785AEOL8pP7AjdfG345jM39NDdYRdqxoZU4I2izhHsjYSsB0JY18EiCbNqgBiGvIAmJWqKmA7u0bCaNBGb66IAboJGZEA8t0tSpApOOg4HVFjmAMVENxFFzz9ORw1eOUQhCI8bErHWDKah9Mh1yuirBs5lP5pyv9gD4KPUvdlxQ5iJEcdLgEePBSMpxhfp3AI9mAmI5yP1UGQ2vx62leVIFhqSIwGc2jMW3lZ7ztVtNReM7aS5n9Zg3fmMDJDlKRDbMDjapedrfQwETGFNVFX1BgsjCrMWB2PQWDUEXQNagewYF1Z0GJ99eqHFPGSGSHygVVfnmeq1gTVYB0skw5VR8UDIEh0a7b1d3GfKUUCkynJHAr3OcyyRLbEgicdWgtUFubkDpdPzjlVYgIBcPFKVYDQfEBcQxQf1oz2CJDMFlbAOe6wtABTOkA3wlg5whtMhk4QneUpeE56mobEBhmcaRwGpPurLbtnxgR1FRfc5TG1VZEy6duFP7D0jakhp0gHNu2yVzQctl7kemwMlvTImX4ZEXr1XLMooWnWOyINml5JLFmYsGxfhHZPlwbie7IpbvqRTZ3JlnBqutcnYxYz7c6v5Igh3OzqWIHXOSzvZEQs8geyGIuVkzr7uPmfirS1YwAtkdWGD60NxDnGNEawxiu3sRKcjzO1lpvhBeZHo0gUYqKlc3";
        Invocation invo = new Invocation();
        invo.setArguments(arguments);
        invo.setRequestID(37);
        invo.setInterfaceName("com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invo.setParameterTypes("Ljava/lang/String;");
        invo.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");

        byte[] bytes = ByteBufUtil.decodeHexDump("dacc8000000000000000002a00000204686173680a4c6a6176612f6c616e672f537472696e673b0a636f6d2e616c69626162612e647562626f2e706572666f726d616e63652e64656d6f2e70726f76696465722e4948656c6c6f536572766963650a61795561426e32566361705a49486d5578647a484576614b70454f756f37444147394f7659596971664e3774384750556a6b6b6178724366515872577a705659714f5570463649373663426569443074765457784677716c717946726b5a533971585a773778355043457144514338516a615638564f57336749645746356e684a36705551563345636a7a69597a4c564e57676e6c4c645154635142597a76514e6f736159376c453359787735716772794a4c446339654565394273733031427a727135575947457a36536e737235623537634d6632773365524943464f3131767235666d6d324f6c6d343346715166737534525132543772545363614f366b686f616439566865493632387656417744374b4679614e394a4c386b6f556b646e727a6c7356326d416d39414a395569584673764d4172507857696a4b6543747a77424d783858675741664f474777706a50516f33546e6153577532436a6174767936383449545a45784b39337649377151493342507039634d666c725a544e7239344969465074446558357034633051614b49317337767976504536706168540adacc8000000000000000003200000088686173680a4c6a6176612f6c616e672f537472696e673b0a636f6d2e616c69626162612e647562626f2e706572666f726d616e63652e64656d6f2e70726f76696465722e4948656c6c6f536572766963650a7971314a466552476a33566634366277386b73366e624f37386967326b5557455a723254770a");

        EmbeddedChannel PAEncodeChannel = new EmbeddedChannel(
                new CacheEncoder0(new ConcurrentHashMap<>(), null)
        );
        EmbeddedChannel PADecodeChannel = new EmbeddedChannel(
                new CacheDecoder0(new ConcurrentHashMap<>(), new ConcurrentHashMap<>())
        );

        PADecodeChannel.writeInbound(Unpooled.wrappedBuffer(bytes));

//        PAEncodeChannel.writeOutbound(invo);
//        ByteBuf out = PAEncodeChannel.readOutbound();
//        PADecodeChannel.writeInbound(out);
    }
}