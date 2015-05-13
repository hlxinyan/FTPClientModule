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
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;

/**
 * Created with StoreShare2.0
 * User: huangli@conversant.com.cn
 * Date: 5/10/15
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class PutFileToFTPService extends AbstractSFTPServiceSkeleton {

    /**
     * FTPServer的目录
     * 可以配置多个目录，多个目录之间用都好分开
     */
    private String destFTPPath;

    /**
     * 本地需要被上传文件的目录，
     * 可以配置多个目录，多个目录之间用都好分开
     */
    private String srcFileFolder;

    private static final Logger logger = LoggerFactory.getLogger("PUT_LOGGER");


    public PutFileToFTPService() {

    }

    public PutFileToFTPService(String sftpHost, String sftpPort, String userName, String sftpPrivateKey, String destFTPPath, String srcFileFolder) {
        this.setSftpHost(sftpHost);
        this.setSftpPort(sftpPort);
        this.setSftpUsername(userName);
        this.setSftpPrivateKey(sftpPrivateKey);
        this.srcFileFolder = srcFileFolder;
        this.destFTPPath = destFTPPath;

    }

    @Override
    public void process(ChannelSftp channelSftp) throws SftpException {

        logger.info("begin handle put destFTPPath={},srcFileFolder={}", destFTPPath, srcFileFolder);
        String[] sourceLocalDirs = srcFileFolder.split(",");
        String[] destFtpDirs = destFTPPath.split(",");

        if (sourceLocalDirs.length != destFtpDirs.length) {
            logger.error("Please be sure sourceLocalDirs={} and  destFtpDirs={} with the same length", sourceLocalDirs, destFtpDirs);
            return;
        }
        for (int i = 0; i < sourceLocalDirs.length; i++) {
            process(channelSftp, sourceLocalDirs[i], destFtpDirs[i]);
        }

    }

    private void process(ChannelSftp channelSftp, String sourceLocalDir, String destFtpDir) throws SftpException {
        File file = new File(sourceLocalDir);

        logger.info("begin handle put sourceLocalDir={},destFtpDir={}", sourceLocalDir, destFtpDir);

        if (!file.exists()) {
            throw new SftpException(0, "SourceLocalDir:" + sourceLocalDir + " doesn't exist");
        }
        checkFtpDir(channelSftp, destFtpDir);

        if (file.isFile()) {
            channelSftp.put(sourceLocalDir, destFtpDir + "/" + file.getName());
            return;
        }

        File[] files = file.listFiles();
        for (File f : files) {
            String localFile = sourceLocalDir + "/" + f.getName();
            String remoteFtpFile = destFtpDir + "/" + f.getName();
            logger.info("upload localFile={},remoteFtpFile={}", localFile, remoteFtpFile);
            if (f.isDirectory()) {
                process(channelSftp, localFile, remoteFtpFile);
            } else {

                channelSftp.put(localFile, remoteFtpFile);

                moveToFinished(localFile);
            }
        }
    }


    private void checkFtpDir(ChannelSftp channelSftp, String destFtpDir) throws SftpException {

        logger.info("check remote ftp dir={}", destFtpDir);
        try {
            SftpATTRS sftpATTRS = channelSftp.stat(destFtpDir);
        } catch (Exception e) {
            logger.info("mkdir remote ftp dir={}", destFtpDir);
            channelSftp.mkdir(destFtpDir);
        }


    }

    private void moveToFinished(String localFile) {
        File local = new File(localFile);
        File parents = local.getParentFile().getParentFile();
        File finishedFile = new File(parents.getAbsolutePath() + "/finished");
        if (!finishedFile.exists()) {
            finishedFile.mkdirs();
        }

        File dest = new File(finishedFile.getAbsolutePath() + "/" + local.getName());

        FileUtil.rename(local, dest);

        logger.info("Move to finished folder={}", localFile);
    }

    public String getDestFTPPath() {
        return destFTPPath;
    }

    public void setDestFTPPath(String destFTPPath) {
        this.destFTPPath = destFTPPath;
    }

    public String getSrcFileFolder() {
        return srcFileFolder;
    }

    public void setSrcFileFolder(String srcFileFolder) {
        this.srcFileFolder = srcFileFolder;
    }
}
