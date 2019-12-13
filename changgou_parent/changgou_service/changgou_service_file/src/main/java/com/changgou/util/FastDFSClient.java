package com.changgou.util;

import com.changgou.file.FastDFSFile;
import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;
import org.springframework.core.io.ClassPathResource;

import java.io.*;

public class FastDFSClient {

    /***
     * 初始化tracker信息
     */
    static {
        try {
            //获取tracker的配置文件fdfs_client.conf的位置
            String filePath = new ClassPathResource("fdfs_client.conf").getPath();
            //加载tracker配置信息
            ClientGlobal.init(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static TrackerServer getTrackerServer(){
        TrackerServer trackerServer=null;
        try {
            //创建一个TrackerCilent对象，直接new
            TrackerClient trackerClient=new TrackerClient();
            //使用client对象创建连接，获得trackerServer对象
            trackerServer=trackerClient.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trackerServer;
    }

    public static StorageClient getStorageClient(){
        StorageClient storageClient=new StorageClient(getTrackerServer(),null);
        return storageClient;
    }


    /****
     * 文件上传
     * @param fastDFSFile : 要上传的文件信息封装->FastDFSFile
     * @return String[]
     *          1:文件上传所存储的组名
     *          2:文件存储路径
     */
    public static String[] upload(FastDFSFile fastDFSFile){
        //uploadResults[0]是文件所存储的组名
        //uploadResults[1]是文件所存储的路径
        String[] uploadResults=null;
        try {
            //获得作者信息
            NameValuePair[] meta_list=new NameValuePair[1];
            meta_list[0]=new NameValuePair("作者",fastDFSFile.getAuthor());
            //执行文件上传
            uploadResults = getStorageClient().upload_file(fastDFSFile.getContent(), fastDFSFile.getExt(), meta_list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uploadResults;
    }


    //获取文件信息，groupname组名，remote——filename文件路径
    public static FileInfo getFileInfo(String group_name,String remote_filename){
        try {
            FileInfo info=getStorageClient().get_file_info(group_name,remote_filename);
            return info;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        return null;
    }


    //下载文件
    public static InputStream downloadFile(String groupname,String remotefilename){
        try {
            byte[] bytes = getStorageClient().download_file(groupname, remotefilename);
            return new ByteArrayInputStream(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        downloadFileTest();
    }

    //文件下载测试
    private static void downloadFileTest() {
        InputStream is=downloadFile("group1","M00/00/00/wKjThF1tEhmAINQPAAdkdgIofZw992.jpg");
        try {
            OutputStream os = new FileOutputStream("D:/a.jpg");
            byte[] bytes=new byte[1024];

            while(is.read(bytes)>-1){
                os.write(bytes);
            }
            os.close();
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //删除文件
    public static void deleteFile(String groupname,String remotefilename){
        try {
            getStorageClient().delete_file(groupname,remotefilename);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
    }

    //获取组信息
    public static StorageServer getStorageServer(String groupName){
        try {
            TrackerClient trackerClient=new TrackerClient();//获取TrackerClient对象
            TrackerServer trackerServer=trackerClient.getConnection();//通过TrackerClient获取连接得到TrackerServer
            StorageServer storageServer=trackerClient.getStoreStorage(trackerServer,groupName);//获取组信息
            return storageServer;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    //获取全部组信息
    public static ServerInfo[] getStorageServerInfo(String groupName,String remotefilename){
        try {
            TrackerClient trackerClient=new TrackerClient();//获取TrackerClient对象
            TrackerServer trackerServer=trackerClient.getConnection();//通过TrackerClient获取连接得到TrackerServer
            ServerInfo[] fetchStorages = trackerClient.getFetchStorages(trackerServer, groupName, remotefilename);//获取组信息
            return fetchStorages;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String getTrackerUrl(){
        try {
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getConnection();
            String url="http://"+trackerServer.getInetSocketAddress().getHostString()+":"+ClientGlobal.getG_tracker_http_port()+"/";
            return url;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
