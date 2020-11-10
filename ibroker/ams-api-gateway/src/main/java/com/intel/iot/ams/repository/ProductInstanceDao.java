/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.repository;

import com.intel.iot.ams.entity.ProductInstance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductInstanceDao extends JpaRepository<ProductInstance, Integer> {

  public List<ProductInstance> findByProductName(String name);

  public List<ProductInstance> findByProductNameAndVersion(String name, String version);

  public ProductInstance findByProductNameAndVersionAndCpuAndPlatformAndOs(
      String name, String version, String cpu, String platform, String os);

  public List<ProductInstance> findByProductNameAndVersionAndCpuAndPlatformAndOsAndSystemAndBits(
      String name,
      String version,
      String cpu,
      String platform,
      String os,
      String system,
      String bits);

  public List<ProductInstance> findByProductNameAndVersionAndCpuAndPlatformAndOsAndBits(
      String name, String version, String cpu, String platform, String os, String bits);

  public void removeByProductName(String name);

  public void removeByProductNameAndVersion(String name, String version);

  @Query("SELECT DISTINCT p.version FROM ProductInstance p WHERE p.productName = ?1")
  public List<String> getVersionsByProductName(String name);

  public List<ProductInstance> findByProductNameAndVersionAndCpu(
      String name, String version, String cpu);


  @Query(value = " select * from ProductInstance where " +
          "if(?1 != '',productName=?1, 1=1) and " +
          "if(?2 != '', version=?2, 1=1) and " +
          "if(?3 != '', cpu=?3, 1=1) and " +
          "if(?4 != '', platform=?4, 1=1) and " +
          "if(?5 != '', os=?5, 1=1) and " +
          "if(?6 != '', system=?6, 1=1) and " +
          "if(?7 != '', bits=?7, 1=1)"
          , nativeQuery = true)
  public List<ProductInstance> findCommon(String name, String version, String cpu, String platform, String os, String system, String bits);

}
