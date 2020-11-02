/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.intel.iot.ams.api;

import com.google.common.base.Enums;
import com.google.gson.*;
import com.intel.iot.ams.api.requestbody.*;
import com.intel.iot.ams.api.requestbody.BpkManifestInfo.ProductDep;
import com.intel.iot.ams.api.requestbody.InstallationPackageInfo.CfgIdInfo;
import com.intel.iot.ams.api.requestbody.InstallationPackageInfo.PropertyItem;
import com.intel.iot.ams.api.requestbody.ManifestInfo.DependencyInfo;
import com.intel.iot.ams.entity.*;
import com.intel.iot.ams.service.*;
import com.intel.iot.ams.task.AmsTaskType;
import com.intel.iot.ams.utils.AmsConstant;
import com.intel.iot.ams.utils.AmsConstant.ProductCategory;
import com.intel.iot.ams.utils.ArrayUtils;
import com.intel.iot.ams.utils.FileAndDirUtils;
import com.intel.iot.ams.utils.HashUtils;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * RESTAPIs implementation of APIs document Chapter 2.1 Product management
 *
 * @author Zhang, Yi Y <yi.y.zhang@intel.com>
 */
@Slf4j
@RestController
@RequestMapping("/ams_user_cloud")
public class ProductMgrAPIs {
  private static final Logger logger = LoggerFactory.getLogger(ProductMgrAPIs.class);

  @Autowired private ProductService pSrv;

  @Autowired private ProductInstanceService piSrv;

  @Autowired private ProductDeployService deploySrv;

  @Autowired private ProductPropertyService ppSrv;

  @Autowired private ClientDeviceMappingService mappingSrv;

  @Autowired private ProductDependencyService pdSrv;

  @Autowired private AmsClientService clientSrv;

  @Autowired private AmsTaskService taskSrv;

  @Autowired private CfgIdentifierService cfgIdSrv;

  @Autowired private CfgContentService cfgCntSrv;

  @Autowired private ProductChangesService changeSrv;

  @Autowired private LogService LogSrv;

  @Autowired private ApiProfileService apiSrv;

  @Autowired private AmsConstant AmsConst;
  // ----------------------------------------------------------------
  //
  // RESTful APIs
  //
  // ----------------------------------------------------------------

  /**
   * ----------------------------------------------------------------
   *
   * <p>Chapter 2.1.1 Query product information
   *
   * <p>----------------------------------------------------------------
   */

  /**
   * Get the product information.
   *
   * <p>RESTful API: GET /ams/v1/product
   *
   * @param uuid the product UUID
   * @param name the product name
   * @param vendor the product vendor
   * @return an instance of ResponseEntity<String> with response code and application information
   *     data in a JSON format string
   */
  @RequestMapping(
      value = "/ams/v1/product",
      produces = "application/json",
      method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<String> getProductInfo(
      @RequestParam(value = "uuid", required = false) String uuid,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "category", required = false) String categoryStr,
      @RequestParam(value = "vendor", required = false) String vendor,
      @RequestParam(value = "supporting_runtime_name", required = false) String supporting_runtime_name,
      @RequestParam(value = "supporting_runtime_ver", required = false) String supporting_runtime_ver) {

    List<Product> pList = null;
    JsonObject jResult = new JsonObject();
    JsonArray jProductArray = new JsonArray();
    jResult.add("product_list", jProductArray);

    if (uuid != null) {
      if (name != null || categoryStr != null || vendor != null) {
        return new ResponseEntity<String>(
            "Query parameter \"uuid\" cannot used together with other parameter",
            HttpStatus.BAD_REQUEST);
      }
      Product p = pSrv.findByUUID(uuid);
      if (p == null) {
        return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
      }
      return new ResponseEntity<String>(productSerialize(p, null).toString(), HttpStatus.OK);
    }

    if (name != null) {
      if (categoryStr != null || vendor != null) {
        return new ResponseEntity<String>(
            "Query parameter \"name\" cannot used together with other parameter",
            HttpStatus.BAD_REQUEST);
      }
      Product p = pSrv.findByName(name);
      if (p == null) {
        return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
      }
      return new ResponseEntity<String>(productSerialize(p, null).toString(), HttpStatus.OK);
    }

    if (vendor != null) {
      if (categoryStr != null) {
        try {
          Integer category = ProductCategory.valueOf(categoryStr.toLowerCase()).toValue();
          pList = pSrv.findByVendorAndCategory(vendor, category);
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
          return new ResponseEntity<String>(
                  "Query parameter \"category\" value is invalid!", HttpStatus.BAD_REQUEST);
        }
      } else {
        pList = pSrv.findByVendor(vendor);
      }
    }

    if (categoryStr != null && vendor == null) {
      try {
        Integer category = ProductCategory.valueOf(categoryStr.toLowerCase()).toValue();
        pList = pSrv.findByVendorAndCategory(vendor, category);
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
        return new ResponseEntity<String>(
                "Query parameter \"category\" value is invalid!", HttpStatus.BAD_REQUEST);
      }
    }

    /**
     * find compatible managed app with supporting_runtime_name &
     *                                  supporting_runtime_ver &
     *                                  category = "runtime_engine"
     */
    if (supporting_runtime_name != null && supporting_runtime_ver != null && categoryStr != null) {
      Integer category = ProductCategory.valueOf(categoryStr.toLowerCase()).toValue();

      /* category must be "managed_app" */
      if (category != ProductCategory.managed_app.toValue()) {
         return new ResponseEntity<String>(
                "Query parameter \"category\" must be \"managed_app\" when it is used with supporting_runtime_name and supporting_runtime_ver!",
                HttpStatus.BAD_REQUEST);
      }

      List<ApiProfiles> rt_apiList = apiSrv.findByProductNameAndProductVersion(supporting_runtime_name, supporting_runtime_ver);
      /**
       *  compatible managed app softwares should has the same api count and name:
       *    - get all api name in rt_apList
       *    - get all api name in managed app ApiProfiles list
       *  compare if these 2 api string list are the same
       */
      if (rt_apiList == null) {
        return new ResponseEntity<String>(
          "Can't find any apiprofile of this supporting runtime engine", HttpStatus.BAD_REQUEST);
      }
      List<String> rt_apiStr = rt_apiList.stream()
                                         .map(x -> x.getApi())
                                         .collect(Collectors.toList());
      rt_apiStr.sort(Comparator.comparing(String::hashCode));

      List<Product> all_mng_prodList = pSrv.findByCategory(ProductCategory.managed_app.toValue());
      for (Product p : all_mng_prodList) {
        /* 1. get all version of this product */
        List<String> versionList = piSrv.getVersionsByProductName(p.getName());
        List<String> matchedVerList = null;

        for (String v : versionList) {
          boolean IsMatch = true;

          /* 2. get all apis string in mng_apiList */
          List<ApiProfiles> mng_apiList = apiSrv.findByProductNameAndProductVersion(p.getName(), v);
          if (mng_apiList == null) { break; }
          List<String> mng_apiStr = mng_apiList.stream().map(x -> x.getApi())
                                               .collect(Collectors.toList());
          mng_apiStr.sort(Comparator.comparing(String::hashCode));

          /* 3. compare rt_apiStr with rt_apiStr before loop */
          if (!mng_apiStr.toString().equals(rt_apiStr.toString())) { break; }

          /* 4. compare managed_app's apis with runtime_engine's */
          for (ApiProfiles r : rt_apiList) {
            for (ApiProfiles m : mng_apiList) {
              if (m.getApi().equals(r.getApi()) && m.getLevel() > r.getBackward()) {
                logger.info("skip product instance for level {} > {} in api {}", m.getLevel(), r.getBackward(), r.getApi());
                IsMatch = false;
                break;
              }
              continue;
            }
            if (!IsMatch) { break; }
          }
          /* check if this version of managed_app match */
          if (IsMatch) {
            matchedVerList.add(v);
            logger.info("found product instance: {} {}", p.getName(), v);
          }
        }

        /* if this product has one compatible version at least, it's matched */
        if (matchedVerList != null) {
          jProductArray.add(productSerialize(p, matchedVerList));
        }
      }

      if (jProductArray != null) {
        String ret = jResult.toString();
        logger.info("api response: {}", ret);
        return new ResponseEntity<String>(ret, HttpStatus.OK);
      }
    }

    if (uuid == null && name == null && categoryStr == null && vendor == null) {
      pList = pSrv.findAll();
    }

    if (pList == null) {
      logger.info("api response: {}", HttpStatus.NOT_FOUND);
      return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
    }

    for (Product p : pList) {
      jProductArray.add(productSerialize(p, null));
    }

    String ret = jResult.toString();
    logger.info("api response: {}", ret);
    return new ResponseEntity<String>(ret, HttpStatus.OK);
  }

  /**
   * ----------------------------------------------------------------
   *
   * <p>Chapter 2.1.2 Upload software product installation package
   *
   * <p>----------------------------------------------------------------
   */

  /**
   * Upload a product installation package
   *
   * <p>RESTful API: POST /ams/v1/product/upload
   *
   * @return an instance of ResponseEntity<String> with response code in a JSON format string
   */
  @RequestMapping(
      value = "/ams/v1/product/upload",
      produces = "application/json",
      method = RequestMethod.POST)
  public ResponseEntity<String> uploadProductPackage(
      @RequestParam("file") MultipartFile file, HttpServletRequest request) {

    String uploadName = file.getOriginalFilename();
    if (uploadName.endsWith(".bpk")) {
      return handleBpkPkgUpload(file, request);
    } else if (uploadName.endsWith(".zip")) {
      try {
        /** Save the uploaded file on AMS local space */
        String temp_filename = String.valueOf(new Date().getTime());
        FileAndDirUtils.saveFile(file, AmsConst.tempPath, temp_filename+".zip");
        String tempDest = AmsConst.tempPath + temp_filename;
        if ( !FileAndDirUtils.unzipAll(tempDest+".zip", tempDest) ){
          return new ResponseEntity<String>(String.format("invalid zip format file:%s", uploadName), HttpStatus.BAD_REQUEST);
        }
        return handleZipPkgUpload(tempDest, request);
      } catch (IOException e) {
        return new ResponseEntity<String>("/O ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (ZipException e) {
        return new ResponseEntity<String>("ZIP FILE ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } else {
      return new ResponseEntity<String>("Unsupported package type!", HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * ----------------------------------------------------------------
   *
   * <p>Chapter 2.1.3 Update product information
   *
   * <p>----------------------------------------------------------------
   */

  /**
   * Update the product description or default version
   *
   * <p>RESTful API: POST /ams/v1/product
   *
   * @param info the POST payload data
   * @return an instance of ResponseEntity<String> with response code
   */
  @RequestMapping(
      value = "/ams/v1/product",
      produces = "application/json",
      method = RequestMethod.POST)
  public ResponseEntity<String> updateProductInfo(@RequestBody UpdateProductInfo info) {

    if (info == null
        || info.getProductName() == null || info.getDescription() == null) {

      return new ResponseEntity<String>(
          "POST payload format is not correct", HttpStatus.BAD_REQUEST);
    }

    Product p = pSrv.findByName(info.getProductName());
    if (p == null) {
      return new ResponseEntity<String>(
          "No such product: " + info.getProductName(), HttpStatus.BAD_REQUEST);
    }

    if (info.getDescription() != null) {
      p.setDescription(info.getDescription());
    }

    pSrv.update(p);

    return new ResponseEntity<String>(HttpStatus.OK);
  }

  /**
   * ----------------------------------------------------------------
   *
   * <p>Chapter 2.1.4 Delete product
   *
   * <p>----------------------------------------------------------------
   */

  /**
   * Delete a product or a product version from AMS
   *
   * <p>RESTful API: DELETE /ams/v1/product
   *
   * @param uuid the product UUID
   * @param name the product name
   * @param version the product version
   * @return an instance of ResponseEntity<String> with response code
   */
  @RequestMapping(
      value = "/ams/v1/product",
      produces = "application/json",
      method = RequestMethod.DELETE)
  public ResponseEntity<String> deleteProduct(
      @RequestParam(value = "uuid", required = false) String uuid,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "version", required = false) String version) {

    Product p = null;

    if (uuid == null && name == null) {
      return new ResponseEntity<String>(
          "Query parameter \"uuid\" or \"name\" is required.", HttpStatus.BAD_REQUEST);
    }

    if (uuid != null && name != null) {
      p = pSrv.findByUuidAndName(uuid, name);
      if (p == null) {
        return new ResponseEntity<String>(
            "Query parameter \"uuid\" and \"name\" are not match.", HttpStatus.BAD_REQUEST);
      }
    }

    if (uuid != null && name == null) {
      p = pSrv.findByUUID(uuid);
      if (p == null) {
        return new ResponseEntity<String>("No such Product: " + name, HttpStatus.NOT_FOUND);
      }
    }

    if (uuid == null && name != null) {
      p = pSrv.findByName(name);
      if (p == null) {
        return new ResponseEntity<String>("No such Product: " + uuid, HttpStatus.NOT_FOUND);
      }
    }

    if (version != null) {
      List<ProductInstance> piList = piSrv.findByNameAndVersion(p.getName(), version);
      if (piList == null) {
        return new ResponseEntity<String>(
            "Product: " + p.getName() + " doesnot have version: " + version + "!",
            HttpStatus.NOT_FOUND);
      }

      /** Delete Product instance */
      piSrv.removeByNameAndVersion(p.getName(), version);

      /** Delete Product deploy */
      if (!p.getName().equals("ams_client")) {
        deploySrv.removeByProductNameAndVersion(p.getName(), version);
      }

      /** Delete Product version from repository */
      try {
        if (p.getCategory() == ProductCategory.imrt_app.toValue()) {
          new File(AmsConst.repoPath + p.getName() + "/" + version + ".bpk").delete();
        } else {
          FileUtils.deleteDirectory(new File(AmsConst.repoPath + p.getName() + "/" + version));
        }
      } catch (IOException e) {
        /** TODO: clean */
        e.printStackTrace();
        return new ResponseEntity<String>("IOException", HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } else {
      /** Delete Product deploy */
      if (!p.getName().equals("ams_client")) {
        deploySrv.removeByProductName(p.getName());
      }

      /** Delete Product instance */
      piSrv.removeByName(p.getName());

      /** Delete Client Device Mapping */
      mappingSrv.removeByProductName(p.getName());

      /** Delete Product Property */
      ppSrv.removeByName(p.getName());

      /** TODO: Delete Product dependencies */

      /** Delete Product */
      pSrv.removeByUUID(p.getUuid());

      /** Delete Product from repository */
      try {
        FileUtils.deleteDirectory(new File(AmsConst.repoPath + p.getName()));
      } catch (IOException e) {
        /** TODO: clean */
        e.printStackTrace();
        return new ResponseEntity<String>("IOException", HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

    /** Create AmsTask to calculate product changes of all clients */
    List<AmsClient> clientList = clientSrv.findAll();
    if (clientList != null) {
      for (AmsClient client : clientList) {
        /** Remove old product changes of this client */
        changeSrv.removeByClientUuid(client.getClientUuid());

        AmsTask task = new AmsTask();
        task.setTaskType(AmsTaskType.CALCULATE_PRODUCT_CHANGES);
        task.setTaskCreateTime(new Date());
        JsonObject jTaskProperty = new JsonObject();
        jTaskProperty.addProperty("client_uuid", client.getClientUuid());
        task.setTaskProperties(jTaskProperty.toString());
        taskSrv.save(task);
      }
    }

    /** Log to MySQL */
    LogSrv.LogToMysql("Delete", "Software", "Software version is " + version + "", null);
    return new ResponseEntity<String>(HttpStatus.OK);
  }

  // ----------------------------------------------------------------
  //
  // Private functions
  //
  // ----------------------------------------------------------------

  /**
   * Serialize the Product instance to JSON Object
   *
   * @param p the Product instance to be serialized
   * @return the JSON object serialized from the Product object.
   */
  private JsonObject productSerialize(Product p, List<String> verlist) {
    List<String> versionList = null;

    JsonObject jProduct = new JsonObject();
    jProduct.addProperty("product_uuid", p.getUuid());
    jProduct.addProperty("name", p.getName());

    String category = ProductCategory.fromValue(p.getCategory()).toString();
    jProduct.addProperty("category", category);

    if (p.getDescription() != null) {
      jProduct.addProperty("description", p.getDescription());
    }

    if (p.getVendor() != null) {
      jProduct.addProperty("vendor", p.getVendor());
    }

    JsonArray jProperties = new JsonArray();

    List<ProductProperty> propertyList = ppSrv.findByName(p.getName());
    if (propertyList != null) {
      jProduct.add("properties", jProperties);
      for (ProductProperty property : propertyList) {
        JsonObject jProperty = new JsonObject();

        jProperty.addProperty("id", property.getId());
        jProperty.addProperty("key", property.getPropKey());
        if (property.getValueType() == 1) {
          jProperty.addProperty("value_type", "int");
        } else if (property.getValueType() == 2) {
          jProperty.addProperty("value_type", "double");
        } else if (property.getValueType() == 3) {
          jProperty.addProperty("value_type", "string");
        } else if (property.getValueType() == 4) {
          jProperty.addProperty("value_type", "date");
        }
        jProperty.addProperty("value", property.getPropValue());
        jProperties.add(jProperty);
      }
    }

    JsonArray jVersions = new JsonArray();
    jProduct.add("versions", jVersions);
    
    if (verlist != null) {
      versionList = verlist;
    } else {
      versionList = piSrv.getVersionsByProductName(p.getName());
    }

    if (versionList != null) {
      for (String version : versionList) {
        JsonObject jVersion = new JsonObject();

        jVersion.addProperty("version", version);
        JsonArray jInstances = new JsonArray();
        jVersion.add("instances", jInstances);

        List<ProductInstance> instanceList = piSrv.findByNameAndVersion(p.getName(), version);
        if (instanceList != null) {
          for (ProductInstance instance : instanceList) {
            JsonObject jInstance = new JsonObject();

            jInstance.addProperty("instance_id", instance.getInstanceId());
            jInstance.addProperty("instance_name", instance.getInstanceName());
            if (instance.getCpu() != null) {
              jInstance.addProperty("cpu", instance.getCpu());
            }
            if (instance.getOs() != null) {
              jInstance.addProperty("os", instance.getOs());
            }
            if (instance.getPlatform() != null) {
              jInstance.addProperty("platform", instance.getPlatform());
            }
            if (instance.getOsMin() != null) {
              jInstance.addProperty("os_min", instance.getOsMin());
            }
            if (instance.getSystem() != null) {
              jInstance.addProperty("system", instance.getSystem());
            }
            if (instance.getSysMin() != null) {
              jInstance.addProperty("sys_min", instance.getSysMin());
            }
            if (instance.getBits() != null) {
              jInstance.addProperty("bits", instance.getBits());
            }
            if (instance.getAotEnable() != null) {
              jInstance.addProperty("aot_enable", instance.getAotEnable().booleanValue());
            }
            if (instance.getWasmEnable() != null) {
              jInstance.addProperty("wasm_enable", instance.getWasmEnable().booleanValue());
            }
            if (instance.getWasmVersion() != null) {
              jInstance.addProperty("wasm_version", instance.getWasmVersion().intValue());
            }
            if (instance.getMinWasmVersion() != null) {
              jInstance.addProperty("min_wasm_version", instance.getMinWasmVersion().intValue());
            }
            if (instance.getDependencyList() != null) {
              JsonObject jDepends =
                  new JsonParser().parse(instance.getDependencyList()).getAsJsonObject();
              if (jDepends != null) {
                JsonArray jDepenArray = jDepends.get("dependencies").getAsJsonArray();
                if (jDepenArray != null) {
                  jInstance.add("dependencies", jDepenArray);
                }
              }
            }
            jInstances.add(jInstance);
          }
        }
        jVersions.add(jVersion);
      }
    }
    return jProduct;
  }

  private InstallationPackageInfo parsePkgInfo(String pkgInfoPath) {
    InstallationPackageInfo pkgInfo;
    File fPkgInfo = new File(pkgInfoPath);
    if (!fPkgInfo.exists()) {
      return null;
    }
    String pkgInfoStr = null;
    try {
      pkgInfoStr = FileUtils.readFileToString(fPkgInfo);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    Gson gson = new Gson();
    try {
      pkgInfo = gson.fromJson(pkgInfoStr, InstallationPackageInfo.class);
    } catch (JsonSyntaxException jse) {
      return null;
    }

    /** Check plugin app package.info format */
    if (pkgInfo.getCategory().toLowerCase() == "plugin_app") {
      if (pkgInfo.getHostName() == null) {
        return null;
      }
    }

    return pkgInfo;
  }

  private ManifestInfo parseManifest(String metaStr) {
    if (metaStr == null) {
      return null;
    }

    ManifestInfo metaInfo;
    Gson gson = new Gson();
    try {
      metaInfo = gson.fromJson(metaStr, ManifestInfo.class);
    } catch (JsonSyntaxException jse) {
      return null;
    }

    /** Check plugin app manifest format */
    if (metaInfo.getCategory().toLowerCase() == "plugin_app") {
      if (metaInfo.getHostName() == null || metaInfo.getDependencyList() == null) {
        return null;
      }

      boolean dFlag = false;

      for (DependencyInfo d : metaInfo.getDependencyList()) {
        if (d.getProductName().equals(metaInfo.getHostName())) {
          dFlag = true;
        }
      }
      if (dFlag == false) {
        return null;
      }
    }

    /** Check component List format */
    if (metaInfo.getComponentList() == null) {
      return null;
    }

    return metaInfo;
  }

  private PlatformInfo parsePlatform(String platformStr) {
    if (platformStr == null) {
      return null;
    }

    PlatformInfo platformInfo;
    Gson gson = new Gson();
    try {
      platformInfo = gson.fromJson(platformStr, PlatformInfo.class);
    } catch (JsonSyntaxException jse) {
      return null;
    }

    return platformInfo;
  }

  private String serializeDependencies(List<DependencyInfo> list) {

    if (list == null || list.size() == 0) {
      return null;
    }

    JsonObject jRet = new JsonObject();
    JsonArray jList = new JsonArray();

    jRet.add("dependencies", jList);
    for (DependencyInfo info : list) {
      if (info.getProductName() != null) {
        JsonObject jInfo = new JsonObject();
        jInfo.addProperty("product_name", info.getProductName());
        if (info.getMinVersion() != null) {
          jInfo.addProperty("min_version", info.getMinVersion());
        }

        jList.add(jInfo);
      }
    }

    return jRet.toString();
  }

  private DependencyList parseDependencies(String depStr) {
    if (depStr == null) {
      return null;
    }
    DependencyList depList;
    Gson gson = new Gson();
    try {
      depList = gson.fromJson(depStr, DependencyList.class);
    } catch (JsonSyntaxException jse) {
      return null;
    }

    return depList;
  }

  private byte[] getBpkManifestData(byte[] bpk_data) {
    byte[] metadata = null;
    int i = 0;
    int bpkHdrSize = 16;
    int bpkSecHdrSize = 12;
    short bpkSecNum = ArrayUtils.bytesToShort(bpk_data, (short) 6);

    for (; i < bpkSecNum; i++) {
      int secType = ArrayUtils.bytesToInt(bpk_data, bpkHdrSize + bpkSecHdrSize * i);
      /** If secType is metadata */
      if (secType == 1) {
        int secOff = ArrayUtils.bytesToInt(bpk_data, bpkHdrSize + bpkSecHdrSize * i + 4);
        int secSize = ArrayUtils.bytesToInt(bpk_data, bpkHdrSize + bpkSecHdrSize * i + 8);
        metadata = new byte[secSize];
        System.arraycopy(bpk_data, secOff, metadata, 0, secSize);
        break;
      }
    }

    return metadata;
  }

  private BpkManifestInfo getBpkManifestInfo(byte[] metadata) {

    if (metadata == null) {
      return null;
    }

    BpkManifestInfo metaInfo;
    Gson gson = new Gson();
    try {
      metaInfo = gson.fromJson(new String(metadata).trim(), BpkManifestInfo.class);
    } catch (JsonSyntaxException jse) {
      return null;
    }

    if (!checkBpkMetaFormat(metaInfo)) {
      return null;
    }

    return metaInfo;
  }

  private boolean checkBpkMetaFormat(BpkManifestInfo meta) {
    /** TODO: shall check if BPK file has all required properties */
    return true;
  }

  private String serializeBpkProductDeps(List<ProductDep> list) {
    boolean hasImrt = false;
    JsonObject jRet = new JsonObject();
    JsonArray jList = new JsonArray();

    jRet.add("dependencies", jList);
    if (list != null) {
      for (ProductDep dep : list) {
        if (dep.getProductName() != null) {
          if (dep.getProductName().equals("iMRT")) {
            hasImrt = true;
          }
          JsonObject jDep = new JsonObject();
          jDep.addProperty("product_name", dep.getProductName());
          if (dep.getMinVersion() != null) {
            jDep.addProperty("min_version", dep.getMinVersion());
          }
          jList.add(jDep);
        }
      }
    }

    /** iMRT is default dependency of iMRT app */
    if (!hasImrt) {
      JsonObject jDep = new JsonObject();
      jDep.addProperty("product_name", "iMRT");
      jList.add(jDep);
    }

    return jRet.toString();
  }

  private ResponseEntity<String> handleBpkPkgUpload(
      MultipartFile file, HttpServletRequest request) {

    try {
      /** Read BPK metadata */
      byte[] rawMeta = getBpkManifestData(file.getBytes());
      BpkManifestInfo metadata = getBpkManifestInfo(rawMeta);
      if (metadata == null) {
        return new ResponseEntity<String>("BPK manifest is not correct", HttpStatus.BAD_REQUEST);
      }

      /** Create product if necessary */
      Product p = pSrv.findByName(metadata.getAppName());
      /** Set elements of the log */
      String action = p == null ? "Add" : "Update";
      String clasS = "";
      String details = "";

      if (p == null) {
        p = new Product();
        p.setUuid(metadata.getAppUuid());
        p.setName(metadata.getAppName());
        p.setCategory(4);
        if (metadata.getVendor() != null) {
          p.setVendor(metadata.getVendor());
        }
        if (metadata.getDescription() != null) {
          p.setDescription(metadata.getDescription());
        }
      } else {
        List<ProductInstance> temp = piSrv.findByNameAndVersion(p.getName(), metadata.getVersion());
        if (temp != null) {
          // TODO: delete all temp files and dirs
          return new ResponseEntity<String>(
              "Product: "
                  + metadata.getAppName()
                  + " already has uploaded this version: "
                  + metadata.getVersion(),
              HttpStatus.CONFLICT);
        }
      }

      /** Create product instances */
      ProductInstance instance = new ProductInstance();
      instance.setProductName(p.getName());
      instance.setInstanceName("imrt_app");
      instance.setVersion(metadata.getVersion());
      instance.setDependencyList(serializeBpkProductDeps(metadata.getProductDeps()));
      instance.setUploadTime(new Date());
      instance.setDescription(metadata.getDescription());
      instance.setMetadata(new String(rawMeta).trim());

      /** Save iMRT app to repository */
      File productRoot = new File(AmsConst.repoPath + p.getName());
      if (!productRoot.exists()) {
        productRoot.mkdir();
      }
      String destDirStr = AmsConst.repoPath + p.getName() + "/" + metadata.getVersion() + ".bpk";
      file.transferTo(new File(destDirStr));

      /** Add/Update Product into DB */
      pSrv.saveOrUpdate(p);
      /** Log to MySQL */
      LogSrv.LogToMysql(
          action,
          "Software",
          ""
              + action
              + " software from the uploaded .Bkp package, software name:"
              + p.getName()
              + ", version:"
              + metadata.getVersion()
              + "",
          null);

      /** Add product instances into DB */
      piSrv.save(instance);
      /** Log to MySQL */
      LogSrv.LogToMysql(
          "Add",
          "Software",
          "Add software version from the uploaded .Bkp package, software name:"
              + instance.getProductName()
              + ", version："
              + instance.getVersion()
              + "",
          null);

      /** Add dependencies into DB, note iMRT app has at least one product dependency "iMRT" */
      DependencyList dList = parseDependencies(instance.getDependencyList());
      for (DependencyInfo d : dList.getDependencyList()) {
        ProductDependency pd = new ProductDependency();
        /** TODO: should check if dependency product exist in repo? */
        pd.setInstanceId(instance.getInstanceId());
        pd.setDependencyName(d.getProductName());
        if (d.getMinVersion() != null) {
          pd.setMinVersion(d.getMinVersion());
        }
        pdSrv.save(pd);
        /** Log to MySQL */
        LogSrv.LogToMysql(
            "Add",
            "Software",
            "Add software dependencies from the uploaded .Bkp package, name:"
                + pd.getDependencyName()
                + "",
            null);
      }

    } catch (IOException e) {
      e.printStackTrace();
      return new ResponseEntity<String>("BPK FILE I/O ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<String>(HttpStatus.OK);
  }

  public ResponseEntity<String> handleZipPkgUpload(
      String unzipPath, HttpServletRequest request) {
    Date upload_time = new Date();
    try {
      /** Read the package.info */
      InstallationPackageInfo pkgInfo = parsePkgInfo(unzipPath + "/package.info");
      if (pkgInfo == null
          || pkgInfo.getProductName() == null
          || pkgInfo.getCategory() == null
          || pkgInfo.getVersion() == null
          || (pkgInfo.getCategory().toLowerCase().equals("plugin_app")
              && pkgInfo.getHostName() == null)) {
        // TODO: delete all temp files and dirs
        return new ResponseEntity<String>(
            "\"package.info\" format is not correct or it is missed!", HttpStatus.BAD_REQUEST);
      }
      List<ProductInstance> temp = piSrv.findByNameAndVersion(pkgInfo.getProductName(), pkgInfo.getVersion());
      if (temp!=null && !temp.isEmpty()) {
        // TODO: delete all temp files and dirs
        return new ResponseEntity<String>(
                "Product: "
                        + pkgInfo.getProductName()
                        + " already has uploaded this version: "
                        + pkgInfo.getVersion(),
                HttpStatus.CONFLICT);
      }

      /** Create product if necessary */
      Product p = pSrv.findByName(pkgInfo.getProductName());

      /** Create product properties */
      String action = p == null ? "Add" : "Update";
      List<ProductProperty> propertyList = new ArrayList<ProductProperty>();
      if (p == null) {
        p = composeProduct(pkgInfo);

        /** TODO: Currently Product properties only added at the first upload */
        propertyList = composeProperties(pkgInfo);
      } else {
        p.setDescription(pkgInfo.getDescription());
      }

      /** Create configuration identifier list from package.info */
      List<CfgIdentifier> cfgIdList = composeCfgs(pkgInfo);

      /** Create product instances */
      List<ProductInstance> instances = null;
      try {
        if (p.getCategory()==ProductCategory.managed_app.toValue()) {
          instances = composeProductInstanceIndependance(unzipPath, p, pkgInfo, upload_time);
        }else if(p.getCategory()==ProductCategory.runtime_engine.toValue()){
          instances = composeProductInstanceInZip(unzipPath, p, pkgInfo, upload_time);
        }else{
          instances = composeProductInstance(unzipPath, p, pkgInfo, upload_time);
        }
      } catch (Exception e) {
        return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
      }

      /** Copy product from temp dir to repository */
      File productRoot = new File(AmsConst.repoPath + p.getName());
      if (!productRoot.exists()) {
        productRoot.mkdir();
      }

      String destDirStr = AmsConst.repoPath + p.getName() + "/" + pkgInfo.getVersion();
      logger.info(String.format("move files from %s to %s", unzipPath, destDirStr));
      File dstDir = new File(destDirStr);
      if (dstDir.exists()) FileUtils.deleteDirectory(dstDir);
      FileUtils.moveDirectory(new File(unzipPath), dstDir);

      /** Add/Update Product into DB */
      pSrv.saveOrUpdate(p);
      /** log product information to mysql */
      LogSrv.LogToMysql(
          action,
          "Software",
          ""
              + action
              + " software from uploaded .Zip package, software name is "
              + p.getName()
              + "",
          null);

      /** Add Product properties into DB */
      for (ProductProperty property : propertyList) {
        ppSrv.save(property);
      }

      /** Add configuration identifier into DB */
      for (CfgIdentifier id : cfgIdList) {
        cfgIdSrv.save(id);

        /** log config identifier info to mysql */
        LogSrv.LogToMysql(
            action,
            "Software Config",
            ""
                + action
                + " software config from uploaded .Zip package, software name:"
                + id.getUserName()
                + ", path name:"
                + id.getPathName()
                + ", target type: "
                + id.getTargetType()
                + "",
            null);
      }

      // save api profile
      if(pkgInfo.getApiProfiles()!=null){
        saveApiProfile(pkgInfo);
      }

      /** Add product instances into DB */
      for (ProductInstance instance : instances) {
        piSrv.save(instance);
        /** log software instances info to mysql */
        LogSrv.LogToMysql(
            "Add",
            "Software",
            "Add software version from uploaded .Zip package, software name:"
                + instance.getProductName()
                + ", version:"
                + instance.getVersion()
                + "",
            null);

        /** Add dependencies into DB */
        if (instance.getDependencyList() != null) {
          DependencyList dList = parseDependencies(instance.getDependencyList());
          if (dList != null && dList.getDependencyList() != null) {
            for (DependencyInfo d : dList.getDependencyList()) {
              ProductDependency pd = new ProductDependency();
              /** TODO: should check if dependency product exist in repo? */
              pd.setInstanceId(instance.getInstanceId());
              pd.setDependencyName(d.getProductName());
              if (d.getMinVersion() != null) {
                pd.setMinVersion(d.getMinVersion());
              }
              pdSrv.save(pd);
              LogSrv.LogToMysql(
                  "Add",
                  "Software",
                  "Add software dependencies from the uploaded .Zip package, name:"
                      + pd.getDependencyName()
                      + "",
                  null);
            }
          }
        }
      }

    } catch (IOException e) {
      log.error("meet an exception while handling zip file", e);
      // TODO: if exception happened, should rollback the database and
      // remove temp files
      return new ResponseEntity<String>("ZIP FILE I/O ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<String>(HttpStatus.OK);
  }

  private void saveApiProfile(InstallationPackageInfo pkgInfo) {
    apiSrv.deleteByProductNameAndProductVersion(pkgInfo.getProductName(), pkgInfo.getVersion());
    Optional<List<InstallationPackageInfo.ApiProfile>> newProfiles = Optional.ofNullable(pkgInfo.getApiProfiles());
    newProfiles.filter(ps->ps!=null && !ps.isEmpty()).ifPresent(ps->{
      ps.stream().forEach(p->{
        ApiProfiles profile = new ApiProfiles();
        profile.setProductName(pkgInfo.getProductName());
        profile.setProductVersion(pkgInfo.getVersion());
        profile.setApi(p.getApiName());
        profile.setLevel(p.getLevel());
        if(p.getBackward()!=null) profile.setBackward(p.getBackward());
        apiSrv.save(profile);
        });
      });
  }

  private List<ProductInstance> composeProductInstance(String unzipPath, Product p, InstallationPackageInfo pkgInfo, Date upload_time) throws Exception {
    List<ProductInstance> instances = new ArrayList<ProductInstance>();

    List<String> distList = new ArrayList<String>();
    File temp[] = new File(unzipPath).listFiles();
    for (int i = 0; i < temp.length; i++) {
      if (temp[i].isDirectory()) {
        distList.add(temp[i].getName());
      }
    }

    for (String dist : distList) {
      ProductInstance instance = new ProductInstance();
      instance.setInstanceName(dist);
      instance.setProductName(p.getName());
      instance.setVersion(pkgInfo.getVersion());

      // fw_app_wasm does not have platform.info as WASM is platform
      // independent
      if (p.getCategory() != ProductCategory.fw_app_wasm.toValue()) {
        String platStr =
                FileUtils.readFileToString(
                        new File(unzipPath + "/" + dist + "/ams/version/platform.info"));
        PlatformInfo platInfo = parsePlatform(platStr);
        if (platInfo == null) {
          throw new Exception(String.format("platform.info is not correct, "
                  + "Product: %s category: %s version: %s distribution: %s",
                  pkgInfo.getProductName(), pkgInfo.getCategory(), pkgInfo.getVersion(), dist));
        }
        instance.setCpu(platInfo.getCpu());
        instance.setOs(platInfo.getOs());
        if (platInfo.getOsMin() != null) {
          instance.setOsMin(platInfo.getOsMin());
        }
        if (platInfo.getSystem() != null) {
          instance.setSystem(platInfo.getSystem());
        }
        if (platInfo.getSysMin() != null) {
          instance.setSysMin(platInfo.getSysMin());
        }
        instance.setBits(platInfo.getBits());
      }

      String metaStr =
              FileUtils.readFileToString(new File(unzipPath + "/" + dist + "/ams/version/manifest"));
      ManifestInfo manifest = parseManifest(metaStr);
      if (manifest == null) {
        throw new Exception(String.format("Manifest is not correct, "
                + "Product: %s category: %s version: %s cpu: %s os: %s",
                pkgInfo.getProductName(), pkgInfo.getCategory(), pkgInfo.getVersion(), instance.getCpu(), instance.getOs()));
        // TODO: delete all temp files and dirs
      }
      if (manifest.getDependencyList() != null) {
        instance.setDependencyList(serializeDependencies(manifest.getDependencyList()));
      }
      instance.setUploadTime(upload_time);
      instance.setDescription(manifest.getDescription());
      instance.setMetadata(metaStr);
      if (p.getCategory() == ProductCategory.fw_product.toValue() || p.getCategory() == ProductCategory.fw_app_wasm.toValue()) {
        instance.setAotEnable(manifest.getAotEnable());
      }
      if (p.getCategory() == ProductCategory.fw_product.toValue()) {
        instance.setWasmEnable(manifest.getWasmEnable());
        instance.setWasmVersion(manifest.getWasmVersion());
      }
      if (p.getCategory() == ProductCategory.fw_app_wasm.toValue()) {
        instance.setMinWasmVersion(manifest.getMinWasmVersion());
      }

      instances.add(instance);
    }

    return instances;
  }

  private List<ProductInstance> composeProductInstanceIndependance(String unzipPath, Product p, InstallationPackageInfo pkgInfo, Date upload_time) throws Exception {
    List<ProductInstance> instances = new ArrayList<ProductInstance>();

    String dist = "os-any";
    List<String> distList = new ArrayList<String>();
    File temp[] = new File(unzipPath).listFiles();
    for (File f : temp) {
      String fileName = f.getName();
      if (fileName.endsWith(".zip") || fileName.endsWith("tar.gz")) {
        dist = fileName.replace(".tar.gz", "");
        break;
      }else if (fileName.endsWith(".zip")){
        dist = fileName.replace(".zip", "");
        break;
      }
    }

    ProductInstance instance = new ProductInstance();
    instance.setInstanceName(dist);
    instance.setProductName(p.getName());
    instance.setVersion(pkgInfo.getVersion());
    instance.setUploadTime(upload_time);
    instance.setMetadata("{}");
    instance.setCpu("any");
    instance.setOs("any");
    instance.setSystem("any");
    instances.add(instance);
    return instances;
  }

  private List<ProductInstance> composeProductInstanceInZip(String unzipPath, Product p, InstallationPackageInfo pkgInfo, Date upload_time) throws Exception {
    List<ProductInstance> instances = new ArrayList<ProductInstance>();

    List<String> distList = new ArrayList<String>();
    File temp[] = new File(unzipPath).listFiles();
    for (File f : temp) {
      String fileName = f.getName();
      if (fileName.endsWith(".zip")) {
        String dist = fileName.replace(".zip", "");
        distList.add(dist);
        if ( !FileAndDirUtils.unzipOne(f.getAbsolutePath(), "ams/version/platform.info", unzipPath + "/" + dist + ".info")){
          throw new Exception(String.format("platform.info is not correct, "
                          + "Product: %s category: %s version: %s distribution: %s",
                  pkgInfo.getProductName(), pkgInfo.getCategory(), pkgInfo.getVersion(), dist));
        }
      }else if (fileName.endsWith(".tar.gz")) {
        String dist = fileName.replace(".tar.gz", "");
        distList.add(dist);
        if ( !FileAndDirUtils.untarOne(f.getAbsolutePath(), "ams/version/platform.info", unzipPath + "/" + dist + ".info")){
          throw new Exception(String.format("platform.info is not correct, "
                          + "Product: %s category: %s version: %s distribution: %s",
                  pkgInfo.getProductName(), pkgInfo.getCategory(), pkgInfo.getVersion(), dist));
        }
      }
    }

    for (String dist : distList) {
      ProductInstance instance = new ProductInstance();
      instance.setInstanceName(dist);
      instance.setProductName(p.getName());
      instance.setVersion(pkgInfo.getVersion());
      instance.setUploadTime(upload_time);
      instance.setMetadata("{}");

      String platStr = FileUtils.readFileToString(new File(unzipPath + "/" + dist + ".info"));
      PlatformInfo platInfo = parsePlatform(platStr);
      if (platInfo == null) {
        throw new Exception(String.format("platform.info is not correct, "
                        + "Product: %s category: %s version: %s distribution: %s",
                pkgInfo.getProductName(), pkgInfo.getCategory(), pkgInfo.getVersion(), dist));
      }
      instance.setCpu(platInfo.getCpu());
      instance.setOs(platInfo.getOs());
      if (platInfo.getOsMin() != null) {
        instance.setOsMin(platInfo.getOsMin());
      }
      if (platInfo.getSystem() != null) {
        instance.setSystem(platInfo.getSystem());
      }
      if (platInfo.getSysMin() != null) {
        instance.setSysMin(platInfo.getSysMin());
      }
      instance.setBits(platInfo.getBits());
      instances.add(instance);
    }

    return instances;
  }

  private List<CfgIdentifier> composeCfgs(InstallationPackageInfo pkgInfo) {
    List<CfgIdentifier> cfgIds = new ArrayList<CfgIdentifier>();
    if (pkgInfo.getCfgIdList() != null) {
      for (CfgIdInfo info : pkgInfo.getCfgIdList()) {
        if (info.getPathName() == null || info.getTargetType() == null) {
          continue;
        }

        String pathName = info.getPathName();
        pathName.trim().replaceAll("\\\\", "/");
        if (pathName.startsWith("./")) {
          pathName = pathName.substring(1);
        } else {
          if (!pathName.startsWith("/")) {
            pathName = "/" + pathName;
          }
        }

        CfgIdentifier cfgId =
                cfgIdSrv.findByUserNameAndPathNameAndTargetType(
                        pkgInfo.getProductName(), pathName, info.getTargetType());
        if (cfgId == null) {
          cfgId = new CfgIdentifier();
          cfgId.setPathName(pathName);
          cfgId.setUserName(pkgInfo.getProductName());
          cfgId.setTargetType(info.getTargetType());
          cfgId.setCfgUuid(UUID.randomUUID().toString());

          CfgContent content = null;
          if (info.getDefaultContent() != null && !info.getDefaultContent().equals("")) {
            String hash = HashUtils.getMd5Hash(info.getDefaultContent().getBytes());
            content = cfgCntSrv.findByHash(hash);
            if (content == null) {
              content = new CfgContent();
              content.setContentType(0);
              content.setContent(info.getDefaultContent());
              content.setContentHash(hash);
              cfgCntSrv.save(content);
            }
          }
          if (content != null) {
            cfgId.setDefaultContentId(content.getId());
          }
          cfgIds.add(cfgId);
        }
      }
    }
    return cfgIds;
  }

  private List<ProductProperty> composeProperties(InstallationPackageInfo pkgInfo) {
    ArrayList<ProductProperty> propertyList = new ArrayList<ProductProperty>();
    if (pkgInfo.getPropertyList() != null) {
      for (PropertyItem item : pkgInfo.getPropertyList()) {
        ProductProperty property = new ProductProperty();
        property.setProductName(pkgInfo.getProductName());
        property.setPropKey(item.getKey());
        property.setValueType(item.getValueType());
        property.setPropValue(item.getValue());

        propertyList.add(property);
      }
    }
    return propertyList;
  }

  private Product composeProduct(InstallationPackageInfo pkgInfo) {
    Product product = new Product();
    product.setUuid(UUID.randomUUID().toString());
    product.setName(pkgInfo.getProductName());
    ProductCategory cate = Enums.getIfPresent(ProductCategory.class, pkgInfo.getCategory().toLowerCase()).or(ProductCategory.software_product);
    product.setCategory(cate.toValue());
    if (pkgInfo.getSubClass() != null) {
      product.setSubclass(pkgInfo.getSubClass());
    }
    if (pkgInfo.getVendor() != null) {
      product.setVendor(pkgInfo.getVendor());
    }
    if (pkgInfo.getDescription() != null) {
      product.setDescription(pkgInfo.getDescription());
    }
    return product;
  }
}
