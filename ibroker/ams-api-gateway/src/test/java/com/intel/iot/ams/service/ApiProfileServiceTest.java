/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.service;

import com.intel.iot.ams.entity.ApiProfiles;
import com.intel.iot.ams.repository.ApiProfilesDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiProfileServiceTest {
  @Autowired private ApiProfileService profileService;
  @Autowired private ApiProfilesDao profileDao;

  @Before
  public void setUp() throws Exception {
    profileDao.deleteAll();
    ApiProfiles profileA = new ApiProfiles();
    profileA.setProductName("fruit");
    profileA.setProductVersion("1.0");
    profileA.setApi("mongo");
    profileA.setLevel(2);
    profileA.setBackward(1);
    profileService.save(profileA);
  }

  @After
  public void tearDown() throws Exception {
    profileDao.deleteAll();
  }

  @Test
  public void testFindByNameAndVersion() {
    List<ApiProfiles> apis = profileService.findByProductNameAndProductVersion("fruit", "1.0");
    assertThat(apis).isNotNull().hasSize(1);
    assertThat(apis.get(0))
            .hasFieldOrPropertyWithValue("productName","fruit")
            .hasFieldOrPropertyWithValue("productVersion", "1.0")
            .hasFieldOrPropertyWithValue("api", "mongo")
            .hasFieldOrPropertyWithValue("level", 2)
            .hasFieldOrPropertyWithValue("backward", 1);
  }

  @Test
  public void testDeleteAll() {
    profileService.deleteByProductNameAndProductVersion("fruit", "1.0");
    List<ApiProfiles> apis = profileService.findAll();
    assertThat(apis).isNotNull().hasSize(0);
  }


  @Test
  public void testUpdate() {
    List<ApiProfiles> apis = profileService.findByProductNameAndProductVersion("fruit", "1.0");
    assertThat(apis).isNotNull().hasSize(1);

    apis.get(0).setProductVersion("2.0");
    profileService.save(apis.get(0));

    apis = profileService.findByProductNameAndProductVersion("fruit", "2.0");
    assertThat(apis).isNotNull().hasSize(1);
  }
}
