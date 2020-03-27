/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.testbase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.openiot.cloud.base.help.ConstDef;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.iotivity.cloud.base.device.IRequestChannel;
import org.iotivity.cloud.base.protocols.IRequest;
import org.iotivity.cloud.base.protocols.IResponse;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.enums.ContentFormat;
import org.iotivity.cloud.base.protocols.enums.RequestMethod;
import org.iotivity.cloud.base.protocols.enums.ResponseStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class TestUtil {

  @Value(value = "${test.timeout:10000}")
  Long timeout;

  @Autowired
  MongoTemplate mo;

  Map<String, List<String>> fileLines = new HashMap<String, List<String>>(); // for readLines
  Map<String, String> fileContent = new HashMap<String, String>(); // for readAll

  public List<String> readLines(String fileName) {
    if (fileLines.get(fileName) == null) {
      try {
        fileLines.put(fileName,
                      Files.readAllLines(Paths.get(getClass().getResource(fileName).getFile())));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return fileLines.getOrDefault(fileName, null);
  }

  public String readAll(String fileName) {
    if (fileContent.get(fileName) == null) {
      try {
        fileContent.put(fileName,
                        new String(Files.readAllBytes(Paths.get(getClass().getResource(fileName)
                                                                          .getFile()))));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return fileContent.getOrDefault(fileName, null);
  }

  String fillId(String coll, int seq) {
    String id = null;
    Object o = mo.getCollection(coll).find().skip(seq - 1).first().get(ConstDef.F_ID);
    if (o instanceof ObjectId) {
      id = ((ObjectId) o).toHexString();
    } else {
      id = (String) o;
    }

    if (id == null) {
      return "";
    }

    return id;
  }

  /**
   * Recursively fill all id fields in format of "$Collection:$seq:TOFILL"
   *
   * @param jEle
   * @return
   */
  Object fillIds(Object jEle) {
    if (jEle instanceof JSONObject) {
      JSONObject o = (JSONObject) jEle;
      Iterator<?> it = o.keys();
      while (it.hasNext()) {
        String key = (String) it.next();
        try {
          o.put(key, fillIds(o.get(key)));
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    } else if (jEle instanceof JSONArray) {
      JSONArray a = (JSONArray) jEle;
      JSONArray na = new JSONArray(); // can't reuse jEle because of
      // ConcurrentModification Exception
      for (int i = 0; i < a.length(); i++) {
        try {
          na.put(fillIds(a.get(i)));
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      jEle = na;
    } else {
      String[] v = jEle.toString().split(":");
      if (v.length == 3 && v[2].equals("TOFILL")) {
        jEle = fillId(v[0], Integer.parseInt(v[1]));
      }
    }

    return jEle;
  }

  /**
   * @param fileName
   * @param objectNumber, starts from 1
   * @return
   */
  public Object getJson(String fileName, int objectNumber) {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode obj = null;
    try {
      obj = mapper.readTree(Files.newBufferedReader(Paths.get(getClass().getResource(fileName)
                                                                        .getFile())));
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (obj == null) {
      return null;
    } else if (!obj.isArray()) {
      return null;
    }

    try {
      return fillIds(JSONParser.parseJSON(obj.get(objectNumber - 1).toString()));
    } catch (JSONException e) {
      e.printStackTrace();
      return null;
    }
  }

  public Object getJson(String fileName) {
    try {
      return fillIds(JSONParser.parseJSON(readAll(fileName)));
    } catch (JSONException e) {
      e.printStackTrace();
      return null;
    }
  }

  public Object getJson(Object obj) {
    try {
      return JSONParser.parseJSON(new ObjectMapper().writeValueAsString(obj));
    } catch (JsonProcessingException | JSONException e) {
      e.printStackTrace();
      return null;
    }
  }

  public Object getJson(byte[] bytes) {
    try {
      return JSONParser.parseJSON(new String(bytes));
    } catch (JSONException e) {
      e.printStackTrace();
      return null;
    }
  }

  public String getId(String fileName, int line) {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode obj = null;
    try {
      obj = mapper.readTree(Files.newBufferedReader(Paths.get(getClass().getResource(fileName)
                                                                        .getFile())));
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (obj == null) {
      return null;
    } else if (!obj.isArray()) {
      return null;
    }
    return obj.get(line - 1).get("_id").toString();
  }

  void saveAJsonString(String jstring, String collectionName) {
    DBObject toSave = (DBObject) JSON.parse(jstring);
    mo.save(toSave, collectionName);
  }

  public void importTestDb(String... collNames) {
    for (String name : collNames) {
      String fileName = TestConstDef.R_IM_PREFIX + name + TestConstDef.R_IM_SUFFIX;

      System.out.println("import " + fileName);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode obj = null;
      try {
        obj = mapper.readTree(Files.newBufferedReader(Paths.get(getClass().getResource(fileName)
                                                                          .getFile())));
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (obj == null) {
        return;
      } else if (obj.isArray()) {
        String collectionName = name;
        obj.forEach(s -> {
          DBObject toSave = (DBObject) JSON.parse(s.toString());
          mo.save(toSave, collectionName);
        });
      } else {
        DBObject toSave = (DBObject) JSON.parse(obj.toString());
        mo.save(toSave, name);
      }
    }
  }

  public static void importTestDb(MongoTemplate mo, String... collNames) throws IOException {
    for (String name : collNames) {
      String fileName = TestConstDef.R_IM_PREFIX + name + TestConstDef.R_IM_SUFFIX;

      System.out.println("import " + fileName);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode obj = null;
      try {
        Resource resource = new ClassPathResource(fileName);
        if (resource.exists() && resource.isFile()) {
          obj = mapper.readTree(Files.newBufferedReader(Paths.get(resource.getURI())));
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (obj == null) {
        return;
      } else if (obj.isArray()) {
        String collectionName = name;
        obj.forEach(s -> {
          DBObject toSave = (DBObject) JSON.parse(s.toString());
          mo.save(toSave, collectionName);
        });
      } else {
        DBObject toSave = (DBObject) JSON.parse(obj.toString());
        mo.save(toSave, name);
      }
    }
  }

  public void dropCollection(String... collNames) {
    for (String name : collNames) {
      mo.getCollection(name).drop();
    }
  }

  public IResponse sendIRequest(String conn, IRequest request) {
    IRequestChannel requestChannel = ConnectorPool.getConnection(conn);
    if (requestChannel == null) {
      log.warn("can not get such {} reuqest channel", conn);
      return MessageBuilder.createResponse(request, ResponseStatus.SERVICE_UNAVAILABLE);
    }

    CompletableFuture<IResponse> fResp = new CompletableFuture<>();
    requestChannel.sendRequest(request, fResp::complete);

    try {
      return fResp.get(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      log.error("meet an exception when sending a request", e);
      return MessageBuilder.createResponse(request, ResponseStatus.GATEWAY_TIMEOUT);
    }
  }

  public IResponse sendIRequest(String conn, RequestMethod method, String uri, String query,
                                byte[] payload) {
    return sendIRequest(conn,
                        MessageBuilder.createRequest(method,
                                                     uri,
                                                     query,
                                                     ContentFormat.APPLICATION_JSON,
                                                     payload));
  }

  public IResponse sendIRequest(String conn, RequestMethod method, String uri, String query,
                                String payloadFile) {
    return sendIRequest(conn,
                        MessageBuilder.createRequest(method,
                                                     uri,
                                                     query,
                                                     ContentFormat.APPLICATION_JSON,
                                                     getJson(payloadFile).toString().getBytes()));
  }

  public IResponse sendIRequest(String conn, RequestMethod method, String uri, String query,
                                ContentFormat format, String payloadFile) {
    return sendIRequest(conn,
                        MessageBuilder.createRequest(method,
                                                     uri,
                                                     query,
                                                     format,
                                                     getJson(payloadFile).toString().getBytes()));
  }

  public IResponse sendIRequest(String conn, RequestMethod method, String uri, String query,
                                ContentFormat format, byte[] payload) {
    return sendIRequest(conn, MessageBuilder.createRequest(method, uri, query, format, payload));
  }
}
