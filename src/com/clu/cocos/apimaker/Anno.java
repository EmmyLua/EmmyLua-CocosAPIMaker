package com.clu.cocos.apimaker;

/**
 * 注解
 */
public enum Anno {

    FIELD("@field"),
    OVERLOAD("@overload"),
    MODULE("@module"),
    EXTEND("@extend"),
    PARENT_MODULE("@parent_module"),
    FUNCTION("@function"),
    PARAM("@param"),
    RETURN("@return");

    private String name;
    Anno(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}
