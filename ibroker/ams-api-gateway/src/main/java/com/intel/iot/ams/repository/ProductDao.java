/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.repository;

import com.intel.iot.ams.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductDao extends JpaRepository<Product, Integer> {

  public Product findByUuid(String uuid);

  public Product findByName(String name);

  public Product findByUuidAndName(String uuid, String name);

  public List<Product> findByVendor(String vendor);

  public List<Product> findByCategory(Integer category);

  public List<Product> findByVendorAndCategory(String vendor, Integer category);

  @Query(value = " select * from Product where " +
          "if(?1 != '',vendor=?1, 1=1) and " +
          "if(?2 != '', category=?2, 1=1) and " +
          "if(?3 != '', subclass=?3, 1=1)", nativeQuery = true)
  public List<Product> findCommon(String vendor, String category, String subclass);

  public void removeByUuid(String uuid);

  public void removeByName(String name);
}
