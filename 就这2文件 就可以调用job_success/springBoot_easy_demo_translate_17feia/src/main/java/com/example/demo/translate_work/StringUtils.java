package com.example.demo.translate_work;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2020/2/29.
 */
public class StringUtils {

    public static boolean hasChinese(String str) {

        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");

        Matcher m = p.matcher(str);

        if (m.find()) {
            return true;
        }
        return false;
    }

    public static boolean isNotBlank(String s) {

        if (s == null || s.length() <= 0) {

            return false;

        } else {

            return true;
        }
    }

    public static boolean isBlank(String s) {
        return !isNotBlank(s);
    }


}
