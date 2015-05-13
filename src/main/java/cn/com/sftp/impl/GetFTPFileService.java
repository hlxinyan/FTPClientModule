/**
 * Copyright (c) 2012 Conversant Solutions. All rights reserved.
 *
 * Created on 5/10/15.
 */
package cn.com.sftp.impl;

import cn.com.conversant.commons.file.FileUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created with StoreShare2.0
 * User: huangli@conversant.com.cn
 * Date: 5/10/15
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetFTPFileService extends AbstractSFTPServiceSkeleton {
    /**
     *每个远程FTP目录对应一个本地目录，该目录用于存放从FTP Server上下载下来的文件
     * 允许配置多个目录，每个目录之间用都好分隔。
     * 目录的长度需要和 rootFtpDir长度保持一致
     * 如果是用 Spring来加载该类，则可以用set属性的方法初始化该值
     **/
    private String sftpLocalRoot;

    /**
     *连接到FTP Server后，要cd 进去的目录，
     * 允许配置多个目录，每个目录之间用都好分隔。
     * 目录的长度需要和 sftpLocalRoot目录保持一致
     * 如果是用 Spring来加载该类，则可以用set属性的方法初始化该值
     **/
    private String rootFtpDir;

    private static final Logger logger = LoggerFactory.getLogger("FTP_LOG");


    private CopyOnWriteArraySet<String> hashSet = new CopyOnWriteArraySet<>();

    private static final String DOWNLOAD_FILE = "download.txt";

    private ReentrantLock reentrantLock = new ReentrantLock();

    public GetFTPFileService() {

    }

    public GetFTPFileService(String sftpHost,String sftpPort,String userName,String sftpPrivateKey,String rootFtpDir,String sftpLocalRoot) {
        this.setSftpHost(sftpHost);
        this.setSftpPort(sftpPort);
        this.setSftpUsername(userName);
        this.setSftpPrivateKey(sftpPrivateKey);
        this.sftpLocalRoot = sftpLocalRoot;
        this.rootFtpDir = rootFtpDir;
        init();
    }


    public void init() {
        hashSet.clear();
        initHaveDownloadedFileMap(hashSet);
    }

    @Override
    public void process(ChannelSftp channelSftp) throws SftpException {
        // Share/Escape
        String sftpRootDir = rootFtpDir;

        String[] rootDirs = sftpRootDir.split(",");
        String[] sftpLocalRootDirs = sftpLocalRoot.split(",");

        if (rootDirs.length != sftpLocalRootDirs.length) {
            logger.error("Please be sure sftpRootDir={} and  sftpLocalRootDirs={} with the same length", sftpRootDir, sftpLocalRoot);
            return;
        }
        for (int i = 0; i < rootDirs.length; i++) {
            process(channelSftp, rootDirs[i], sftpLocalRootDirs[i]);
        }
    }


    private void initLocalDir(String localDir) {
        logger.debug("localDir={}", localDir);
        File dir = new File(localDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }


    private void process(ChannelSftp channelSftp, Vector lsEntryVector, String parentRootFolder, String localRootFolder) throws SftpException {
        if (lsEntryVector == null) return;
        for (int i = 0; i < lsEntryVector.size(); i++) {
            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) lsEntryVector.get(i);
            SftpATTRS sftpATTRS = entry.getAttrs();
            String fileName = entry.getFilename();

            if (".".equalsIgnoreCase(fileName) || "..".equalsIgnoreCase(fileName)) {
                continue;
            }

            String parentRootFolder1 = parentRootFolder + "/" + fileName;

            String localRootFolder1 = localRootFolder + "/" + fileName;

            logger.info("Find remoteFile:{}  ", parentRootFolder1);

            if (sftpATTRS.isDir()) {

                process(channelSftp, parentRootFolder1, localRootFolder1);
            } else {

                boolean downloaded = hashSet.contains(parentRootFolder1);


                if (downloaded) {


                    logger.info("Ignore File: {} for having been downloaded", parentRootFolder1);
                    continue;
                } else {
                    //todo:consider async
                    //download file to the local path
                    // logger.info("local pwd={}", channelSftp.lpwd());
                    logger.info("Download filename={} to lpwd={}", parentRootFolder1, channelSftp.lpwd());

                    String tempName = localRootFolder1 + ".temp";
                    channelSftp.get(parentRootFolder1, tempName);
                    //rename
                    FileUtil.rename(tempName, localRootFolder1);

                    updateDownloadFileSet(parentRootFolder1);
                }


            }


        }

    }


    private void process(ChannelSftp channelSftp, String parentRootFolder, String localRootFolder) throws SftpException {

        this.initLocalDir(localRootFolder);

        logger.info("Enter parentRootFolder={},localRootFolder={}", parentRootFolder, localRootFolder);

        // channelSftp.lcd(localRootFolder);

        Vector lsEntryVector = channelSftp.ls(parentRootFolder);

        process(channelSftp, lsEntryVector, parentRootFolder, localRootFolder);

    }

    private void initHaveDownloadedFileMap(CopyOnWriteArraySet<String> set) {
        if (set == null) set = new CopyOnWriteArraySet<String>();


        URL url = Thread.currentThread().getContextClassLoader().getResource(DOWNLOAD_FILE);

        InputStream inputStream = null;
        BufferedReader fileReader = null;

        try {
            inputStream = url.openStream();
            fileReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = fileReader.readLine()) != null) {
                String trim = line.trim();
                set.add(trim);

            }
        } catch (Exception e) {
            logger.error("parse download.txt error", e);
        } finally {
            IOUtils.closeQuietly(fileReader);
            IOUtils.closeQuietly(inputStream);
        }


    }

    private void updateDownloadFileSet(String realFileName) {
        hashSet.add(realFileName);

        URL url = Thread.currentThread().getContextClassLoader().getResource(DOWNLOAD_FILE);

        String path = url.getPath();

        RandomAccessFile randomAccessFile = null;
        try {
            reentrantLock.lock();
            hashSet.add(realFileName);
            File file = new File(path);
            randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.skipBytes((int) file.length());
            randomAccessFile.write(realFileName.getBytes());
            randomAccessFile.write("\n".getBytes());

        } catch (Exception e) {
            logger.error("add to file exception", e);

        } finally {
            IOUtils.closeQuietly(randomAccessFile);
        }


    }

    public String getSftpLocalRoot() {
        return sftpLocalRoot;
    }

    public void setSftpLocalRoot(String sftpLocalRoot) {
        this.sftpLocalRoot = sftpLocalRoot;
    }

    public String getRootFtpDir() {
        return rootFtpDir;
    }

    public void setRootFtpDir(String rootFtpDir) {
        this.rootFtpDir = rootFtpDir;
    }
}
