/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.repository;

import com.intel.iot.ams.entity.ApiProfiles;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiProfilesDao extends JpaRepository<ApiProfiles, Integer> {

  public List<ApiProfiles> findByProductNameAndProductVersion(String productName, String productVersion);

  void deleteByProductNameAndProductVersion(String productName, String productVersion);
}
