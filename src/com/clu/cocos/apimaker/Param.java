package com.clu.cocos.apimaker;

public class Param {

    /**
     * 参数类型
     */
    public Type type;

    /**
     * 参数名
     */
    public String name;

    /**
     * 参数注释
     */
    public String comment;

    public Param() {

    }

    public Param(Type type, String name) {
        this.type = type;
        this.name = name;
    }
}
