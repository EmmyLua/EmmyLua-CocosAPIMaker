package com.clu.cocos.apimaker;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class CocosdocMaker {

    public static final List<String> TOP_MODULE_NAMES = Arrays.asList("cc", "ccui");

    /**
     * 段落的开始
     */
    public static final String SEGMENT_START = "--------------------------------";

    /**
     * field的段落的开头
     */
    public static final String SEGMENT_START_FIELD = "--------------------------------------------------------";

    private List<String> sourceLines;

    /**
     * 类池，便于构建Class.parent属性，为了降低复杂度，key为Class.name
     */
    private static final Map<String, Class> CLASS_POOL = new LinkedHashMap<>();

    private Class clazz;

    public CocosdocMaker(List<String> sourceLindes) {
        this.sourceLines = sourceLindes;
    }

    /**
     * 源码转类
     * @return
     */
    public void toClass() {
        // 段落的行
        List<String> segmentLines = new ArrayList<>();
        boolean segmentStart = false;

        int lineNumber = 0;
        for (String line : this.sourceLines) {
            lineNumber++;
            line = line.trim();

            // 删除空行
            if (line.isEmpty()) {
                continue;
            }

            // 匹配到下一个段落或者文件扫描完毕了, lineNumber从1开始的，所以=size
            if (SEGMENT_START.equals(line) || SEGMENT_START_FIELD.equals(line) || lineNumber == this.sourceLines.size()) {
                // 进入段落
                if (segmentStart == false) {
                    segmentStart = true;
                } else {
                    // 段落结束了
                    if (SegmentTool.isModuleSegment(segmentLines)) {
                        processModuleSegment(segmentLines);
                    } else if (SegmentTool.isFunctionSegment(segmentLines)) {
                        processFunctionSegment(segmentLines);
                    } else if (SegmentTool.isFieldSegment(segmentLines)) {
                        processFieldSegment(segmentLines);
                    } else {
                        String errorMessage = CommonUtils.join(segmentLines, "\n");
                        throw new RuntimeException("未知的段落：" + errorMessage);
                    }

                    // 段落处理完毕，准备下一个段落
                    segmentLines.clear();
                }
            }

            segmentLines.add(line);
        }
    }

    /**
     * 类声明处理
     *
     * @param segmentLines
     */
    private void processModuleSegment(List<String> segmentLines) {
        List<String> comments = new ArrayList<>();
        for (String line : segmentLines) {
            String[] split = null;
            if (LineTool.isModuleSegment(line)) {
                split = line.split(" ");
            }
            if (LineTool.isModule(line)) {
                String className = split[split.length - 1];
                this.clazz = getOrCreate(className);
            } else if (LineTool.isExtend(line)) {
                // -- @extend Widget,LayoutProtocol
                String superClassName = split[split.length - 1];
                if (superClassName.contains(",")) {
                    // 取第一个
                    superClassName = superClassName.split(",")[0];
                }
                Class superClass = getOrCreate(superClassName);
                this.clazz.superClass = superClass;
            } else if (LineTool.isParentModule(line)) {
                this.clazz.namePrefix = split[split.length - 1];
            } else {
                // 其他的都认为是注释
                comments.add(processComment(line));
            }
        }
        this.clazz.comments = comments;
    }

    /**
     * 方法处理
     *
     * @param segmentLines
     */
    private void processFunctionSegment(List<String> segmentLines) {
        List<String> comments = new ArrayList<>();
        Function function = new Function();
        for (String line : segmentLines) {
            if (LineTool.isOverload(line)) {
                // -- @overload self, vec2_table, float, float, unsigned int, color4f_table
                OverrideFunction overrideFunction = new OverrideFunction();
                // 剔除开头的注解
                line = line.replace("-- " + Anno.OVERLOAD.getName(), "").trim();
                String[] paramTypes = line.split(",");
                for (String typeName : paramTypes) {
                    Type type = Type.getType(typeName.trim());
                    overrideFunction.params.add(new Param(type, null));
                }
                function.overloads.add(overrideFunction);
            } else if (LineTool.isFunction(line)) {
                // -- @function [parent=#DrawNode] drawSolidCircle
                String[] split = line.split(" ");
                function.name = split[3];
            } else if (LineTool.isParam(line)) {
                // -- @param self
                // -- @param #float angle
                // -- @param #unsigned int segments
                // -- @param #unsigned char red
                // -- @param #ccui.Widget current 当前焦点的控件
                Param param = new Param();
                // #float angle
                line = line.replace("-- " + Anno.PARAM.getName(), "").trim();
                String[] split = line.split(" ");
                // self 或者 #unsigned
                String selfOrTypeName = split[0];
                if (!"self".equals(selfOrTypeName)) {
                    // 非self，其实就是typeName了
                    if (selfOrTypeName.equals("#unsigned")) {
                        selfOrTypeName += split[1];
                        // 防止没有参数名，只有参数类型
                        if (split.length > 2) {
                            param.name = split[2];
                        } else {
                            param.name = selfOrTypeName;
                        }
                        if (split.length > 3) {
                            param.comment = CommonUtils.join(Arrays.copyOfRange(split, 3, split.length), " ");
                        }
                    } else {
                        // 防止没有参数名，只有参数类型
                        if (split.length > 1) {
                            param.name = split[1];
                            if (split.length > 2) {
                                param.comment = CommonUtils.join(Arrays.copyOfRange(split, 2, split.length), " ");
                            }
                        } else {
                            param.name = CommonUtils.toValidTypeName(selfOrTypeName);
                        }
                    }
                    param.type = Type.getType(selfOrTypeName.replace("#", "").trim());
                } else {
                    // 其实就是self了
                    param.name = selfOrTypeName;
                }

                param.name = CommonUtils.toValidName(param.name);


                /*String nameOrSelf = split[split.length - 1];
                param.name = nameOrSelf;
                if (!"self".equals(nameOrSelf)) {
                    // 非self，补充类型
                    // #string backGroundSelected
                    // #function func
                    String typeName = CommonUtils.stringBefore(line, param.name).replace("#", "").trim();
                    Type type = Type.getType(typeName);
                    param.type = type;
                }*/
                function.params.add(param);
            } else if (LineTool.isReturn(line)) {
                // -- @return DrawNode#DrawNode self (return value: cc.DrawNode)
                // -- @return unsigned int#unsigned int ret (return value: unsigned int)
                // -- @return void#void
                // -- @return ret
                String[] cols = CommonUtils.stringAfter(line, Anno.RETURN.getName()).split("#", -1);
                String typeName = cols[0].trim();
                String returnName = null;
                if (!"void".equals(typeName) && cols.length >= 2) {
                    if (cols[1].startsWith("unsigned")) {
                        // unsigned int ret (return value: unsigned int)
                        returnName = cols[1].split(" ")[2];
                    } else {
                        // DrawNode self (return value: cc.DrawNode)
                        returnName = cols[1].split(" ")[1];
                    }
                }

                function.returnType = Type.getType(typeName, returnName);
            } else {
                // 3.10版本的Widget.lua文件的254行有错误的注释
                if (line.trim().equalsIgnoreCase("-- return void") || line.trim().equalsIgnoreCase("--- return void")) {
                    // ignore
                } else {
                    comments.add(processComment(line));
                }
            }
        }
        function.comments = comments;
        // add by clu on 2019-10-8 17:02:49 缺少返回值
        if (function.returnType == null) {
            function.returnType = Type.getType("void");
        }
        clazz.functions.add(function);
    }

    /**
     * 字段声明，主要是类似于：lua_cocos2dx_auto_api.lua文件
     *
     * @param segmentLines
     */
    private void processFieldSegment(List<String> segmentLines) {
        List<String> comments = new ArrayList<>();
        Field field = new Field();
        for (String line : segmentLines) {
            if (LineTool.isField(line)) {
                String[] split = line.split(" ");
                String[] nameAndType = split[3].split("#");
                field.name = nameAndType[0];
                field.type = Type.getType(nameAndType[1]);
            } else {
                comments.add(processComment(line));
            }
        }
        field.comments = comments;
        clazz.fields.add(field);
    }

    private Class getOrCreate(String className) {
        Class clazz = CLASS_POOL.get(className);
        if (clazz == null) {
            clazz = new Class(className);
            CLASS_POOL.put(className, clazz);
        }
        return clazz;
    }


    /**
     * 类转Luadoc
     *
     * @param clazz
     * @return
     */
    public String classToString(Class clazz) {
        List<String> lines = new ArrayList<>();

        if (CommonUtils.hasText(clazz.namePrefix)) {
            // 普通类
            // cc = cc or {}
            lines.add(String.format("%s = %s or {}", clazz.namePrefix, clazz.namePrefix));

            // 头部声明
            // lines.add("");
            // --- Director object
            lines.add(String.format("---%s object", clazz.name));

            // --- @class Director : Object
            if (clazz.superClass != null) {
                lines.add(String.format("---@class %s : %s", clazz.name, clazz.superClass.name));
            } else {
                lines.add(String.format("---@class %s", clazz.name));
            }

            // local Director = {}
            lines.add(String.format("local %s = {}", clazz.name));

            // cc.Director = Director
            // lines.add(String.format("%s.%s = {}", clazz.namePrefix, clazz.name));
            lines.addAll(clazz.comments);
            lines.add(String.format("%s.%s = %s", clazz.namePrefix, clazz.name, clazz.name));
        } else {
            // cc的声明
            // cc = cc or {}
            lines.add(String.format("%s = %s or {}", clazz.name, clazz.name));
        }
        lines.add("");


        // 字段
        for (Field field : clazz.fields) {
            // ---@field public used System.Boolean
            lines.addAll(field.comments);
            lines.add(String.format("---@field public %s %s", field.name, field.type.name));
            lines.add("");
        }

        // 方法
        for (Function function : clazz.functions) {
            lines.addAll(function.comments);
            /*
            ---
            --- Looks for the first match of `pattern` in the string `s`. If it finds a
            --- match, then `find` returns the indices of `s` where this occurrence starts
            --- and ends; otherwise, it returns nil. A third, optional numerical argument
            --- `init` specifies where to start the search; its default value is 1 and
            --- can be negative. A value of true as a fourth, optional argument `plain`
            --- turns off the pattern matching facilities, so the function does a plain
            --- "find substring" operation, with no characters in `pattern` being considered
            --- "magic". Note that if `plain` is given, then `init` must be given as well.
            --- If the pattern has captures, then in a successful match the captured values
            --- are also returned, after the two indices.
            ---@overload fun(s:string, pattern:string):number, number
            ---@overload fun(s:string, pattern:string, init:number):number, number
            ---@param s string
            ---@param pattern string
            ---@param init number
            ---@param plain boolean
            ---@return number, number
            function string.find(s, pattern, init, plain) end
            */
            for (OverrideFunction overrideFunction : function.overloads) {
                // ---@overload fun(s:string, pattern:string):number, number

                // add by clu on 2018-8-15 21:36:11 忽略overload中的自己
               /* if (overrideFunction.params.size() == function.params.size()) {
                    System.out.println(String.format("function %s:%s(%s)", clazz.name, function.name, CommonUtils.join(overrideFunction.params,
                            ", ", param -> {
                                if ("boolean".equals(param.type.name)) {
                                    return "bool:boolean";
                                } else {
                                    return param.type.name;
                                }
                            })));
                    continue;
                }*/

                // 重载的时候，使用类型名代替变量名
                List<Param> params = Main.WITH_SELF_PARAM ? overrideFunction.params
                        : overrideFunction.params.stream().filter(param -> !"self".equals(param.type.name)).collect(Collectors.toList());

                lines.add(String.format("---@overload fun(%s):%s",
                        // 方法列表
                        CommonUtils.join(params,
                                ", ", param -> {
                                    if ("boolean".equals(param.type.name)) {
                                        return "bool:boolean";
                                    } else {
                                        // FIXME overload重载的方法参数缺参数名
                                        return param.type.name;
                                    }
                                }),
                        // 返回值类型
                        processReturnType(function.returnType, clazz, function).name)
                );
            }

            for (Param param : function.params) {
                // ---@param pattern string

                if (!Main.WITH_SELF_PARAM && param.name.equals("self")) {
                    continue;
                }

                if (param.name.equals("self")) {
                    param.type = Type.getType(clazz.name);
                }

                String line = null;
                if (param.type != null) {
                    line = String.format("---@param %s %s", CommonUtils.toSafeName(param.name), param.type.name);
                } else {
                    line = String.format("---@param %s", CommonUtils.toSafeName(param.name));
                }
                // 方法注释
                if (param.comment != null) {
                    line += " ";
                    if (!param.comment.startsWith("@")) {
                        line += "@";
                    }
                    line += param.comment;
                }
                lines.add(line);
            }

            // ---@return number, number
            lines.add(String.format("---@return %s", processReturnType(function.returnType, clazz, function).name));

            // function Director:getInstance() end
            // edit by clu on 2018-4-9 09:32:21 修复方法名是end导致解析错误的问题
            function.name = CommonUtils.toSafeName(function.name);

            List<Param> params = Main.WITH_SELF_PARAM ? function.params
                    : function.params.stream().filter(param -> !param.name.equals("self")).collect(Collectors.toList());
            if (!function.params.isEmpty() && function.params.get(0).name.equals("self")) {
                lines.add(String.format("function %s:%s(%s) end", clazz.name, function.name,
                        CommonUtils.join(params, ", ", param -> CommonUtils.toSafeName(param.name))));
            } else {
                lines.add(String.format("function %s.%s(%s) end", clazz.name, function.name,
                        CommonUtils.join(params, ", ", param -> CommonUtils.toSafeName(param.name))));
            }

            lines.add("");
        }

        if (CommonUtils.hasText(clazz.namePrefix)) {
            lines.add(String.format("return %s", clazz.name));
        }

        return CommonUtils.join(lines, "\n");
    }

    private Type processReturnType(Type type, Class clazz, Function function) {
        if (type == null) {
            type = Type.getType("void");
        } else if (type.isFunctionReturnSelf(clazz, function)) {
            return Type.getType(clazz.name);
        }
        return type;
    }

    private String processComment(String commentLine) {
        if (commentLine != null) {
            if (commentLine.startsWith("--") && !commentLine.startsWith("---")) {
                commentLine = "-" + commentLine;
            } else {
                commentLine = "---" + commentLine;
            }
            return commentLine;
        } else {
            return "";
        }
    }

    /**
     * 将旧的格式转换为新的
     * @return
     */
    public String toEmmyLuaString() {
        if (clazz == null) {
            toClass();
        }
        return classToString(clazz);
    }


    public Class getClazz() {
        return clazz;
    }
}
