/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.entity;

import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "ApiProfiles")
public class ApiProfiles {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(unique = true, nullable = false)
  private Integer id;

  @Column(nullable = false)
  private String api;

  @Column(nullable = false)
  private int level;

  private int backward;

  @Column(nullable = false)
  private String productName;

  @Column(nullable = false)
  private String productVersion;

  public void setApi(String api) {
    this.api = api;
  }

  public String getApi() {
    return api;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }

  public void setBackward(int backward) {
    this.backward = backward;
  }

  public int getBackward() {
    return backward;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductVersion(String productVersion) {
    this.productVersion = productVersion;
  }
  
  public String getProductVersion() {
    return productVersion;
  }

  @Override
  public String toString() {
    return "ApiProfiles{" +
            "id=" + id +
            ", api='" + api + '\'' +
            ", level=" + level +
            ", backward=" + backward +
            ", productName='" + productName + '\'' +
            ", productVersion='" + productVersion + '\'' +
            '}';
  }
}
