/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.performance.demo.nettyagent.model;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC Invocation.
 *
 * @serial Don't change the class name and properties.
 */
public class Invocation extends FuncType{

//    private static final long serialVersionUID = -4355285085441097045L;

    private static AtomicLong atomicLong = new AtomicLong(31);
    private static AtomicInteger atomicInteger = new AtomicInteger(23);

    private String arguments;

    private String result;

    private Map<String, String> attachments;

    private long requestID = -1;
    private int methodID = -1;

    public static long getUniqueReqeustID() {
        return atomicLong.getAndIncrement();
    }
    public static int getUniqueMethodID() {
        return atomicInteger.getAndIncrement();
    }

    public int getMethodID() {
        return methodID;
    }

    public void setMethodID(int methodID) {
        this.methodID = methodID;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public long getRequestID() {
        return requestID;
    }

    public void setRequestID(long requestID) {
        this.requestID = requestID;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }


    public void setAttachment(String key, String value) {
        /*
        path 和 interface 同义
         */
        if (key.equals("path")) {
            this.setInterfaceName(value);
            return;
        }
        if (attachments == null) {
            attachments = new HashMap<String, String>();
        }
        attachments.put(key, value);
    }

    public String getAttachment(String key, String defaultValue) {
        if (key.equals("path")) {
            if (this.getInterfaceName() != null) {
                return this.getInterfaceName();
            } else {
                return defaultValue;
            }
        }
        if (attachments == null) {
            return defaultValue;
        }
        String value = attachments.get(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    public String getAttachment(String key) {
        if (key.equals("path")) {
            if (this.getInterfaceName() != null) {
                return this.getInterfaceName();
            } else {
                return null;
            }
        }
        if (attachments == null) {
            return null;
        }
        return attachments.get(key);
    }

    public Map<String, String> getAttachments() {
        if (this.getInterfaceName() != null)  {
            if (attachments != null ) {
                HashMap<String, String> new_map = new HashMap<>(attachments);
                new_map.put("path", this.getInterfaceName());
                return new_map;
            } else {
                HashMap<String, String> new_map = new HashMap<>();
                new_map.put("path", this.getInterfaceName());
                return new_map;
            }
        }
            return attachments;
    }

    @Override
    public String toString() {
        return "Invocation{" +
                "methodName='" + this.getMethodName() + '\'' +
                ", parameterTypes='" + this.getParameterTypes() + '\'' +
                ", arguments='" + arguments + '\'' +
                ", result='" + result + '\'' +
                ", requestID=" + requestID +
                ", methodID=" + methodID +
                '}';
    }
}