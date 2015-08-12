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
import io.wcm.wcm.commons.contenttype.ContentType;
import io.wcm.wcm.commons.contenttype.FileExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.CharEncoding;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.DamConstants;

/**
 * Implements a simple REST interface that allows resolving DAM asset paths to URLs.
 * For image assets resolving to specific dimensions is supported.
 */
@SlingServlet(metatype = true,
resourceTypes = DamConstants.NT_DAM_ASSET,
extensions = FileExtension.JSON,
label = "wcm.io DAM Asset Service",
description = "A RESTful service for resolving URLs to DAM assets and renditions.")
public class AssetServiceServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 1L;

  @Property(label = "Selector", description = "Selector for attaching REST service to DAM asset paths.",
      value = AssetServiceServlet.SELECTOR_PROPERTY_DEFAULT)
  static final String SELECTOR_PROPERTY = "sling.servlet.selectors";
  static final String SELECTOR_PROPERTY_DEFAULT = "wcm-io-asset-service";

  @Property(label = "DAM paths", description = "List of DAM paths for which the asset service should be active. "
      + "If not set, the service is active for all paths.",
      cardinality = Integer.MAX_VALUE)
  static final String DAM_PATHS_PROPERTY = "damPaths";

  private static final Logger log = LoggerFactory.getLogger(AssetServiceServlet.class);

  private AssetRequestProcessor processor;

  @Reference
  private DamPathHandler damPathHandler;

  @Activate
  protected void activate(Map<String, Object> config) {
    damPathHandler.setDamPaths(PropertiesUtil.toStringArray(config.get(DAM_PATHS_PROPERTY)));
    processor = new AssetRequestProcessor();
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
    String assetPath = request.getResource().getPath();

    // get media handler
    MediaHandler mediaHandler = request.getResource().adaptTo(MediaHandler.class);
    if (mediaHandler == null) {
      log.debug("Unable to get media handler for {}", assetPath);
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // build list of asset service requests with optional input parameters
    List<AssetRequest> requests = processor.getAssetRequests(assetPath, request);

    // resolve asset service requests
    List<Media> mediaList = processor.resolveMedia(requests, mediaHandler);
    if (mediaList.size() == 0) {
      log.debug("No matching assets/renditions found for {}; requests: {}", assetPath, requests);
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // output result json
    try {
      JSONArray resultJson = processor.toResultJson(mediaList);
      response.setContentType(ContentType.JSON);
      response.setCharacterEncoding(CharEncoding.UTF_8);
      response.getWriter().write(resultJson.toString());
    }
    catch (JSONException ex) {
      throw new ServletException("Unable to generate JSON.", ex);
    }
  }

}
