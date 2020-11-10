/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.intel.iot.ams.entity.Product;
import com.intel.iot.ams.repository.ProductDao;
import com.intel.iot.ams.utils.AmsConstant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProductServiceTest {
  @Autowired private ProductService productService;
  @Autowired private ProductDao productDao;

  private Product productA = null;

  @Before
  public void setUp() throws Exception {
    productA = new Product();
    productA.setUuid("jujube");
    productA.setName("jostaberry");
    productA.setCategory(AmsConstant.ProductCategory.runtime_engine.toValue());
    productA.setVendor("blackberry");
    productA.setSubclass("fruit");

    if (productDao.findByUuid(productA.getUuid()) == null) {
      productDao.saveAndFlush(productA);
    }
  }

  @After
  public void tearDown() throws Exception {
    productDao.delete(productA);
  }

  @Test
  public void findByUUID() {
    assertThat(productService.findByUUID(productA.getUuid()))
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", productA.getName())
        .hasFieldOrPropertyWithValue("category", productA.getCategory());
  }

  @Test
  public void testFindCommon() {
    List<Product> ps = productService.findCommon(null, null, "fruit");
    assertThat(ps).isNotNull().hasSize(1);
    assertThat(ps.get(0)).hasFieldOrPropertyWithValue("subclass","fruit")
            .hasFieldOrPropertyWithValue("category", productA.getCategory())
            .hasFieldOrPropertyWithValue("name", "jostaberry");
    ps = productService.findCommon(null, String.valueOf(AmsConstant.ProductCategory.runtime_engine.toValue()), null);
    assertThat(ps).isNotNull().hasSize(1);
    assertThat(ps.get(0)).hasFieldOrPropertyWithValue("subclass","fruit")
            .hasFieldOrPropertyWithValue("category", productA.getCategory())
            .hasFieldOrPropertyWithValue("name", "jostaberry");
    ps = productService.findCommon("blackberry", String.valueOf(AmsConstant.ProductCategory.runtime_engine.toValue()), null);
    assertThat(ps).isNotNull().hasSize(1);
    assertThat(ps.get(0)).hasFieldOrPropertyWithValue("subclass","fruit")
            .hasFieldOrPropertyWithValue("category", productA.getCategory())
            .hasFieldOrPropertyWithValue("name", "jostaberry");
  }

  @Test
  public void findByName() {}

  @Test
  public void findByUuidAndName() {}

  @Test
  public void findByVendorAndCategory() {}

  @Test
  public void findAll() {}

  @Test
  public void saveOrUpdate() {}

  @Test
  public void update() {}
}
