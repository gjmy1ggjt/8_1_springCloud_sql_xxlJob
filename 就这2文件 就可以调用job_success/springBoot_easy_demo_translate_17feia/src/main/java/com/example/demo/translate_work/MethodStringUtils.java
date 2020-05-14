package com.example.demo.translate_work;

import java.io.*;
import java.util.*;

/**
 * Created by Administrator on 2020/2/29.
 */
public class MethodStringUtils {

    //    有 ); 没有{ }
//    是抽象方法 返回 true, 不是 返回 false

    private static boolean isMethodSameLine(String s) {

        if (s.indexOf(Constant.step_left_big) != -1 && s.indexOf(Constant.step_right_big) != -1) {

            return true;

        } else {

            return false;
        }
    }

    private static boolean isAbsMethod(String s) {

        if (s.indexOf(Constant.caseRightType) != -1 && s.indexOf(Constant.point_two) != -1) {
//        if (s.indexOf(Constant.caseRightType) != -1 && s.indexOf(Constant.point_two) != -1 && s.indexOf(Constant.step_left_big) == -1) {

            return true;

        } else {

            return false;
        }
    }

    public static boolean isMethodNoAbsLine(String s) {
//      排除 {} 在同一行的方法
        if (isMethodLine(s) && !isAbsMethod(s) && !isMethodSameLine(s)) {

            return true;

        }

        return false;
    }

    public static boolean isMethodLine(String s) {

        if (StringUtils.isBlank(s)) {

            return false;

        } else {
            if (s.indexOf(Constant.classType) == -1) {

//                存在 {}； 在同一行的方法，所以 不能使用 indexOf(";") == -1;
                return ListUtils.hasString(s, Constant.getWordListName()) && ListUtils.hasString(s, Constant.getCaseName());

            } else {

                return false;
            }
        }
    }

    public static String getMethodName(String s) {

        String methodParam = s.substring(0, s.indexOf(Constant.caseLeftType));
        return methodParam.substring(methodParam.lastIndexOf(Constant.nullType) + 1, methodParam.length()).trim();

    }


    public static List<String> getMethodName(File file) {

        BufferedReader br = null;

        List<String> list = new ArrayList<>();
//       不能排除 重载方法；要排除 方法内部重载方法
        int countMethod = 0;

        try {

            br = new BufferedReader(new FileReader(file));

            String line = null;

            String methodName = "";

            while ((line = br.readLine()) != null) {

                String deMethodName = "";

                line = IgnoreStringUtils.noIgnoreStringTwo(line);

                if (StringUtils.isNotBlank(line)) {

                    line = line.trim();

                    if (line.indexOf(Constant.step_left_big)!= -1) {

                        countMethod ++;
                    }

                    if (line.indexOf(Constant.step_right_big)!= -1) {

                        countMethod --;
                    }

                    if (isMethodNoAbsLine(line)) {

                        deMethodName = getMethodName(line);
//                        排除重载方法名称
                        if (StringUtils.isNotBlank(deMethodName)) {

                            if (countMethod == 2) {

                                list.add(deMethodName);

                            } else {

                                if (line.indexOf(Constant.caseLeftType) != -1 && line.indexOf(Constant.caseRightType) == -1) {

                                    list.add(deMethodName);
                                }
                            }
                        }
                    } else {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return list;
    }

    public static List<List<String>> getMethodLine(File file) {

        List<List<String>> listList = new ArrayList<>();

        List<String> list = new ArrayList<>();

        int countMethod = 0;

        BufferedReader br = null;

        try {

            br = new BufferedReader(new FileReader(file));

            String line = null;

            while ((line = br.readLine()) != null) {

                line = IgnoreStringUtils.noIgnoreStringTwo(line);

//                if (StringUtils.isNotBlank(line)) {
//
//                    line = IgnoreStringUtils.noIgnoreStringOne(line);
//
//                }
                if (StringUtils.isNotBlank(line)) {

                    line = line.trim();

                    if (countMethod > 1) {

                        if (!list.contains(line)) {
                            list.add(line);
                        }
                    } else {

                        if (ListUtils.isNotEmpty(list)) {

                            listList.add(list);

                            list = new ArrayList<>();

                        }
                    }

                    if (line.indexOf(Constant.step_left_big) != -1) {

                        countMethod++;
                    }

                    if (line.indexOf(Constant.step_right_big) != -1) {

                        countMethod--;

                        if (countMethod > 1) {

                            if (!list.contains(line)) {
                                list.add(line);
                            }
                        }
                    }

                }


            }
        } catch (
                Exception e)

        {
            e.printStackTrace();
        } finally

        {
            try {

                br.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return listList;
    }

    public static Map<String, List<String>> getMapFileMethodAndChinese(File file) {

        System.out.println(FileUtils.getClassName(file.getName()));

        Map<String, List<String>> map = new LinkedHashMap<>();

        List<String> listMethodName = getMethodName(file);

        List<List<String>> listChineseLine = getMethodLine(file);

        if (ListUtils.isNotEmpty(listMethodName) && ListUtils.isNotEmpty(listChineseLine) && listMethodName.size() == listChineseLine.size()) {

            for (int i = 0; i < listMethodName.size(); i++) {

                List<String> list = new ArrayList<>();

                String method = listMethodName.get(i);

                List<String> line = listChineseLine.get(i);

                for (String useLine : line) {

                    if (StringUtils.hasChinese(useLine) && !IgnoreStringUtils.ignoreStringLine(useLine)) {

                        list.add(useLine.trim());
                    }
                }

                if (ListUtils.isNotEmpty(list)) {

                    map.put(method, list);
                }
            }
        }
        return map;
    }

    public static void main(String[] args) {

//        String path = "C:\\Users\\Administrator\\Desktop\\springCloud_221\\idea_open\\springBoot_easy_demo_translate_17feia\\controller\\x\\ApiOrderBusiness.java";
        String path = "C:\\Users\\Administrator\\Desktop\\springCloud_221\\idea_open\\springBoot_easy_demo_translate_17feia\\controller\\x\\ApiOrderBusiness.java";
//        String path = "C:\\Users\\Administrator\\Desktop\\springCloud_221\\idea_open\\springBoot_easy_demo_translate_17feia\\controller\\x\\UserController.java";
        File file = new File(path);

        System.out.println(getMapFileMethodAndChinese(file).size());
        System.out.println(getMapFileMethodAndChinese(file).toString());

        ;
    }
}
