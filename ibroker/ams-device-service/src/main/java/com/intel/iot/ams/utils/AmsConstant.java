/*
* Copyright (C) 2020 Intel Corporation. All rights reserved.
* SPDX-License-Identifier: Apache-2.0
*/

package com.intel.iot.ams.utils;

public class AmsConstant {

  /** Linux repo root*/
  public static final String repoPath = "/ams/ams_cloud/";

  /** Windows repo root*/
  // public static final String repoPath = "C:\\code\\ams\\";

  public static final String tempPath = repoPath + "/temp/";

  public static final String downloadPath = repoPath + "/download/";

  public static enum ProductCategory{
    software_product(1),fw_product(2),plugin_app(3),imrt_app(4),fw_app_wasm(5), runtime_engine(6), managed_app(7);
    private int value;
    private ProductCategory(int v) {
      value = v;
    }
    public int toValue() {
      return value;
    }
    public static ProductCategory fromValue(int v) {
      for(ProductCategory c : ProductCategory.values()){
        if (c.toValue()==v) return c;
      }
      return software_product;
    }
  }

}
