/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

  public static String getSha1Hash(byte[] buffer) {
    try {
      MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
      digest.update(buffer);
      byte messageDigest[] = digest.digest();
      // Create Hex String
      StringBuffer hexString = new StringBuffer();
      for (int i = 0; i < messageDigest.length; i++) {
        String shaHex = Integer.toHexString(messageDigest[i] & 0xFF);
        if (shaHex.length() < 2) {
          hexString.append(0);
        }
        hexString.append(shaHex);
      }
      return hexString.toString();

    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return "";
  }

  public static String getMd5Hash(byte[] buffer) {
    try {
      MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
      digest.update(buffer);
      byte messageDigest[] = digest.digest();
      // Create Hex String
      StringBuffer hexString = new StringBuffer();
      for (int i = 0; i < messageDigest.length; i++) {
        String md5Hex = Integer.toHexString(messageDigest[i] & 0xFF);
        if (md5Hex.length() < 2) {
          hexString.append(0);
        }
        hexString.append(md5Hex);
      }
      return hexString.toString();

    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return "";
  }

  public static boolean checkVerComp(String version, String minVersion) {
    if (version == null || minVersion == null) {
      return true;
    }

    if (version.equals(minVersion)) {
      return true;
    }

    String[] verArray = new String[4];
    String[] minVerArray = new String[4];

    String[] tempVerArray = version.split("\\.");
    String[] tempMinVerArray = minVersion.split("\\.");

    for(int i=0; i<verArray.length; i++){
      verArray[i] = i<tempVerArray.length?tempVerArray[i]:"0";
    }

    for(int i=0; i<minVerArray.length; i++){
      minVerArray[i] = i<tempMinVerArray.length?tempMinVerArray[i]:"0";
    }

    for (int i = 0; i < verArray.length; i++) {
      if (Integer.parseInt(verArray[i]) > Integer.parseInt(minVerArray[i])) {
        return true;
      } else if (Integer.parseInt(verArray[i]) < Integer.parseInt(minVerArray[i])) {
        return false;
      }
    }

    return true;
  }
}
