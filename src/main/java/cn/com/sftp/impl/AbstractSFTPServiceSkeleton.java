/**
 * Copyright (c) 2012 Conversant Solutions. All rights reserved.
 *
 * Created on 5/10/15.
 */
package cn.com.sftp.impl;

import cn.com.sftp.SFTPService;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created with StoreShare2.0
 * User: huangli@conversant.com.cn
 * Date: 5/10/15
 * Time: 4:07 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract  class AbstractSFTPServiceSkeleton implements SFTPService {

    private String sftpHost;

    private String sftpPort;

    private String sftpUsername;

    private String sftpPrivateKey;

    public String getSftpHost() {
        return sftpHost;
    }

    public void setSftpHost(String sftpHost) {
        this.sftpHost = sftpHost;
    }

    public String getSftpPort() {
        return sftpPort;
    }

    public void setSftpPort(String sftpPort) {
        this.sftpPort = sftpPort;
    }

    public String getSftpUsername() {
        return sftpUsername;
    }

    public void setSftpUsername(String sftpUsername) {
        this.sftpUsername = sftpUsername;
    }

    public String getSftpPrivateKey() {
        return sftpPrivateKey;
    }

    public void setSftpPrivateKey(String sftpPrivateKey) {
        this.sftpPrivateKey = sftpPrivateKey;
    }

    private static final String DEFAULT_CHANNEL = "sftp";


    private static final Logger logger = LoggerFactory.getLogger("FTP_LOG");

    private final ReentrantLock ftpLocker = new ReentrantLock();


    @Override
    public void exec() {
        Session session = null;
        Channel channel = null;


        try {
            ftpLocker.lock();
            session = connectToFtp();
            channel = connectChannel(session);
            final ChannelSftp channelSftp = (ChannelSftp) channel;

            process(channelSftp);


        } catch (SftpException e) {
            logger.error("Open Channel exception for sftpHost={},sftpUsername={},sftpPort={}", sftpHost, sftpUsername, sftpPort, e);

        } catch (JSchException e) {
            logger.error("Open Channel exception for sftpHost={},sftpUsername={},sftpPort={}", sftpHost, sftpUsername, sftpPort, e);
        } finally {

            logger.info("#########Get FTP file End############## ");
            this.closeChannel(channel);
            this.closeFtpSession(session);
            ftpLocker.unlock();
        }


    }


    private Session connectToFtp() throws JSchException {
        logger.info("######begin connecting to ftp sftpHost={},sftpUsername={},sftpPort={}", sftpHost, sftpUsername, sftpPort);
        Session session = null;
        JSch jsch = new JSch();

        jsch.addIdentity(sftpPrivateKey);
        session = jsch.getSession(sftpUsername, sftpHost, Integer.valueOf(sftpPort));
        if (session == null) {
            throw new RuntimeException("session is null");
        }
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        logger.info("Connect to ftp sftpHost={},sftpUsername={},sftpPort={} successful", sftpHost, sftpUsername, sftpPort);
        return session;


    }

    private Channel connectChannel(Session session) throws JSchException {
        if (session == null) {
            throw new JSchException("Session for sftpHost=" + sftpHost + ",sftpUsername=" + sftpUsername + ",sftpPort=" + sftpPort + " is null");
        }
        Channel channel = (Channel) session.openChannel(DEFAULT_CHANNEL);
        channel.connect();
        return channel;


    }


    private void closeFtpSession(Session session) {
        if (session != null) {
            session.disconnect();
        }
    }

    private void closeChannel(Channel channel) {
        if (channel != null) {
            channel.disconnect();
        }
    }


    public abstract void process(ChannelSftp channelSftp) throws SftpException;
}
