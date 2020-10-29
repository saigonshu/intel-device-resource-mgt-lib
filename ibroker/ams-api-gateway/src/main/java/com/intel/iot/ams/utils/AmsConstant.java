/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AmsConstant {

  public static final String K_SUBCLASS_PLC = "PLC";
  public static final String K_SUBCLASS_JAVA = "JAVA";
    /** Linux AMS repo root directories */
  @Value("${ams.repoPath}")
  public String repoPath;

  /** Windows AMS repo root directories */
  // public static final String repoPath = "C:\\code\\ams\\";
  @Value("${ams.tempPath}")
  public String tempPath;

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
