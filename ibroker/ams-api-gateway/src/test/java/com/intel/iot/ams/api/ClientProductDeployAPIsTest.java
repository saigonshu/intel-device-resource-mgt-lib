/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.iot.ams.entity.*;
import com.intel.iot.ams.repository.*;
import com.intel.iot.ams.service.*;
import com.intel.iot.ams.utils.AmsConstant;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class ClientProductDeployAPIsTest {
  @Autowired private AmsClientDao amsClientDao;
  @Autowired private AmsClientService amsClientSvr;
  @Autowired private ProductDeployService deploySrv;
  @Autowired private ProductService pSrv;
  @Autowired private ProductInstanceDao piDao;
  @Autowired private ApiProfilesDao apiDao;
  @Autowired private ProductPropertyDao ppDao;

  @Autowired private ClientProductDeployAPIs productDeployAPIs;
  @Autowired private ProductMgrAPIs productMgrAPIs;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AmsConstant AmsConst;

  @Autowired private WebApplicationContext webCtx;

  @Before
  public void setup() throws Exception {
    piDao.deleteAll();
    ppDao.deleteAll();
    apiDao.deleteAll();
    amsClientDao.deleteAll();
    Optional.ofNullable(pSrv.findAll()).ifPresent(ps->ps.forEach(p->pSrv.removeByUUID(p.getUuid())));
    Optional.ofNullable(deploySrv.findAll(0, Integer.MAX_VALUE)).ifPresent(ps->ps.forEach(p->deploySrv.removeByClientUUID(p.getClientUuid())));

    // prepare product and product instance
    String pathUnzip = unzipResource("plcvm.zip");
    assertThat(pathUnzip).isNotNull();
    ResponseEntity<String> response = productMgrAPIs.handleZipPkgUpload(pathUnzip, null);
    assertThat(response.getBody()).isNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // check cfg record
    List<ProductInstance> plcvmInstances = piDao.findByProductName("plcvm");
    assertThat(plcvmInstances).isNotNull().hasSize(2);
    int piIndex = plcvmInstances.get(0).getCpu().equals("IA")?0:1;
    assertThat(plcvmInstances.get(piIndex)).isNotNull()
            .hasFieldOrPropertyWithValue("productName", "plcvm")
            .hasFieldOrPropertyWithValue("version", "1.0")
            .hasFieldOrPropertyWithValue("cpu", "IA")
            .hasFieldOrPropertyWithValue("os", "Linux")
            .hasFieldOrPropertyWithValue("osMin", "4.4.0")
            .hasFieldOrPropertyWithValue("system", "Ubuntu")
            .hasFieldOrPropertyWithValue("sysMin", "16.04")
            .hasFieldOrPropertyWithValue("bits", "64bit");


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

  @After
  public void tearDown() throws Exception {
    piDao.deleteAll();
    ppDao.deleteAll();
    apiDao.deleteAll();
    amsClientDao.deleteAll();
    Optional.ofNullable(pSrv.findAll()).ifPresent(ps->ps.forEach(p->pSrv.removeByUUID(p.getUuid())));
    Optional.ofNullable(deploySrv.findAll(0, Integer.MAX_VALUE)).ifPresent(ps->ps.forEach(p->deploySrv.removeByClientUUID(p.getClientUuid())));

    deleteDir(AmsConst.tempPath);
  }

  @Test
  public void testDeployProdcutSuccess(){
    MockMvc webMock = MockMvcBuilders.webAppContextSetup(webCtx).build();
    AmsClient client = amsClientSvr.findByClientUUID("melon");
    assertThat(client)
            .hasFieldOrPropertyWithValue("serial", "melon")
            .hasFieldOrPropertyWithValue("clientUuid", "melon");

    // test successfully get for subclass
    String cate = AmsConstant.ProductCategory.managed_app.name();
    MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.post("/ams_user_cloud/ams/v1/product/deploy" );
    mockHttpServletRequestBuilder.content(String.format(
            "{\"client_uuid\":\"%s\",\"product_name\":\"plcvm\",\"category\":\"runtime_engine\",\"version\":\"1.0\"}",
            client.getClientUuid()));
    mockHttpServletRequestBuilder.contentType(MediaType.APPLICATION_JSON);
    ResultActions resultActions;
    try {
      resultActions = webMock.perform( mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      assertThat(result.getContentAsString()).isNotNull().isEqualTo("");
      resultActions.andExpect(status().isOk());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // test deploy record
    List<ProductDeploy> deploys = deploySrv.findByClientUUID(client.getClientUuid());
    assertThat(deploys).isNotNull().hasSize(1);
    assertThat(deploys.get(0)).isNotNull()
            .hasFieldOrPropertyWithValue("clientUuid", client.getClientUuid())
            .hasFieldOrPropertyWithValue("productName", "plcvm")
            .hasFieldOrPropertyWithValue("version", "1.0");
  }

  @Test
  public void testDeployProdcutAgainSuccess(){
    MockMvc webMock = MockMvcBuilders.webAppContextSetup(webCtx).build();
    AmsClient client = amsClientSvr.findByClientUUID("melon");
    assertThat(deploySrv.findByClientUuidAndProductNameAndVersion(client.getClientUuid(), "plcvm", "1.0")).isNull();
    assertThat(client)
            .hasFieldOrPropertyWithValue("serial", "melon")
            .hasFieldOrPropertyWithValue("clientUuid", "melon");

    // test successfully get for subclass
    String cate = AmsConstant.ProductCategory.managed_app.name();
    MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.post("/ams_user_cloud/ams/v1/product/deploy" );
    mockHttpServletRequestBuilder.content(String.format(
            "{\"client_uuid\":\"%s\",\"product_name\":\"plcvm\",\"category\":\"runtime_engine\",\"version\":\"1.0\"}",
            client.getClientUuid()));
    mockHttpServletRequestBuilder.contentType(MediaType.APPLICATION_JSON);
    ResultActions resultActions;
    try {
      assertThat(deploySrv.findByClientUuidAndProductNameAndVersion(client.getClientUuid(), "plcvm", "1.0")).isNull();
      resultActions = webMock.perform( mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      assertThat(result.getContentAsString()).isNotNull().isEqualTo("");
      resultActions.andExpect(status().isOk());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // deploy again
    try {
      resultActions = webMock.perform( mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      assertThat(result.getContentAsString()).isNotNull().isEqualTo("AMS has already product version package for this client");
      resultActions.andExpect(status().isOk());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // test deploy record
    List<ProductDeploy> deploys = deploySrv.findByClientUUID(client.getClientUuid());
    assertThat(deploys).isNotNull().hasSize(1);
    assertThat(deploys.get(0)).isNotNull()
            .hasFieldOrPropertyWithValue("clientUuid", client.getClientUuid())
            .hasFieldOrPropertyWithValue("productName", "plcvm")
            .hasFieldOrPropertyWithValue("version", "1.0");
  }

  @Test
  public void testDeployProdcutFail(){
    MockMvc webMock = MockMvcBuilders.webAppContextSetup(webCtx).build();
    AmsClient client = amsClientSvr.findByClientUUID("melon");
    assertThat(client)
            .hasFieldOrPropertyWithValue("serial", "melon")
            .hasFieldOrPropertyWithValue("clientUuid", "melon");
    client.setOsVer("1.1.0");
    amsClientDao.saveAndFlush(client);

    // test successfully get for subclass
    String cate = AmsConstant.ProductCategory.managed_app.name();
    MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.post("/ams_user_cloud/ams/v1/product/deploy" );
    mockHttpServletRequestBuilder.content(String.format(
            "{\"client_uuid\":\"%s\",\"product_name\":\"plcvm\",\"category\":\"runtime_engine\",\"version\":\"1.0\"}",
            client.getClientUuid()));
    mockHttpServletRequestBuilder.contentType(MediaType.APPLICATION_JSON);
    ResultActions resultActions;
    try {
      resultActions = webMock.perform( mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      assertThat(result.getContentAsString()).isNotNull();
      resultActions.andExpect(status().isNotFound());
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
