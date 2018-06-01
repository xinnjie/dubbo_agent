package com.alibaba.dubbo.performance.demo.nettyagent.model;

/**
 * Created by gexinjie on 2018/5/30.
 */
public class FuncType {
    private String methodName;

    private String parameterTypes;

    public FuncType(String methodName, String parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    public FuncType() {
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(String parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    /*
    只浅复制一次调用的方法名，类型名，接口名，参数，方法号
     */
    public FuncType shallowCopy() {
        return new FuncType(this.methodName, this.parameterTypes);
    }
    public void shallowCopyInPlace(FuncType ft) {
        ft.setMethodName(this.methodName);
        ft.setParameterTypes(this.parameterTypes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        为了方便将与 methodName, parameterTypes 相等的 RpcInvocation比较结果为相等，这里去掉了对象类型的比较
        if (o == null ) return false;

        FuncType funcType = (FuncType) o;

        if (methodName != null ? !methodName.equals(funcType.methodName) : funcType.methodName != null) return false;
        return parameterTypes != null ? parameterTypes.equals(funcType.parameterTypes) : funcType.parameterTypes == null;
    }

    @Override
    public int hashCode() {
        int result = methodName != null ? methodName.hashCode() : 0;
        result = 31 * result + (parameterTypes != null ? parameterTypes.hashCode() : 0);
        return result;
    }
}
