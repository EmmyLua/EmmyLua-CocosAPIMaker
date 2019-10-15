package com.clu.cocos.apimaker;

import java.util.ArrayList;
import java.util.List;

/**
 * 方法
 */
public class Function {

    /**
     * 方法注释
     */
    public List<String> comments;

    /**
     * 重载方法
     */
    public List<OverrideFunction> overloads = new ArrayList<>();

    /**
     * 方法名
     */
    public String name;

    /**
     * 返回类型
     */
    public Type returnType;

    /**
     * 参数列表
     */
    public List<Param> params = new ArrayList<>();

    /*public Function(String name, Type returnType, List<Param> params) {
        this.name = name;
        this.returnType = returnType;
        this.params = params;
    }*/

    @Override
    public String toString() {
        return name;
    }
}
