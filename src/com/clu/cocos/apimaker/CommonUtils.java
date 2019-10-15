package com.clu.cocos.apimaker;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class CommonUtils {

    /**
     * 某个特殊字符后面的内容
     *
     * @param source
     * @param stringToFind
     * @return
     */
    public static String stringAfter(String source, String stringToFind) {
        int index = source.indexOf(stringToFind);
        if (index > -1) {
            return source.substring(index + stringToFind.length());
        } else {
            return null;
        }
    }

    /**
     * 某个特殊字符后面的内容，最后一次出现的后面
     * @param source
     * @param stringToFind
     * @return
     */
    public static String stringAfterLast(String source, String stringToFind) {
        int index = source.lastIndexOf(stringToFind);
        if (index > -1) {
            return source.substring(index + stringToFind.length());
        } else {
            return null;
        }
    }

    /**
     * 某个特殊字符前面的
     *
     * @param source
     * @param stringToFind
     * @return
     */
    public static String stringBefore(String source, String stringToFind) {
        int index = source.indexOf(stringToFind);
        if (index > -1) {
            return source.substring(0, index);
        } else {
            return null;
        }
    }

    /**
     * 将字符串数组合并为一个字符串
     * @param array
     * @param seperator
     * @return
     */
    public static String join(String[] array, String seperator) {
        if (array == null || array.length == 0) {
            return "";
        } else {
            StringBuilder builder = new StringBuilder();
            for (String s : array) {
                builder.append(s).append(seperator);
            }
            // 删除最后一个seperator
            return builder.substring(0, builder.length() - seperator.length());
        }
    }

    public static String toSafeName(String name) {
        if ("end".equals(name)) {
            return "end_";
        } else if ("repeat".equals(name)) {
            return "repeat_";
        } else {
            return name;
        }
    }

    public static String firstCharToLower(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    public static String toValidTypeName(String name) {
        Objects.requireNonNull(name);
        if (name.contains("cc.Texture2D::_TexParams")) {
            name = "cc_Texture2D_TexParams";
        } else if (name.contains(".")) {
            name = Objects.requireNonNull(stringAfter(name, "."));
            // 第一个字母小写
            return firstCharToLower(name);
        }
        return name;
    }

    public static String toValidName(String name) {
        return toSafeName(name).replace("#", "");
    }

    public static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public static <T> String join(Collection<T> lines, String seperator) {
        return join(lines, seperator, line -> line.toString());
    }

    public static <T> String join(Collection<T> lines, String seperator, Function<T, String> function) {
        StringBuilder builder = new StringBuilder();
        Iterator<T> iterator = lines.iterator();
        if (iterator.hasNext()) {
            while (true) {
                builder.append(function.apply(iterator.next()));
                if (iterator.hasNext()) {
                    builder.append(seperator);
                } else {
                    break;
                }
            }
        }
        return builder.toString();
    }

    public static List<String> readAllLines(File file) {
        BufferedReader reader = null;
        try {
            List<String> lines = new ArrayList<>();
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
    }

    public static void writeFile(File file, String content) {
        FileOutputStream outputStream = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file);
            outputStream.write(content.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
