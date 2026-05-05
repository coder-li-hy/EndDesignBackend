package com.reggie.reg.utils;

import java.util.Random;

/**
 * 随机生成验证码工具类
 */
public class ValidateCodeUtils {

    /**
     * 生成指定位数的数字验证码
     * @param length 验证码的长度，只能是4或6
     * @return 生成的验证码，类型为Integer
     * @throws RuntimeException 当传入的length不是4或6时抛出异常
     */
    public static Integer generateValidateCode(int length){
        Integer code =null; // 初始化验证码为null
        if(length == 4){ // 如果验证码长度为4
            code = new Random().nextInt(9999);//生成随机数，最大为9999
            if(code < 1000){
                code = code + 1000;//保证随机数为4位数字
            }
        }else if(length == 6){
            code = new Random().nextInt(999999);//生成随机数，最大为999999
            if(code < 100000){
                code = code + 100000;//保证随机数为6位数字
            }
        }else{
            throw new RuntimeException("只能生成4位或6位数字验证码");
        }
        return code;
    }


    /**
     * 生成指定位数的随机验证码字符串
     * @param length 需要生成的验证码长度
     * @return 生成的随机验证码字符串
     */
    public static String generateValidateCode4String(int length){
        // 创建随机数生成器
        Random rdm = new Random();
        // 生成随机整数并转换为十六进制字符串
        String hash1 = Integer.toHexString(rdm.nextInt());
        // 截取指定长度的子字符串作为验证码
        String capstr = hash1.substring(0, length);
        // 返回生成的验证码
        return capstr;
    }
}
