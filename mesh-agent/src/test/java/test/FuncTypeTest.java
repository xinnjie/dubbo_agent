package test;

import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/2.
 */
public class FuncTypeTest {
    @Test
    public void equals() throws Exception {
        HashMap<FuncType, Integer> map = new HashMap<>();
        String aa = "aa",
                bb = "bb",
                cc = "cc";
        map.put(new FuncType(aa, bb, cc), 1);
        Invocation invocation = new Invocation();
        invocation.setInterfaceName("aa");
        invocation.setMethodName("bb");
        invocation.setParameterTypes("cc");
        assertEquals(invocation.hashCode(), new FuncType(aa, bb,cc).hashCode());
        assertTrue(map.containsKey(invocation));
        assertTrue(map.containsKey(new FuncType(aa, bb, cc)));
    }

}