package com.example.demo.translate_work;

/**
 * @Description 注释帮助类
 * @Date Administrator 2020/2/29
 * @Param
 */
public class IgnoreStringUtils {

    private static int countPassTwo = 0;

    //1  单选注释：符号是：//
//2、块注释： 符号是： /* */ 可以跨多行
//3、javadoc注释： 符号是： /** */ 可以跨多行，
    public static String noIgnoreStringOne(String str) {

//       1 // something  ;  2 something  // 中文

        if (str.indexOf(Constant.step_left_two) != -1) {

            if (str.startsWith(Constant.step_left_two)) {

                return null;

            } else {

                return str.substring(0, str.indexOf(Constant.step_left_two)).trim();

            }

        } else {

            return str;
        }
    }

    public static String getUseChineseString(String str) {

        String useLine = IgnoreStringUtils.noIgnoreStringTwo(str);

        if (StringUtils.isNotBlank(useLine)) {

            if (StringUtils.hasChinese(useLine) && !IgnoreStringUtils.ignoreStringLine(useLine)) {

                return useLine;
            }
        }

        return "";
    }

    public static String noIgnoreStringTwo(String str) {

        if (isNoIgnoreStringTwoSameLine(str)) {

            return noIgnoreStringSameLineTwo(str);

        } else {

            if (noIgnoreStringTwoLeft(str)) {

                countPassTwo++;

                return noIgnoreStringSameLineTwo(str);

            } else {

                if (noIgnoreStringTwoRight(str)) {

                    countPassTwo--;

                    return "";

                } else {

                    if (countPassTwo == 0) {

                        return noIgnoreStringOne(str);

                    } else {

                        return "";
                    }
                }
            }
        }
    }

    public static boolean ignoreStringLine(String str) {

        for (String ignore : Constant.ignoreList()) {

            if (str.indexOf(ignore) != -1) {

                return true;
            }
        }

        return false;

    }

    private static String noIgnoreStringSameLineTwo(String str) {

//       1 // something  ;  2 something  // 中文

        if (str.indexOf(Constant.step_left_pass) != -1) {

            if (str.startsWith(Constant.step_left_pass)) {

                return null;

            } else {

                return str.substring(0, str.indexOf(Constant.step_left_pass)).trim();

            }

        } else {

            return str;
        }
    }

    private static Boolean noIgnoreStringTwoLeft(String str) {

        //2、块注释： 符号是： /* */ 可以跨多行
//3、javadoc注释： 符号是： /**  */ 可以跨多行，
        if (str.indexOf(Constant.step_left_pass) != -1) {

            return true;

        } else {

            return false;
        }

    }

    private static Boolean noIgnoreStringTwoRight(String str) {

        //2、块注释： 符号是： /* */ 可以跨多行
//3、javadoc注释： 符号是： /**  */ 可以跨多行，
        if (str.indexOf(Constant.step_right_pass) != -1) {

            return true;

        } else {

            return false;
        }

    }

    //    /* */ 在同一行  和 不在同一行 两种情况
    private static Boolean isNoIgnoreStringTwoSameLine(String str) {

        if (noIgnoreStringTwoLeft(str) && noIgnoreStringTwoRight(str)) {

            return true;

        } else {

            return false;
        }
    }


    public static void main(String[] args) {

        String one_1 = "  235353;  // ewjgjleakeg ";

        String one_2 = "   // ewjgjleakeg ";

//        System.out.println(noIgnoreStringOne(one_1));
//
//        System.out.println(noIgnoreStringOne(one_2));

        String two_1 = " /** dsfds */  ";

        String two_2 = " dfsfds /** dsfds */  ";

        System.out.println(noIgnoreStringSameLineTwo(two_1));

        System.out.println(noIgnoreStringSameLineTwo(two_2));


    }

}
