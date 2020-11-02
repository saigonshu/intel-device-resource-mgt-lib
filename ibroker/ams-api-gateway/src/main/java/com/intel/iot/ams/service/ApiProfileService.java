/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.service;

import com.intel.iot.ams.entity.ApiProfiles;
import com.intel.iot.ams.repository.ApiProfilesDao;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApiProfileService extends BaseService<Integer, ApiProfiles> {

  @Autowired private ApiProfilesDao apiProfilesDao;

  public List<ApiProfiles> findByProductNameAndProductVersion(String productName, String productVersion) {
    return apiProfilesDao.findByProductNameAndProductVersion(productName, productVersion);
  }

  public List<ApiProfiles> findAll() {
    return apiProfilesDao.findAll();
  }

  public void save(ApiProfiles a) {
    apiProfilesDao.save(a);
  }

  public void update(ApiProfiles a) {
    apiProfilesDao.save(a);
  }
}
