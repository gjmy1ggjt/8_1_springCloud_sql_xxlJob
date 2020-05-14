package com.example.demo.utils;

import java.util.List;

/**
 * Created by Administrator on 2020/1/23.
 */
public class ListUtil {

    public static boolean isEmpty(List list) {

        if (list != null && list.size() > 0) {

            return false;

        } else {

            return true;
        }
    }

    public static boolean isNotEmpty(List list) {
        return !isEmpty(list);
    }


}
