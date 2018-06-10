package com.alibaba.dubbo.performance.demo.nettyagent.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by gexinjie on 2018/6/9.
 */
public class GetTraceString {
    static public String get(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        return  sStackTrace;
    }
}
