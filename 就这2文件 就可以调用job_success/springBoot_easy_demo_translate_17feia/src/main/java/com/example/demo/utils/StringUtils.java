package com.example.demo.utils;

/**
 * Created by Administrator on 2020/1/23.
 */
public class StringUtils {

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
