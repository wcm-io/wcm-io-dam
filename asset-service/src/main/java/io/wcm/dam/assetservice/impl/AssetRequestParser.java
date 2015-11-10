/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.dam.assetservice.impl;

import io.wcm.sling.commons.request.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Parses asset requests from request URL.
 */
final class AssetRequestParser {

  static final String RP_MEDIAFORMAT = "mediaFormat";
  static final String RP_WIDTH = "width";
  static final String RP_HEIGHT = "height";

  private AssetRequestParser() {
    // static methods only
  }

  /**
   * Reads asset requests from URL. If requests using the new suffix-based approach are provided those are returned.
   * Otherwise requests defined via URL parameters are parsed and returned.
   * If no parameters are given at all one single un-parameterized request for the dam asset is generated.
   * @param assetPath Asset path
   * @param request Request
   * @return List of asset requests
   */
  public static List<AssetRequest> getAssetRequests(String assetPath, SlingHttpServletRequest request) {
    List<AssetRequest> requests = getAssetRequestsFromSuffix(assetPath, request);
    if (requests.isEmpty()) {
      requests = getAssetRequestsFromUrlParams(assetPath, request);
    }
    if (requests.isEmpty()) {
      requests.add(new AssetRequest(assetPath, null, 0, 0));
    }
    return requests;
  }

  private static List<AssetRequest> getAssetRequestsFromSuffix(String assetPath, SlingHttpServletRequest request) {
    List<AssetRequest> requests = new ArrayList<>();

    String suffixWithoutExtension = StringUtils.substringBefore(request.getRequestPathInfo().getSuffix(), ".");
    String[] suffixParts = StringUtils.split(suffixWithoutExtension, "/");
    if (suffixParts != null) {
      for (String suffixPart : suffixParts) {
        Map<String, String> params = parseSuffixPart(suffixPart);
        String mediaFormat = params.get(RP_MEDIAFORMAT);
        long width = NumberUtils.toLong(params.get(RP_WIDTH));
        long height = NumberUtils.toLong(params.get(RP_HEIGHT));
        if (StringUtils.isNotEmpty(mediaFormat) || width > 0 || height > 0) {
          requests.add(new AssetRequest(assetPath, mediaFormat, width, height));
        }
      }
    }

    return requests;
  }

  private static Map<String, String> parseSuffixPart(String suffixPart) {
    Map<String, String> params = new HashMap<>();
    String[] paramPairParts = StringUtils.split(suffixPart, ",");
    if (paramPairParts != null) {
      for (String paramPairPart : paramPairParts) {
        String[] paramParts = StringUtils.split(paramPairPart, "=");
        if (paramParts != null && paramParts.length == 2) {
          params.put(paramParts[0], paramParts[1]);
        }
      }
    }
    return params;
  }

  private static List<AssetRequest> getAssetRequestsFromUrlParams(String assetPath, SlingHttpServletRequest request) {
    String[] mediaFormats = ObjectUtils.defaultIfNull(RequestParam.getMultiple(request, RP_MEDIAFORMAT), new String[0]);
    String[] widthStrings = ObjectUtils.defaultIfNull(RequestParam.getMultiple(request, RP_WIDTH), new String[0]);
    String[] heightStrings = ObjectUtils.defaultIfNull(RequestParam.getMultiple(request, RP_HEIGHT), new String[0]);
    int maxParamIndex = NumberUtils.max(mediaFormats.length, widthStrings.length, heightStrings.length);

    List<AssetRequest> requests = new ArrayList<>();
    for (int i = 0; i < maxParamIndex; i++) {
      String mediaFormat = mediaFormats.length > i ? mediaFormats[i] : null;
      long width = widthStrings.length > i ? NumberUtils.toLong(widthStrings[i]) : 0;
      long height = heightStrings.length > i ? NumberUtils.toLong(heightStrings[i]) : 0;
      requests.add(new AssetRequest(assetPath, mediaFormat, width, height));
    }
    return requests;
  }

}
