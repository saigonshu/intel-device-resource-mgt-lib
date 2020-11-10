/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.intel.iot.ams.entity.*;
import com.intel.iot.ams.repository.*;
import com.intel.iot.ams.service.CfgIdentifierService;
import com.intel.iot.ams.service.ProductPropertyService;
import com.intel.iot.ams.service.ProductService;

import com.intel.iot.ams.utils.AmsConstant;
import com.intel.iot.ams.utils.HashUtils;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class ProductMgrAPIsTest {
  @Autowired
  private ProductService pSrv;
  @Autowired
  private ProductInstanceDao piDao;
  @Autowired
  private ProductPropertyService ppSrv;
  @Autowired
  private CfgIdentifierService cfgIdSrv;
  @Autowired
  private ProductPropertyDao ppDao;
  @Autowired
  private ProductDependencyDao pdDao;
  @Autowired
  private ApiProfilesDao apiProfilesDao;
  @Autowired private AmsClientDao amsClientDao;

  @Autowired
  private ProductMgrAPIs productMgrAPIs;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private AmsConstant AmsConst;

  @Autowired
  private WebApplicationContext webCtx;

  private List<AmsClient> amsClientList = new ArrayList<>();

  @After
  public void tearDown() throws Exception {
    List<Product> ps = pSrv.findAll();
    if (ps != null && !ps.isEmpty()) {
      for (Product p : ps) pSrv.removeByUUID(p.getUuid());
    }

    piDao.deleteAll();
    amsClientDao.deleteAll();

    List<CfgIdentifier> cs = cfgIdSrv.findAll();
    if (cs != null && !cs.isEmpty()) {
      for (CfgIdentifier c : cs) cfgIdSrv.delete(c);
    }

    ppDao.deleteAll();
    pdDao.deleteAll();
    apiProfilesDao.deleteAll();

    deleteDir(AmsConst.tempPath);
  }

  @Test
  public void testUploadIagent() throws Exception {
    uploadZipPakage("iagent.zip");

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

    deleteDir(AmsConst.repoPath + iagent.getName());
  }

  @Test
  public void testUploadPlcVm() throws Exception {
    uploadZipPakage("plcvm.zip");

    // check product
    // check product record
    Product plcvm = pSrv.findByName("plcvm");
    assertThat(plcvm)
            .isNotNull()
            .hasFieldOrPropertyWithValue("category", AmsConstant.ProductCategory.runtime_engine.toValue())
            .hasFieldOrPropertyWithValue("vendor", "openiot")
            .hasFieldOrPropertyWithValue("subclass", AmsConstant.K_SUBCLASS_PLC)
            .hasFieldOrPropertyWithValue("description", "runtime engine from openiot");
    // check cfg record
    List<ProductInstance> plcvmInstances = piDao.findByProductName(plcvm.getName());
    assertThat(plcvmInstances).isNotNull().hasSize(2);
    int piIndex = plcvmInstances.get(0).getCpu().equals("ARM") ? 0 : 1;
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
    int cfgIndex = cfgs.get(0).getPathName().equals("/meta.cfg") ? 0 : 1;
    assertThat(cfgs.get(cfgIndex)).isNotNull()
            .hasFieldOrPropertyWithValue("pathName", "/meta.cfg")
            .hasFieldOrPropertyWithValue("targetType", "device");

    // check product properties
    List<ProductProperty> pps = ppDao.findByProductName(plcvm.getName());
    assertThat(pps).hasSize(0);

    //check product dependance
    List<ProductDependency> pds = pdDao.findByDependencyName(plcvm.getName());
    assertThat(pds).hasSize(0);

    //check for api profile
    List<ApiProfiles> apiProfiles = apiProfilesDao.findByProductNameAndProductVersion(plcvm.getName(), plcvmInstances.get(piIndex).getVersion());
    assertThat(apiProfiles).isNotNull().hasSize(2);
    int apiOs = apiProfiles.get(0).getApi().equals("os") ? 0 : 1;
    assertThat(apiProfiles.get(apiOs)).isNotNull()
            .hasFieldOrPropertyWithValue("api", "os")
            .hasFieldOrPropertyWithValue("level", 25)
            .hasFieldOrPropertyWithValue("backward", 22);
    assertThat(apiProfiles.get(1 - apiOs)).isNotNull()
            .hasFieldOrPropertyWithValue("api", "ai")
            .hasFieldOrPropertyWithValue("level", 2)
            .hasFieldOrPropertyWithValue("backward", 2);


    deleteDir(AmsConst.repoPath + plcvm.getName());
  }

  @Test
  public void testUploadPlcApp() throws Exception {
    uploadZipPakage("plcapp.zip");

    // check product
    // check product record
    Product plcapp = pSrv.findByName("plc-app-demo");
    assertThat(plcapp)
            .isNotNull()
            .hasFieldOrPropertyWithValue("category", AmsConstant.ProductCategory.managed_app.toValue())
            .hasFieldOrPropertyWithValue("vendor", "openiot")
            .hasFieldOrPropertyWithValue("subclass", AmsConstant.K_SUBCLASS_PLC)
            .hasFieldOrPropertyWithValue("description", "plc app demo from openiot");
    // check product instance record
    List<ProductInstance> plcappInstances = piDao.findByProductName(plcapp.getName());
    assertThat(plcappInstances).isNotNull().hasSize(1);
    assertThat(plcappInstances.get(0)).isNotNull()
            .hasFieldOrPropertyWithValue("instanceName", "any")
            .hasFieldOrPropertyWithValue("productName", "plc-app-demo")
            .hasFieldOrPropertyWithValue("version", "1.0")
            .hasFieldOrPropertyWithValue("cpu", "any")
            .hasFieldOrPropertyWithValue("os", "any")
            .hasFieldOrPropertyWithValue("system", "any");

    // check cfg record
    List<CfgIdentifier> cfgs = cfgIdSrv.findAll();
    assertThat(cfgs).isNotNull().hasSize(2);
    int cfgIndex = cfgs.get(0).getPathName().equals("/meta.cfg") ? 0 : 1;
    assertThat(cfgs.get(cfgIndex)).isNotNull()
            .hasFieldOrPropertyWithValue("pathName", "/meta.cfg")
            .hasFieldOrPropertyWithValue("targetType", "device");

    // check product properties
    List<ProductProperty> pps = ppDao.findByProductName(plcapp.getName());
    assertThat(pps).hasSize(0);

    //check product dependance
    List<ProductDependency> pds = pdDao.findByDependencyName(plcapp.getName());
    assertThat(pds).hasSize(0);

    //check for api profile
    List<ApiProfiles> apiProfiles = apiProfilesDao.findByProductNameAndProductVersion(plcapp.getName(), plcappInstances.get(0).getVersion());
    assertThat(apiProfiles).isNotNull().hasSize(2);
    int apiOs = apiProfiles.get(0).getApi().equals("os") ? 0 : 1;
    assertThat(apiProfiles.get(apiOs)).isNotNull()
            .hasFieldOrPropertyWithValue("api", "os")
            .hasFieldOrPropertyWithValue("level", 21)
            .hasNoNullFieldsOrPropertiesExcept("backward");
    assertThat(apiProfiles.get(1 - apiOs)).isNotNull()
            .hasFieldOrPropertyWithValue("api", "ai")
            .hasFieldOrPropertyWithValue("level", 2)
            .hasNoNullFieldsOrPropertiesExcept("backward");

    deleteDir(AmsConst.repoPath + plcapp.getName());
  }

  @Test
  public void testGetProdcut() {
    MockMvc webMock = MockMvcBuilders.webAppContextSetup(webCtx).build();

    // prepare product and product instance
    uploadZipPakage("plcapp.zip");
    uploadZipPakage("plcvm.zip");

    // test fail to get for subclass
    String cate = AmsConstant.ProductCategory.managed_app.name();
    MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.get("/ams_user_cloud/ams/v1/product");
    mockHttpServletRequestBuilder.param("category", cate);
    mockHttpServletRequestBuilder.param("subclass", "JAVA");
    ResultActions resultActions = null;
    try {
      resultActions = webMock.perform(mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      String content = result.getContentAsString();
      assertThat(content).isNotNull().contains("[]");
      resultActions.andExpect(status().isOk());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // test successfully get for subclass
    mockHttpServletRequestBuilder = MockMvcRequestBuilders.get("/ams_user_cloud/ams/v1/product");
    mockHttpServletRequestBuilder.param("category", cate);
    mockHttpServletRequestBuilder.param("subclass", "PLC");
    try {
      resultActions = webMock.perform(mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      String content = result.getContentAsString();
      assertThat(content).isNotNull().contains("plc-app-demo")
              .contains("subclass").contains("api_profiles");
      resultActions.andExpect(status().isOk());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // test successfully get scenario
    mockHttpServletRequestBuilder.param("supporting_runtime_name", "plcvm");
    mockHttpServletRequestBuilder.param("supporting_runtime_ver", "1.0");
    try {
      resultActions = webMock.perform(mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      String content = result.getContentAsString();
      resultActions.andExpect(status().isOk());
      assertThat(content).isNotNull().contains("plc-app-demo");
      JSONObject jsonContent = new JSONObject(content);
      assertThat(jsonContent.getJSONArray("product_list").length()).isEqualTo(1);

      // assert product
      JSONObject product = new JSONObject(jsonContent.getJSONArray("product_list").getString(0));
      assertThat(product.get("name")).isEqualTo("plc-app-demo");
      assertThat(product.get("category")).isEqualTo("managed_app");
      assertThat(product.get("subclass")).isEqualTo("PLC");
      assertThat(product.getJSONArray("versions").length()).isEqualTo(1);

      // assert version
      JSONObject version = product.getJSONArray("versions").getJSONObject(0);
      assertThat(version.get("version")).isEqualTo("1.0");
      assertThat(version.getJSONArray("api_profiles").length()).isEqualTo(2);

      // assert product
      JSONObject api = version.getJSONArray("api_profiles").getJSONObject(0);
      assertThat(api.get("api")).isEqualTo("os");
      assertThat(api.get("level")).isEqualTo(21);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // test fail to get scenario
    List<ApiProfiles> apis = apiProfilesDao.findByProductNameAndProductVersion("plc-app-demo", "1.0");
    assertThat(apis).isNotNull().hasSize(2);
    apis.get(0).setLevel(100);
    apiProfilesDao.saveAndFlush(apis.get(0));

    try {
      resultActions = webMock.perform(mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      String content = result.getContentAsString();
      assertThat(content).isNotNull().contains("[]");
      resultActions.andExpect(status().isOk());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Test
  public void testGetProductInstance() {
    MockMvc webMock = MockMvcBuilders.webAppContextSetup(webCtx).build();
    // prepare product and product instance
    uploadZipPakage("plcapp.zip");
    uploadZipPakage("plcvm.zip");

    List<ProductInstance> pis = piDao.findByProductName("plcvm");
    assertThat(pis).isNotNull().hasSize(2);

    pis = piDao.findCommon("plcvm", null, null, null, null, null, null);
    assertThat(pis).isNotNull().hasSize(2);

    pis = piDao.findCommon("plcvm", null, "IA", null, null, null, null);
    assertThat(pis).isNotNull().hasSize(1);

    pis = piDao.findCommon("plcvm", null, "IA", null, "Linux", null, null);
    assertThat(pis).isNotNull().hasSize(1);

    pis = piDao.findCommon("plcvm", null, "IA", null, "Linux", "Ubuntu", null);
    assertThat(pis).isNotNull().hasSize(1);

    pis = piDao.findCommon("plcvm", null, "IA", null, "Linux", "Ubuntu", "64bit");
    assertThat(pis).isNotNull().hasSize(1);

    pis = piDao.findCommon("plcvm", "1.0", "IA", null, "Linux", "Ubuntu", "64bit");
    assertThat(pis).isNotNull().hasSize(1);
  }

    @Test
  public void testGetProductByDeviceSuccess(){
    MockMvc webMock = MockMvcBuilders.webAppContextSetup(webCtx).build();
    // prepare product and product instance
    uploadZipPakage("plcapp.zip");
    uploadZipPakage("plcvm.zip");

    addAmsClient();

    // test fail to get for subclass
    String cate = AmsConstant.ProductCategory.runtime_engine.name();
    MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.get("/ams_user_cloud/ams/v1/product" );
    mockHttpServletRequestBuilder.param("category", cate );
    mockHttpServletRequestBuilder.param("subclass", "PLC");
    mockHttpServletRequestBuilder.param("supported_by_device", "melon");
    ResultActions resultActions = null;
    try {
      resultActions = webMock.perform( mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      String content = result.getContentAsString();
      assertThat(content).isNotNull().contains("plcvm")
              .contains("subclass").contains("api_profiles");
      resultActions.andExpect(status().isOk());

      JSONObject jsonContent = new JSONObject(content);
      assertThat(jsonContent.getJSONArray("product_list").length()).isEqualTo(1);
      // assert product
      JSONObject product = new JSONObject(jsonContent.getJSONArray("product_list").getString(0));
      assertThat(product.get("name")).isEqualTo("plcvm");
      assertThat(product.get("category")).isEqualTo("runtime_engine");
      assertThat(product.get("subclass")).isEqualTo("PLC");
      assertThat(product.getJSONArray("versions").length()).isEqualTo(1);
      // assert version
      JSONObject version = product.getJSONArray("versions").getJSONObject(0);
      assertThat(version.get("version")).isEqualTo("1.0");
      assertThat(version.getJSONArray("api_profiles").length()).isEqualTo(2);
      assertThat(version.getJSONArray("instances").length()).isEqualTo(2);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testGetProductByDeviceFail(){
    MockMvc webMock = MockMvcBuilders.webAppContextSetup(webCtx).build();
    // prepare product and product instance
    uploadZipPakage("plcapp.zip");
    uploadZipPakage("plcvm.zip");

    addAmsClient();
    AmsClient clt = amsClientDao.findByClientUuid("melon");
    clt.setOsVer("2.0");
    amsClientDao.saveAndFlush(clt);

    // test fail to get for subclass
    String cate = AmsConstant.ProductCategory.runtime_engine.name();
    MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.get("/ams_user_cloud/ams/v1/product" );
    mockHttpServletRequestBuilder.param("category", cate );
    mockHttpServletRequestBuilder.param("subclass", "PLC");
    mockHttpServletRequestBuilder.param("supported_by_device", "melon");
    ResultActions resultActions = null;
    try {
      resultActions = webMock.perform( mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      String content = result.getContentAsString();
      assertThat(content).isNotNull().contains("plcvm");
      resultActions.andExpect(status().isOk());

      JSONObject jsonContent = new JSONObject(content);
      assertThat(jsonContent.getJSONArray("product_list").length()).isEqualTo(1);
      // assert product
      JSONObject product = new JSONObject(jsonContent.getJSONArray("product_list").getString(0));
      assertThat(product.get("name")).isEqualTo("plcvm");
      assertThat(product.get("category")).isEqualTo("runtime_engine");
      assertThat(product.get("subclass")).isEqualTo("PLC");
      assertThat(product.getJSONArray("versions").length()).isEqualTo(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Test
  public void testCheckVersion(){
    assertThat(HashUtils.checkVerComp("2","1")).isTrue();
    assertThat(HashUtils.checkVerComp("2","1.0")).isTrue();
    assertThat(HashUtils.checkVerComp("2","1.9")).isTrue();
    assertThat(HashUtils.checkVerComp("2.1","2.0")).isTrue();
    assertThat(HashUtils.checkVerComp("2.1.3","2.1")).isTrue();
    assertThat(HashUtils.checkVerComp("2.1.3","2.1.2")).isTrue();
    assertThat(HashUtils.checkVerComp("2.1.3","2.1.2.99")).isTrue();
    assertThat(HashUtils.checkVerComp("2.1.3.4","2.1.3.3")).isTrue();
    assertThat(HashUtils.checkVerComp("1", "2")).isFalse();
    assertThat(HashUtils.checkVerComp("1.0","2")).isFalse();
    assertThat(HashUtils.checkVerComp("1.9","2")).isFalse();
    assertThat(HashUtils.checkVerComp("2.0", "2.1")).isFalse();
    assertThat(HashUtils.checkVerComp("2.1", "2.1.3")).isFalse();
    assertThat(HashUtils.checkVerComp("2.1.2", "2.1.3")).isFalse();
    assertThat(HashUtils.checkVerComp("2.1.2.99", "2.1.3")).isFalse();
    assertThat(HashUtils.checkVerComp("2.1.3.3", "2.1.3.4")).isFalse();
  }


    private void addAmsClient() {
    AmsClient amsClient = new AmsClient();
    amsClient.setBits("64bit");
    amsClient.setClientUuid("melon");
    amsClient.setCpu("IA");
    amsClient.setOs("Linux");
    amsClient.setOsVer("4.15.0");
    amsClient.setSystem("Ubuntu");
    amsClient.setSysVer("24.15.0");
    amsClient.setProvisionTime(new Date());
    amsClient.setSerial("melon");
    amsClientDao.saveAndFlush(amsClient);
  }

  private void uploadZipPakage(String zipName) {
    String pathUnzip = unzipResource(zipName);
    assertThat(pathUnzip).isNotNull();
    ResponseEntity<String> response = productMgrAPIs.handleZipPkgUpload(pathUnzip, null);
    assertThat(response.getBody()).isNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testGetProdcutByName(){
    MockMvc webMock = MockMvcBuilders.webAppContextSetup(webCtx).build();

    // prepare product and product instance
    String pathUnzip = unzipResource("plcapp.zip");
    assertThat(pathUnzip).isNotNull();
    ResponseEntity<String> response = productMgrAPIs.handleZipPkgUpload(pathUnzip, null);
    assertThat(response.getBody()).isNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    pathUnzip = unzipResource("plcvm.zip");
    assertThat(pathUnzip).isNotNull();
    response = productMgrAPIs.handleZipPkgUpload(pathUnzip, null);
    assertThat(response.getBody()).isNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // test successfully get for name
    MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.get("/ams_user_cloud/ams/v1/product");
    mockHttpServletRequestBuilder.param("name", "plc-app-demo" );
    ResultActions resultActions;
    try {
      resultActions = webMock.perform( mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      String content = result.getContentAsString();
      resultActions.andExpect(status().isOk());
      assertThat(content).isNotNull().contains("plc-app-demo");
      JSONObject jsonContent = new JSONObject(content);
      assertThat(jsonContent.getJSONArray("product_list").length()).isEqualTo(1);

      // assert product
      JSONObject product = new JSONObject(jsonContent.getJSONArray("product_list").getString(0));
      assertThat(product.get("name")).isEqualTo("plc-app-demo");
      assertThat(product.get("category")).isEqualTo("managed_app");
      assertThat(product.get("subclass")).isEqualTo("PLC");
      assertThat(product.getJSONArray("versions").length()).isEqualTo(1);

      // assert version
      JSONObject version = product.getJSONArray("versions").getJSONObject(0);
      assertThat(version.get("version")).isEqualTo("1.0");
      assertThat(version.getJSONArray("api_profiles").length()).isEqualTo(2);

      // assert product
      JSONObject api = version.getJSONArray("api_profiles").getJSONObject(0);
      assertThat(api.get("api")).isEqualTo("os");
      assertThat(api.get("level")).isEqualTo(21);
    } catch (Exception e) {
      e.printStackTrace();
    }
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
