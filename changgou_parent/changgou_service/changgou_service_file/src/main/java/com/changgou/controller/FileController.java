package com.changgou.controller;

import com.changgou.file.FastDFSFile;
import com.changgou.util.FastDFSClient;
import entity.Result;
import entity.StatusCode;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
public class FileController {

    @RequestMapping("/upload")
    public Result upload(MultipartFile file) throws Exception {
        //包装fastdfs上传文件对象
        FastDFSFile fastDFSFile=new FastDFSFile(
                file.getOriginalFilename(),//原来文件名
                file.getBytes(),//获取字节数组
                StringUtils.getFilenameExtension(file.getOriginalFilename())//获取后缀名
        );
        //上传文件
        String[] upload = FastDFSClient.upload(fastDFSFile);
        //拼接图片返回url返回结果
        String url=FastDFSClient.getTrackerUrl()+upload[0]+"/"+upload[1];
        return new Result(true, StatusCode.OK,url);
    }
}
