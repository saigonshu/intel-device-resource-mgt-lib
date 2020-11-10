/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.ConfigRepository;
import com.openiot.cloud.base.mongo.dao.DeviceRepository;
import com.openiot.cloud.base.mongo.dao.ResProRepository;
import com.openiot.cloud.base.mongo.dao.ResourceRepository;
import com.openiot.cloud.base.mongo.model.Config;
import com.openiot.cloud.base.mongo.model.Device;
import com.openiot.cloud.cfg.model.PlcManagerConfig;
import com.openiot.cloud.testbase.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {Application.class},
    properties = {"mongo.db = test_openiot"})
public class PlcMgrConfigTest {

  @Autowired private MongoTemplate mongoTemplate;

  @Autowired DeviceRepository devRepo;

  @Autowired ConfigRepository cfgRepo;

  @Autowired ConfigTaskHandler handler;

  @Before
  public void setUp() {
    try {
      devRepo.deleteAll();
      cfgRepo.deleteAll();
      TestUtil.importTestDb(
          mongoTemplate, ConstDef.C_DEV,  ConstDef.C_TASKSRVREG);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testPlcMgrConfigWithPeer() {
    try {
      String deviceId = "plc-gw";
      Device device = devRepo.findOneById(deviceId);
      System.out.println("--> " + device);
      Optional<List<Device>> childDevices = Optional.ofNullable(devRepo.findByIAgentId(deviceId));
      Optional<List<Device>> vplcs = childDevices.filter(ds->!ds.isEmpty()).map(ds -> ds.stream()
              .filter(d -> d.getDeviceType().equals(ConstDef.DEV_TYPE_VPLC))
              .collect(Collectors.toList()));
      Optional<List<Device>> rplcs = childDevices.filter(ds->!ds.isEmpty()).map(ds -> ds.stream()
              .filter(d -> d.getDeviceType().equals(ConstDef.DEV_TYPE_RPLC))
              .collect(Collectors.toList()));
      PlcManagerConfig devCfg = PlcManagerConfig.from(Optional.ofNullable(device), vplcs, rplcs);
      System.out.println("--> " + devCfg);

      assertCfg(devCfg);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testPlcMgrConfigWithoutPeer() {
    try {
      String deviceId = "plc-gw";
      Device device = devRepo.findOneById(deviceId);
      Device.Config cfg = device.getConfig();
      cfg.setUserCfgs(new ArrayList<>());
      device.setConfig(cfg);
      System.out.println("--> " + device);
      Optional<List<Device>> childDevices = Optional.ofNullable(devRepo.findByIAgentId(deviceId));
      Optional<List<Device>> vplcs = childDevices.filter(ds->!ds.isEmpty()).map(ds -> ds.stream()
              .filter(d -> d.getDeviceType().equals(ConstDef.DEV_TYPE_VPLC))
              .collect(Collectors.toList()));
      Optional<List<Device>> rplcs = childDevices.filter(ds->!ds.isEmpty()).map(ds -> ds.stream()
              .filter(d -> d.getDeviceType().equals(ConstDef.DEV_TYPE_RPLC))
              .collect(Collectors.toList()));
      PlcManagerConfig devCfg = PlcManagerConfig.from(Optional.ofNullable(device), vplcs, rplcs);
      System.out.println("--> " + devCfg);

      assertThat(devCfg)
              .isNotNull()
              .hasFieldOrPropertyWithValue("id", "plc-gw")
              .hasFieldOrPropertyWithValue("peer", null)
              .hasFieldOrProperty("vPlcs")
              .hasFieldOrProperty("rPlcs")
              .hasFieldOrPropertyWithValue("credential", null);

      assertThat(devCfg.toJsonString()).isNotNull().doesNotContain("peer");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testCheckAndBuildGateway() {
    try {
      handler.generateDevConfiguration("plc-gw");
      List<Config> config = cfgRepo.findAll();
      assertThat(config).isNotNull().isNotEmpty().hasSize(2);

      int index = config.get(0).getTargetType()==ConstDef.CFG_TT_PLC_MGR?0:1;
      String configJson = config.get(index).getConfig();
      PlcManagerConfig devCfg = new ObjectMapper().readValue(configJson, PlcManagerConfig.class);

      assertCfg(devCfg);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testCheckAndBuildVPlc() {
    try {
      handler.generateDevConfiguration("vplc-1");
      List<Config> config = cfgRepo.findAll();
      assertThat(config).isNotNull().isNotEmpty().hasSize(2);

      int index = config.get(0).getTargetType()==ConstDef.CFG_TT_PLC_MGR?0:1;
      String configJson = config.get(index).getConfig();
      PlcManagerConfig devCfg = new ObjectMapper().readValue(configJson, PlcManagerConfig.class);

      assertCfg(devCfg);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testCheckAndBuildRPlc() {
    try {
      handler.generateDevConfiguration("rplc-1");
      List<Config> config = cfgRepo.findAll();
      assertThat(config).isNotNull().isNotEmpty().hasSize(2);

      int index = config.get(0).getTargetType()==ConstDef.CFG_TT_PLC_MGR?0:1;
      String configJson = config.get(index).getConfig();
      PlcManagerConfig devCfg = new ObjectMapper().readValue(configJson, PlcManagerConfig.class);

      assertCfg(devCfg);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testCheckAndBuildNormalGateway() {
    try {
      handler.generateDevConfiguration("cardio");
      List<Config> config = cfgRepo.findAll();
      assertThat(config).isNotNull().isNotEmpty().hasSize(1);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void assertCfg(PlcManagerConfig devCfg) {
    assertThat(devCfg)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", "plc-gw")
            .hasFieldOrProperty("peer")
            .hasFieldOrProperty("vPlcs")
            .hasFieldOrProperty("rPlcs")
            .hasNoNullFieldsOrPropertiesExcept("credential");

    assertThat(devCfg.getPeer())
            .isNotNull()
            .hasFieldOrPropertyWithValue("name", "cardio")
            .hasFieldOrPropertyWithValue("ip", "192.168.1.1")
            .hasFieldOrPropertyWithValue("port", 180)
            .hasFieldOrPropertyWithValue("hbInMs", 12)
            .hasFieldOrPropertyWithValue("role", "master")
            .hasFieldOrPropertyWithValue("lossAction", "reset")
            .hasNoNullFieldsOrPropertiesExcept("key");

    assertThat(devCfg.getvPlcs())
            .isNotEmpty()
            .hasSize(1);

    assertThat(devCfg.getrPlcs())
            .isNotEmpty()
            .hasSize(1);

    assertThat(devCfg.getvPlcs().get(0))
            .isNotNull()
            .hasFieldOrPropertyWithValue("name", "vplc-1")
            .hasFieldOrPropertyWithValue("plcRuntimeSoftware", "plc_runtime1")
            .hasFieldOrPropertyWithValue("plcRuntimeSoftwareVer", "1.1")
            .hasFieldOrPropertyWithValue("plcApp", "plc_app1")
            .hasFieldOrPropertyWithValue("plcAppVer", "2.1")
            .hasFieldOrPropertyWithValue("startMode", "auto")
            .hasFieldOrPropertyWithValue("debugMode", true)
            .hasFieldOrPropertyWithValue("localStandby", true)
            .hasFieldOrPropertyWithValue("remoteStandby", false);

    assertThat(devCfg.getrPlcs().get(0))
            .isNotNull()
            .hasFieldOrPropertyWithValue("name", "rplc-1")
            .hasFieldOrPropertyWithValue("plcRuntimeSoftware", "plc_runtime1")
            .hasFieldOrPropertyWithValue("plcRuntimeSoftwareVer", "1.1")
            .hasFieldOrPropertyWithValue("plcApp", "plc_app1")
            .hasFieldOrPropertyWithValue("plcAppVer", "2.1")
            .hasFieldOrProperty("addr");
    assertThat(devCfg.getrPlcs().get(0).getAddr())
            .isNotNull()
            .hasFieldOrPropertyWithValue("port", 10)
            .hasFieldOrPropertyWithValue("ip", "192.168.1.1");

    String devCfgAsJsonString = devCfg.toJsonString();
    System.out.println("--> " + devCfgAsJsonString);
    assertThat(devCfgAsJsonString)
            .isNotNull()
            .isNotEmpty()
            .contains("\"peer\"")
            .contains("\"virtual-plcs\"")
            .contains("\"real-plcs\"");
  }
}
