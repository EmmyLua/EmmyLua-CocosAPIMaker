package com.clu.cocos.apimaker;

import java.util.*;

/**
 * 类型
 */
public class Type {

    public String name;

    /**
     * 返回的name，比如：-- @return Widget#Widget self (return value: ccui.Widget)，值就为self
     */
    public String returnName;

    // 是否为自己，如果是自己，则返回的类型需要根据实际类型输出
    // 必须精确到某个类的某个方法的返回值，因为Type是复用的
    private Map<Map.Entry<Class, Function>, Boolean> isSelfMap = new HashMap<>();

    private Type(String name) {
        this.name = name;
    }

    private static final Map<String, Type> TYPES = new LinkedHashMap<>();

    /**
     * 获取类型
     * @param typeName 不要包含#和.，可以有空格(unsigned int)
     * @return
     */
    public static Type getType(String typeName) {
        String aliasTypeName = typeName;
        typeName = toEmmyLuaTypeName(typeName);
        Type type = TYPES.get(typeName);
        if (type == null) {
            type = new Type(typeName);
            TYPES.put(typeName, type);
        }

        if (!aliasTypeName.equals(typeName) && !TYPES.containsKey(aliasTypeName)) {
            TYPES.put(aliasTypeName, type);
        }
        return type;
    }

    public static Type getType(String typeName, String returnName) {
        Type type = getType(typeName);
        if (CommonUtils.hasText(returnName)) {
            type.returnName = returnName;
        }
        return type;
    }

    private static String toEmmyLuaTypeName(String typeName) {
        if (typeName.contains(" ")) {
            // unsigned int变成int，有空格可能插件无法识别
            if (typeName.toLowerCase().startsWith("unsigned")) {
                return "number";
            }
            return CommonUtils.stringAfter(typeName, " ");
        } else if (typeName.contains(".")) {
            // #cc.Node, #ccui.Widget
            return CommonUtils.stringAfter(typeName, ".");
        } else if (typeName.contains("::")) {
            // experimental::ui::
            return CommonUtils.stringAfterLast(typeName, "::");
        } else if ("function".equalsIgnoreCase(typeName)) {
            return "fun()";
        } else if ("bool".equalsIgnoreCase(typeName)) {
            return "boolean";
        } else if (Arrays.asList("float", "int", "long", "char", "short").contains(typeName.toLowerCase())) {
            return "number";
        } else {
            switch (typeName.toLowerCase()) {
                case "vec2_table":
                    return "{x:number,y:number}";
                case "vec3_table":
                    return "{x:number,y:number,z:number}";
                case "mat4_table":
                    return "number[]";
                case "color3b_table":
                case "color3f_table":
                    return "{r:number,g:number,b:number}";
                case "color4f_table":
                case "color4b_table":
                    return "{r:number,g:number,b:number,a:number}";
                case "size_table":
                    return "{width:number,height:number}";
                case "rect_table":
                    return "{x:number,y:number,width:number,height:number}";
                default:
                    if (typeName.toLowerCase().endsWith("_table")) {
                        return "table";
                    }
                    break;
            }
            return typeName;
        }
    }

    public void setFunctionReturnSelf(Class clazz, Function function, boolean isSelf) {
        isSelfMap.put(new AbstractMap.SimpleEntry<>(clazz, function), isSelf);
    }

    public boolean isFunctionReturnSelf(Class clazz, Function function) {
        return Boolean.parseBoolean(String.valueOf(isSelfMap.get(new AbstractMap.SimpleEntry<>(clazz, function))));
    }

}
