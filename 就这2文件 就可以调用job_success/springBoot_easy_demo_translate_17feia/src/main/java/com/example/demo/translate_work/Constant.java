package com.example.demo.translate_work;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2020/2/28.
 */
public class Constant {

    public static String controller = "controller";

    public static String service = "service";

    public static String impl = "impl";

    public static String business = "business";

    public static String step_right_two = "\\";

    public static String step_left_two = "//";

    public static String step_left_big = "{";

    public static String step_right_big = "}";

    public static String point = "\\.";

    public static String point_two = ";";

    public static String step_left_pass = "/*";

    public static String step_right_pass = "*/";

    public static String classType = "class";

    public static String javaType = "java";

    public static String publicType = "public";

    public static String privateType = "private";

    public static String protectedType = "protected";

    public static String nullType = " ";

    public static String caseLeftType = "(";

    public static String caseRightType = ")";

    public static String ignoreLogger = "logger";

    public static String ignoreLog = "log";

    public static String ignoreApi = "Api";

    public static String override = "@Override";

    public static String ignoreWordOne = "* ";

    public static List<String> ignoreList() {

        List<String> list = new ArrayList<>();

        list.add(ignoreLogger);

        list.add(ignoreWordOne);

        list.add(ignoreLog);

        list.add(ignoreApi);

        return list;

    }

    public static List<String> getCaseName() {
        List<String> list = new ArrayList<>();

        list.add(caseRightType);

        list.add(caseLeftType);

        return list;

    }

    public static List<String> getWordListName() {

        List<String> list = new ArrayList<>();
        list.add(publicType);
        list.add(privateType);
        list.add(protectedType);

        return list;
    }

    public static List<String> getDirListName() {

        List<String> list = new ArrayList<>();

        list.add(controller);
        list.add(service);
        list.add(business);
        list.add(impl);

        return list;
    }


}
