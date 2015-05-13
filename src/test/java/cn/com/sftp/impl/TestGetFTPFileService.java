/**
 * Copyright (c) 2012 Conversant Solutions. All rights reserved.
 *
 * Created on 5/13/15.
 */
package cn.com.sftp.impl;

import cn.com.sftp.SFTPService;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with StoreShare2.0
 * User: huangli@conversant.com.cn
 * Date: 5/13/15
 * Time: 6:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestGetFTPFileService {

    private SFTPService sftpService;

    @Before
    public  void setUp(){
        String host="sftp.celcom.com.my" ;
        String port="22";
        String userName="07714";
        String privateKey="E:\\software\\SecureCRTSecureFX_x64_6.7.5.411_PortableSoft\\SecureCRSecureFXPortable\\Data\\Settings\\Config\\putty\\putty\\accelionsftp.ppk";
         String sftpRootDir="Share/Escape/DryRun/InfiWifi,Share/Escape/DryRun/FirstOne";
        String sftpLocalDir="E:/opt/storage-scms/escape/DryRun/InfiWifi,E:/opt/storage-scms/escape/DryRun/FirstOne";

        sftpService=new GetFTPFileService(host,port,userName,privateKey,sftpRootDir,sftpLocalDir);
    }
    @Test
    public void testGetFtpFile(){

        sftpService.exec();

    }
}
