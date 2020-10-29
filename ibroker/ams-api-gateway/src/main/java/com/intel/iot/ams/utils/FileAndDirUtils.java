/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.utils;

import java.io.*;
import java.util.zip.GZIPInputStream;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.springframework.web.multipart.MultipartFile;

public class FileAndDirUtils {

  /**
   * Save the uploaded application package on to local disk
   *
   * @param file the instance represent the file
   * @param path the path to be saved on the local disk
   * @param filename the file name to be saved on the local disk
   */
  public static void saveFile(MultipartFile file, String path, String filename) throws IOException {

    File f = new File(path);
    // if the repository path is not existed, then create it.
    if (!f.exists()) {
      f.mkdirs();
    }

    FileUtils.copyInputStreamToFile(file.getInputStream(), new File(path + filename));
  }

  /**
   * Rename a file
   *
   * @param path the path of the file
   * @param oldname the old name of the file
   * @param newname the new name of the file
   */
  public static void renameFile(String path, String oldname, String newname) {
    if (!oldname.equals(newname)) {
      File oldfile = new File(path + "/" + oldname);
      File newfile = new File(path + "/" + newname);
      if (!oldfile.exists()) {
        return;
      }
      if (newfile.exists()) {
        newfile.delete();
      }
      oldfile.renameTo(newfile);
    }
  }


  public static boolean unzipAll(String zipFilePath, String destPath) throws ZipException {
    ZipFile zFile = new ZipFile(zipFilePath);
    if (!zFile.isValidZipFile()) {
      return false;
    }
    /** Create temp unzip dir */
    File tempDestDir = new File(destPath);
    if (!tempDestDir.exists()) {
      tempDestDir.mkdir();
    }
    /** Unzip the file */
    zFile.extractAll(destPath);

    return true;
  }

  public static boolean unzipOne(String zipFilePath, String srcPath, String destPath) throws ZipException {
    ZipFile zFile = new ZipFile(zipFilePath);
    if (!zFile.isValidZipFile()) {
      return false;
    }
    zFile.extractFile(srcPath, destPath);
    return true;
  }

  public static boolean untarOne(String tarFilePath, String srcPath,String destPath) throws IOException{
      TarInputStream tarIn = null;
      try{
          tarIn = new TarInputStream(new GZIPInputStream(
                  new BufferedInputStream(new FileInputStream(new File(tarFilePath)))),
                  1024 * 2);
          TarEntry entry = null;
          while( (entry = tarIn.getNextEntry()) != null ){
              if(!entry.isDirectory()){
                  System.out.println(String.format("current path: %s, target path: %s",entry.getName(), srcPath));
      	          if(srcPath.equals(entry.getName())){
                      OutputStream out = null;
                      try{
                          out = new FileOutputStream(destPath);
                          int length = 0;
                          byte[] b = new byte[2048];
                          while((length = tarIn.read(b)) != -1){
                              out.write(b, 0, length);
                          }
                      }catch(IOException ex){
                          ex.printStackTrace();
                          return false;
                      }finally{
                          if(out!=null)
                              out.close();
                      }
                      System.out.println(String.format("Success: file %s is extract to %s",srcPath, destPath));
      	            break;
      	        }
              }
          }
      }catch(IOException ex){
          ex.printStackTrace();
          return false;
      } finally{
          try{
              if(tarIn != null){
                  tarIn.close();
              }
          }catch(IOException ex){
              ex.printStackTrace();
              return false;
          }
      }
      return true;
  }
}
