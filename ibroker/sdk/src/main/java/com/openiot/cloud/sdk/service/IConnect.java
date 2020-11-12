/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jms.Destination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class IConnect {
  private static final Logger logger = LoggerFactory.getLogger(IConnect.class.getName());

  // arguments for mq
  // private AmqpMqClient mqClient;
  @Autowired private JmsMqClient mqClient;

  @Value(value = "${mq.host:localhost}")
  private String mqServerHost;

  @Value(value = "${mq.user:system}")
  private String mqUser;

  @Value(value = "${mq.password:manager}")
  private String mqPassword;

  @Value(value = "${spring.application.name:app_name_undefine}")
  private String appName;

  private static IConnect instance;

  static IConnect getInstance() {
    return instance;
  }

  @PostConstruct
  public void init() {
    // mqClient = new AmqpMqClient(mqServerHost, mqUser, mqPassword);
    instance = this;
    logger.info(
        "IConnect init " + System.identityHashCode(this) + "," + System.identityHashCode(mqClient));
    if (JmsMqClient.class.isAssignableFrom((mqClient.getClass()))) {
      mqClient.addMessageListener(new IConnectJMSMessageListener(pingService()));
    } else {
      mqClient.addMessageListener(new IConnectAmqpMessageListener(pingService()));
    }
  }

  private IConnectService pingService() {
    IConnectService pingSvc = new IConnectService();
    String url = "/ping/" + appName;
    pingSvc.addHandler(url, req->{
      IConnectResponse.createFromRequest(req, HttpStatus.OK, MediaType.TEXT_PLAIN, String.join("pong from ", appName).getBytes())
              .send(); });
    return pingSvc;
  }

  public void startService(IConnectService service) {
    if (service == null) {
      logger.error("Null service instance to start");
      return;
    }

    // add message listener
    logger.debug("mqClient " + mqClient.getClass());
    if (JmsMqClient.class.isAssignableFrom((mqClient.getClass()))) {
      mqClient.addMessageListener(new IConnectJMSMessageListener(service));
    } else {
      mqClient.addMessageListener(new IConnectAmqpMessageListener(service));
    }

    // execute some extra steps in service
    service.onInit();
  }

  // for IConnectResponse send only, so no response handler and timeout
  @Deprecated
  public void send(String dst, IConnectResponse response) {
    mqClient.send(dst, response);
  }

  public void send(
      IConnectRequest iConnectRequest,
      IConnectResponseHandler handler,
      int timeout,
      TimeUnit unit) {
    mqClient.send(iConnectRequest, handler, timeout, unit);
  }

  public void send(Destination dst, IConnectResponse response) {
    mqClient.send(dst, response);
  }

  public void disConnectAll() {}

  @PreDestroy
  public void destroy() {
    logger.info("IConnect destroy " + System.identityHashCode(this));
    // mqClient.deregisterClient();
    disConnectAll();
  }
}
