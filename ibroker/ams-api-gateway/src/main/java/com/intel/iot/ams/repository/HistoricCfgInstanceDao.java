/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.repository;

import com.intel.iot.ams.entity.HistoricCfgInstance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricCfgInstanceDao extends JpaRepository<HistoricCfgInstance, Integer> {

  public List<HistoricCfgInstance> findByCfgUuid(String uuid);

  public List<HistoricCfgInstance> findByInstanceId(int id);
}
