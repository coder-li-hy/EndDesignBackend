package com.reggie.reg;

import org.junit.jupiter.api.Test;

import java.util.UUID;

public class UploadTest {
    @Test
    public void test1() {
        String fileName = "ewewewe.jpg";

        //获取原始文件名称
        String suffix=fileName.substring(fileName.lastIndexOf("."));
        //使用uuid重新生成文件名，防止文件名重复造成文件名覆盖
        String filename = UUID.randomUUID().toString()+suffix;
        System.out.println(filename);
    }
}
