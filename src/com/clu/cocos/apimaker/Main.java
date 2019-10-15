package com.clu.cocos.apimaker;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class Main {

    public static String ROOT_PATH = "F:\\api";
    /**
     * 生成的方法是否包含self参数
     */
    public static final boolean WITH_SELF_PARAM = false;

    private static final Map<CocosdocMaker, File> CLASS_AND_OUTPUT_FILE = new LinkedHashMap<>();

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0 || args[0] == null || args[0].trim().length() == 0) {
            JOptionPane.showMessageDialog(null, "请将原目录拖放到此程序上！");
            return;
        } else {
            ROOT_PATH = args[0];
        }

        File sourceFolder = new File(ROOT_PATH);
        File outputFolder = new File(sourceFolder.getParent(), sourceFolder.getName() + "-output");
        try {
            if (!outputFolder.exists()) {
                if (!outputFolder.mkdirs()) {
                    throw new Exception("输出目录创建失败！");
                }
            }

            for (File file : Objects.requireNonNull(sourceFolder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().toLowerCase().endsWith(".lua");
                }
            }))) {
                process(outputFolder, file);
            }
            output();
            if (!"PC-CLU".equals(System.getenv("USERDOMAIN_ROAMINGPROFILE"))) {
                JOptionPane.showMessageDialog(null, "处理完毕，文件输出到：" + outputFolder.getAbsolutePath());
            } else {
                System.out.println("处理完毕，文件输出到：" + outputFolder.getAbsolutePath());
            }
        } catch (Throwable t) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            t.printStackTrace(printWriter);
            JOptionPane.showMessageDialog(null, "错误：\n" + stringWriter.toString());
            t.printStackTrace();
        }

    }

    private static void process(File outputFolder, File file) throws Exception {
        List<String> lines = CommonUtils.readAllLines(file);
        CocosdocMaker maker = new CocosdocMaker(lines);
        // 构建类信息，生成类的层级关系
        maker.toClass();
        File outputFile = new File(outputFolder, file.getName());
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }
        CLASS_AND_OUTPUT_FILE.put(maker, outputFile);
    }

    private static void output() {
        for (Map.Entry<CocosdocMaker, File> entry : CLASS_AND_OUTPUT_FILE.entrySet()) {
            CocosdocMaker maker = entry.getKey();
            File outputFile = entry.getValue();
            // 此处获取时，父类已经构建好，所以可以获取到
            if (maker.getClazz() == null) {
                System.out.println(String.format("%s 文件无法获取到类信息！", outputFile.getName()));
                continue;
            }

            addCommon(maker.getClazz());
            String content = maker.toEmmyLuaString();
            CommonUtils.writeFile(outputFile, content);
        }
    }

    /**
     * 统一对类做修改，将父类的方法增加到子类中，主要是返回值是self的，EmmyLua无法智能判断self
     * @param clazz
     */
    private static void addCommon(Class clazz) {
        if (instanceOf(clazz, "Ref")) {
            // 目前仅仅对Node的子节点做返回值为self的方法的继承
            Stack<Class> stack = new Stack<>();
            Class currentClass = clazz.superClass;
            while (currentClass != null) {
                stack.push(currentClass);
                currentClass = currentClass.superClass;
            }

            // 自己有的方法
            List<Function> declaredFunctions = new ArrayList<>(clazz.functions);

            while (!stack.isEmpty()) {
                Class superClass = stack.pop();
                superClass.functions.forEach(superClassFunction -> {
                    // 父类方法的返回值类型是父类表示返回的额是self
                    if (superClassFunction.returnType.name.equals(superClass.name) || "self".equals(superClassFunction.returnType.returnName)) {
                        // 父类的构造方法不要
                        if (superClassFunction.name.equals(superClass.name)) {
                            // System.out.println("跳过添加父类构造方法：" + superClass.name + ":" + superClassFunction + "到："+ clazz.name +"类");
                        } else {
                            if (!declaredFunctions.stream().anyMatch(df -> df.name.equals(superClassFunction.name))) {
                                // type是复用的。。直接修改会导致其它地方用到这个类型的都有问题，所以需要明确是什么类的什么方法的返回值是self
                                superClassFunction.returnType.setFunctionReturnSelf(clazz, superClassFunction, true);
                                // 防止重复方法，父类的父类和父类同时重写方法，则以父类的为准
                                clazz.functions.removeIf(tf -> tf.name.equals(superClassFunction.name));
                                clazz.functions.add(superClassFunction);
                            }
                        }
                    }
                });
            }
        }
    }

    private static boolean instanceOf(Class clazz, String superClassName) {
        while (clazz.superClass != null) {
            if (clazz.superClass.name.equals(superClassName)) {
                return true;
            }
            clazz = clazz.superClass;
        }
        return false;
    }

}
