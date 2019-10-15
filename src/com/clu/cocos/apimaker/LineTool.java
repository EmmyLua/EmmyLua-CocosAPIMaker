package com.clu.cocos.apimaker;

public class LineTool {

    public static boolean isField(String line) {
        return isAnnoLine(line, Anno.FIELD);
    }

    public static boolean isOverload(String line) {
        return isAnnoLine(line, Anno.OVERLOAD);
    }

    public static boolean isFunction(String line) {
        return isAnnoLine(line, Anno.FUNCTION);
    }

    public static boolean isParam(String line) {
        return isAnnoLine(line, Anno.PARAM);
    }

    public static boolean isReturn(String line) {
        return isAnnoLine(line, Anno.RETURN);
    }

    // -- @module Action 表示类名
    public static boolean isModule(String line) {
        return isAnnoLine(line, Anno.MODULE);
    }

    // -- @extend Ref 表示父类，父类可能有多个，可以通过检测对应的文件是否存在来排除多余的
    public static boolean isExtend(String line) {
        return isAnnoLine(line, Anno.EXTEND);
    }

    // -- @parent_module ccui，表示类名的前缀
    public static boolean isParentModule(String line) {
        return isAnnoLine(line, Anno.PARENT_MODULE);
    }

    private static boolean isAnnoLine(String line, Anno anno) {
        return line.startsWith("-- " + anno.getName());
    }

    public static boolean isModuleSegment(String line) {
        return isModule(line) || isExtend(line) || isParentModule(line);
    }

    public static boolean isFunctionSegment(String line) {
        return isOverload(line) || isFunction(line) || isParam(line) || isReturn(line);
    }

    public static boolean isFieldSegment(String line) {
        return isField(line);
    }

    /**
     * 是否为未知标记，上面的都是已知的标记
     *
     * @param line
     * @return
     */
    public static boolean isUnknownAnnoMaker(String line) {
        return !(isField(line) || isOverload(line) || isFunction(line)
                || isParam(line) || isReturn(line) || isModule(line)
                || isExtend(line) || isParam(line));
    }

}
