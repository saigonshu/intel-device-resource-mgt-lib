/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.iot.ams.entity.AmsClient;
import com.intel.iot.ams.repository.AmsClientDao;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class ClientMgrAPIsTest {
  @Value(value = "${spring.application.name:app_name_undefine}")
  private String appName;

  @Autowired private AmsClientDao amsClientDao;
  @Autowired private ClientMgrAPIs clientMgrAPIs;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private WebApplicationContext webCtx;

  private List<AmsClient> amsClientList = new ArrayList<>();

  @After
  public void tearDown() throws Exception {
    amsClientDao.deleteAll(amsClientList);
  }

  @Test
  public void testBasic() throws Exception {
    amsClientList.add(new AmsClient());

    AmsClient amsClient = amsClientList.get(amsClientList.size() - 1);
    amsClient.setBits("64");
    amsClient.setClientUuid("melon");
    amsClient.setCpu("x86");
    amsClient.setOs("linux");
    amsClient.setProvisionTime(new Date());
    amsClient.setSerial("melon");
    amsClient.setTemplateName("plum");

    // PUT /ams_user_cloud/ams/v1/ams_client
    ResponseEntity<String> response = clientMgrAPIs.createClient(amsClient);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // GET /ams_user_cloud/ams/v1/ams_client
    response =
        clientMgrAPIs.getAmsClientInformation(
            amsClient.getClientUuid(), null, null, null, null, 0, 10);
    log.info("response = {}", response);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"template_name\":\"plum\"");
  }

  @Test
  public void testPing() throws Exception {
    MockMvc webMock = MockMvcBuilders.webAppContextSetup(webCtx).build();
    MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.get("/ams_user_cloud/ping");
    try {
      ResultActions resultActions = webMock.perform( mockHttpServletRequestBuilder);
      MockHttpServletResponse result = resultActions.andReturn().getResponse();
      resultActions.andExpect(status().isOk());
      assertThat(result.getContentAsString()).isNotNull().contains("pong");
    } catch (Exception e) {
      e.printStackTrace();
    }  }


}
