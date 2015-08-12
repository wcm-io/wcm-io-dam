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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Processes the REST requests incoming to the asset service, resolves the media items and produces the JSON output.
 */
class AssetRequestProcessor {

  static final String RP_MEDIAFORMAT = "mediaFormat";
  static final String RP_WIDTH = "width";
  static final String RP_HEIGHT = "height";

  public List<AssetRequest> getAssetRequests(String assetPath, SlingHttpServletRequest request) {
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

  public List<Media> resolveMedia(List<AssetRequest> requests, MediaHandler mediaHandler) {
    List<Media> result = new ArrayList<>();
    for (AssetRequest request : requests) {
      Media media = request.resolve(mediaHandler);
      if (media.isValid()) {
        result.add(media);
      }
    }
    return result;
  }

  public JSONArray toResultJson(List<Media> mediaList) throws JSONException {
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
