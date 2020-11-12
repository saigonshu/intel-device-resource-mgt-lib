/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud;

import com.openiot.cloud.alarm.AlarmMain;
import com.openiot.cloud.sdk.service.IConnectRequest;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AlarmMain.class})
public class AppTest extends TestCase {
  @Value(value = "${spring.application.name:app_name_undefine}")
  private String appName;

  @Test
  public void testPing() throws Exception {
    IConnectRequest request1 =
            IConnectRequest.create(
                    HttpMethod.GET, "/ping/"+appName, MediaType.TEXT_PLAIN, "anything".getBytes());
    AtomicBoolean result = new AtomicBoolean(false);
    CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
    request1.send(
            response -> {
              System.out.println("receive a response " + response);
              assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);

              result.set(true);
              completableFuture.complete(true);
            },
            1,
            TimeUnit.SECONDS);

    completableFuture.get(1, TimeUnit.SECONDS);
    assertThat(result.get()).isTrue();
  }

}
