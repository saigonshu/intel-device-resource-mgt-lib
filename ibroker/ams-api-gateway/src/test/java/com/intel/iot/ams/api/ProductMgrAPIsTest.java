/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.iot.ams.entity.*;
import com.intel.iot.ams.repository.ProductDependencyDao;
import com.intel.iot.ams.repository.ProductInstanceDao;
import com.intel.iot.ams.repository.ProductPropertyDao;
import com.intel.iot.ams.service.CfgIdentifierService;
import com.intel.iot.ams.service.ProductPropertyService;
import com.intel.iot.ams.service.ProductService;

import com.intel.iot.ams.utils.AmsConstant;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class ProductMgrAPIsTest {
  @Autowired private ProductService pSrv;
  @Autowired private ProductInstanceDao piDao;
  @Autowired private ProductPropertyService ppSrv;
  @Autowired private CfgIdentifierService cfgIdSrv;
  @Autowired private ProductPropertyDao ppDao;
  @Autowired private ProductDependencyDao pdDao;

  @Autowired private ProductMgrAPIs productMgrAPIs;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AmsConstant AmsConst;

  private List<AmsClient> amsClientList = new ArrayList<>();

  @After
  public void tearDown() throws Exception {
    List<Product> ps = pSrv.findAll();
    if (ps!=null && !ps.isEmpty()){
      for(Product p: ps) pSrv.removeByUUID(p.getUuid());
    }

    piDao.deleteAll();

    List<CfgIdentifier> cs = cfgIdSrv.findAll();
    if (cs!=null && !cs.isEmpty()){
      for(CfgIdentifier c: cs) cfgIdSrv.delete(c);
    }

    ppDao.deleteAll();
    pdDao.deleteAll();

    deleteDir(AmsConst.tempPath);
  }

  @Test
  public void testUploadIagent() throws Exception {
    String pathUnzip = unzipResource("iagent.zip");
    assertThat(pathUnzip).isNotNull();

    // POST /ams_user_cloud/ams/v1/upload
    ResponseEntity<String> response = productMgrAPIs.handleZipPkgUpload(pathUnzip, null);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // check product record
    Product iagent = pSrv.findByName("iagent");
    assertThat(iagent)
            .isNotNull()
            .hasFieldOrPropertyWithValue("category", AmsConstant.ProductCategory.software_product.toValue())
            .hasFieldOrPropertyWithValue("vendor", "intel")
            .hasFieldOrPropertyWithValue("description", "Intel Device Management SDK");
    // check cfg record
    List<ProductInstance> iagentInstances = piDao.findByProductName("iagent");
    assertThat(iagentInstances).isNotNull().hasSize(1);
    assertThat(iagentInstances.get(0)).isNotNull()
            .hasFieldOrPropertyWithValue("productName", "iagent")
            .hasFieldOrPropertyWithValue("version", "1.0")
            .hasFieldOrPropertyWithValue("cpu", "IA")
            .hasFieldOrPropertyWithValue("os", "Linux")
            .hasFieldOrPropertyWithValue("osMin", "4.4.0")
            .hasFieldOrPropertyWithValue("system", "Ubuntu")
            .hasFieldOrPropertyWithValue("sysMin", "16.04")
            .hasFieldOrPropertyWithValue("bits", "64bit");

    // check product instance record
    List<CfgIdentifier> cfgs = cfgIdSrv.findAll();
    assertThat(cfgs).isNotNull().hasSize(7);
    CfgIdentifier cfg = cfgs.stream().filter(c -> c.getPathName().equals("/modbus_meta.cfg")).findFirst().orElse(null);
    assertThat(cfg).isNotNull()
            .hasFieldOrPropertyWithValue("pathName", "/modbus_meta.cfg")
            .hasFieldOrPropertyWithValue("targetType", "modbus_type");

    // check product properties
    List<ProductProperty> pps = ppDao.findByProductName("iagent");
    assertThat(pps).hasSize(0);

    //check product dependance
    List<ProductDependency> pds = pdDao.findByDependencyName("iagent");
    assertThat(pds).hasSize(0);

    deleteDir(AmsConst.repoPath+iagent.getName());
  }

  @Test
  public void testUploadPlcVm() throws Exception {
    String pathUnzip = unzipResource("plcvm.zip");
    assertThat(pathUnzip).isNotNull();

    // POST /ams_user_cloud/ams/v1/upload
    ResponseEntity<String> response = productMgrAPIs.handleZipPkgUpload(pathUnzip, null);
    assertThat(response.getBody()).isNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // check product
    // check product record
    Product plcvm = pSrv.findByName("plcvm");
    assertThat(plcvm)
            .isNotNull()
            .hasFieldOrPropertyWithValue("category", AmsConstant.ProductCategory.runtimeengine.toValue())
            .hasFieldOrPropertyWithValue("vendor", "openiot")
//TODO            .hasFieldOrPropertyWithValue("subclass", AmsConstant.K_SUBCLASS_PLC)
            .hasFieldOrPropertyWithValue("description", "runtime engine from openiot");
    // check cfg record
    List<ProductInstance> plcvmInstances = piDao.findByProductName(plcvm.getName());
    assertThat(plcvmInstances).isNotNull().hasSize(2);
    int piIndex = plcvmInstances.get(0).getCpu().equals("ARM")?0:1;
    assertThat(plcvmInstances.get(piIndex)).isNotNull()
            .hasFieldOrPropertyWithValue("productName", "plcvm")
            .hasFieldOrPropertyWithValue("version", "1.0")
            .hasFieldOrPropertyWithValue("cpu", "ARM")
            .hasFieldOrPropertyWithValue("os", "linux")
            .hasFieldOrPropertyWithValue("osMin", "2.4.0")
            .hasFieldOrPropertyWithValue("system", "Android")
            .hasFieldOrPropertyWithValue("sysMin", "7.04")
            .hasFieldOrPropertyWithValue("bits", "32bit");

    // check product instance record
    List<CfgIdentifier> cfgs = cfgIdSrv.findAll();
    assertThat(cfgs).isNotNull().hasSize(2);
    int cfgIndex = cfgs.get(0).getPathName().equals("/meta.cfg")?0:1;
    assertThat(cfgs.get(cfgIndex)).isNotNull()
            .hasFieldOrPropertyWithValue("pathName", "/meta.cfg")
            .hasFieldOrPropertyWithValue("targetType", "device");

    // check product properties
    List<ProductProperty> pps = ppDao.findByProductName(plcvm.getName());
    assertThat(pps).hasSize(0);

    //check product dependance
    List<ProductDependency> pds = pdDao.findByDependencyName(plcvm.getName());
    assertThat(pds).hasSize(0);

    //TODO check for api profile


    deleteDir(AmsConst.repoPath+plcvm.getName());
  }

  @Test
  public void testUploadPlcApp() throws Exception {
    String pathUnzip = unzipResource("plcapp.zip");
    assertThat(pathUnzip).isNotNull();

    // POST /ams_user_cloud/ams/v1/upload
    ResponseEntity<String> response = productMgrAPIs.handleZipPkgUpload(pathUnzip, null);
    assertThat(response.getBody()).isNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // check product
    // check product record
    Product plcapp = pSrv.findByName("plc-app-demo");
    assertThat(plcapp)
            .isNotNull()
            .hasFieldOrPropertyWithValue("category", AmsConstant.ProductCategory.managedapp.toValue())
            .hasFieldOrPropertyWithValue("vendor", "openiot")
//TODO            .hasFieldOrPropertyWithValue("subclass", AmsConstant.K_SUBCLASS_PLC)
            .hasFieldOrPropertyWithValue("description", "plc app demo from openiot");
    // check product instance record
    List<ProductInstance> plcappInstances = piDao.findByProductName(plcapp.getName());
    assertThat(plcappInstances).isNotNull().hasSize(1);
    assertThat(plcappInstances.get(0)).isNotNull()
            .hasFieldOrPropertyWithValue("productName", "plc-app-demo")
            .hasFieldOrPropertyWithValue("version", "1.0")
            .hasFieldOrPropertyWithValue("cpu", "any")
            .hasFieldOrPropertyWithValue("os", "any")
            .hasFieldOrPropertyWithValue("system", "any");

    // check cfg record
    List<CfgIdentifier> cfgs = cfgIdSrv.findAll();
    assertThat(cfgs).isNotNull().hasSize(2);
    int cfgIndex = cfgs.get(0).getPathName().equals("/meta.cfg")?0:1;
    assertThat(cfgs.get(cfgIndex)).isNotNull()
            .hasFieldOrPropertyWithValue("pathName", "/meta.cfg")
            .hasFieldOrPropertyWithValue("targetType", "device");

    // check product properties
    List<ProductProperty> pps = ppDao.findByProductName(plcapp.getName());
    assertThat(pps).hasSize(0);

    //check product dependance
    List<ProductDependency> pds = pdDao.findByDependencyName(plcapp.getName());
    assertThat(pds).hasSize(0);

    //TODO check for api profile

    deleteDir(AmsConst.repoPath+plcapp.getName());
  }

  private String unzipResource(String zipResourceName) {
    try {
      String pathBase = ResourceUtils.getURL("target/test-classes").getPath();
      String pathUnzip = AmsConst.tempPath+zipResourceName.replace(".zip","");
      System.out.println(String.format("pathBase: %s, \n pathUnzip:%s", pathBase, pathUnzip));
      ZipFile zFile = new ZipFile(pathBase+zipResourceName);
      File unzip = new File(pathUnzip);
      if(!unzip.exists()){
        unzip.mkdir();
      }
      zFile.extractAll(pathUnzip);
      return pathUnzip;
    } catch (ZipException | FileNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static boolean deleteDir(String filePath) {
    File dir = new File(filePath);
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (int i=0; i<children.length; i++) {
        boolean success = deleteDir(filePath+"/"+children[i]);
        if (!success) {
          return false;
        }
      }
    }
    return dir.delete();
  }

}
