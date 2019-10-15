package com.clu.cocos.apimaker;

import java.util.List;

public class SegmentTool {
    public static boolean isModuleSegment(List<String> lines) {
        return lines.stream().anyMatch(line -> LineTool.isModule(line));
    }

    public static boolean isFunctionSegment(List<String> lines) {
        return lines.stream().anyMatch(line -> LineTool.isFunction(line));
    }

    public static boolean isFieldSegment(List<String> segmentLines) {
        return segmentLines.stream().anyMatch(line -> LineTool.isField(line));
    }
}
