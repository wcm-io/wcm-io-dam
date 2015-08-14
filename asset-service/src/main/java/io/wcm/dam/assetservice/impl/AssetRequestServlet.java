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

import io.wcm.handler.media.Media;
import io.wcm.handler.media.MediaHandler;
import io.wcm.sling.commons.request.RequestParam;
import io.wcm.wcm.commons.contenttype.ContentType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a simple REST interface that allows resolving DAM asset paths to URLs.
 * For image assets resolving to specific dimensions is supported.
 */
class AssetRequestServlet extends SlingSafeMethodsServlet {
  private static final long serialVersionUID = 1L;

  static final String RP_MEDIAFORMAT = "mediaFormat";
  static final String RP_WIDTH = "width";
  static final String RP_HEIGHT = "height";

  private final DamPathHandler damPathHandler;

  private static final Logger log = LoggerFactory.getLogger(AssetRequestServlet.class);

  public AssetRequestServlet(DamPathHandler damPathHandler) {
    this.damPathHandler = damPathHandler;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
    String assetPath = request.getResource().getPath();

    // check if asset path is valid
    if (!damPathHandler.isAllowedAssetPath(assetPath)) {
      log.debug("Asset path not allowed {}", assetPath);
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // get media handler
    MediaHandler mediaHandler = request.getResource().adaptTo(MediaHandler.class);
    if (mediaHandler == null) {
      log.debug("Unable to get media handler for {}", assetPath);
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // build list of asset service requests with optional input parameters
    List<AssetRequest> requests = getAssetRequests(assetPath, request);

    // resolve asset service requests
    List<Media> mediaList = resolveMedia(requests, mediaHandler);
    if (mediaList.size() == 0) {
      log.debug("No matching assets/renditions found for {}; requests: {}", assetPath, requests);
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // output result json
    try {
      JSONArray resultJson = toResultJson(mediaList);
      response.setContentType(ContentType.JSON);
      response.setCharacterEncoding(CharEncoding.UTF_8);
      response.getWriter().write(resultJson.toString());
    }
    catch (JSONException ex) {
      throw new ServletException("Unable to generate JSON.", ex);
    }
  }

  private List<AssetRequest> getAssetRequests(String assetPath, SlingHttpServletRequest request) {
    String[] mediaFormats = ObjectUtils.defaultIfNull(RequestParam.getMultiple(request, RP_MEDIAFORMAT), new String[0]);
    String[] widthStrings = ObjectUtils.defaultIfNull(RequestParam.getMultiple(request, RP_WIDTH), new String[0]);
    String[] heightStrings = ObjectUtils.defaultIfNull(RequestParam.getMultiple(request, RP_HEIGHT), new String[0]);
    int maxParamIndex = NumberUtils.max(mediaFormats.length, widthStrings.length, heightStrings.length);

    List<AssetRequest> requests = new ArrayList<>();
    if (maxParamIndex == 0) {
      requests.add(new AssetRequest(assetPath, null, 0, 0));
    }
    else {
      for (int i = 0; i < maxParamIndex; i++) {
        String mediaFormat = mediaFormats.length > i ? mediaFormats[i] : null;
        long width = widthStrings.length > i ? NumberUtils.toLong(widthStrings[i]) : 0;
        long height = heightStrings.length > i ? NumberUtils.toLong(heightStrings[i]) : 0;
        requests.add(new AssetRequest(assetPath, mediaFormat, width, height));
      }
    }

    return requests;
  }

  private List<Media> resolveMedia(List<AssetRequest> requests, MediaHandler mediaHandler) {
    List<Media> result = new ArrayList<>();
    for (AssetRequest request : requests) {
      Media media = request.resolve(mediaHandler);
      if (media.isValid()) {
        result.add(media);
      }
    }
    return result;
  }

  private JSONArray toResultJson(List<Media> mediaList) throws JSONException {
    JSONArray array = new JSONArray();
    for (Media media : mediaList) {
      JSONObject mediaObject = new JSONObject();
      mediaObject.put("assetPath", media.getAsset().getPath());
      mediaObject.put("url", media.getUrl());
      if (media.getRendition().getWidth() > 0 && media.getRendition().getHeight() > 0) {
        mediaObject.put("width", media.getRendition().getWidth());
        mediaObject.put("height", media.getRendition().getHeight());
      }
      if (media.getRendition().getFileSize() > 0) {
        mediaObject.put("fileSize", media.getRendition().getFileSize());
      }
      if (StringUtils.isNotEmpty(media.getRendition().getFileExtension())) {
        mediaObject.put("fileExtension", media.getRendition().getFileExtension());
      }
      array.put(mediaObject);
    }
    return array;
  }

}
