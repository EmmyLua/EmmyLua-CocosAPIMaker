package com.clu.cocos.apimaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 类
 */
public class Class {
    /**
     * 类名
     */
    public String name;
    /**
     * 类注释
     */
    public List<String> comments;
    /**
     * 属性
     */
    public List<Field> fields = new ArrayList<>();
    /**
     * 方法列表
     */
    public List<Function> functions = new ArrayList<>();
    /**
     * 父类
     */
    public Class superClass;

    /**
     * 类名的前缀
     */
    public String namePrefix;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Class aClass = (Class) o;
        return Objects.equals(name, aClass.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public Class(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        if (namePrefix == null) {
            return "Class: " + name;
        } else {
            return "Class: " + namePrefix + "." + name;
        }
    }
}
