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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.dam.api.DamEvent;
import com.google.common.collect.ImmutableMap;

import io.wcm.dam.assetservice.impl.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class DataVersionServletTest {

  private static final String VALID_DAM_PATH = "/content/dam/sample";
  private static final String INVALID_DAM_PATH = "/content/dam/invalid";

  private final AemContext context = AppAemContext.newAemContext();

  private AssetService assetService;
  private DataVersionServlet underTest;

  @BeforeEach
  void setUp() {
    context.load().json("/dam-sample-content.json", VALID_DAM_PATH);
    assetService = context.registerInjectActivateService(new AssetService(),
        "damPaths", new String[] { VALID_DAM_PATH });
    underTest = assetService.getDataVersionServlet();
  }

  @AfterEach
  void tearDown() {
    MockOsgi.deactivate(assetService, context.bundleContext(), ImmutableMap.<String, Object>of());
  }

  @Test
  void testInvalidRootPath() throws Exception {
    context.currentResource(context.create().resource(INVALID_DAM_PATH));
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());
  }

  @Test
  void testValidRootPath() throws Exception {
    context.currentResource(context.resourceResolver().getResource(VALID_DAM_PATH));
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

    assertNotNull(getDataVersion(context.response()));
  }

  @Test
  void testDataVersionChange() throws Exception {
    context.currentResource(context.resourceResolver().getResource(VALID_DAM_PATH));

    underTest.doGet(context.request(), context.response());
    assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    String dataVersion1 = getDataVersion(context.response());

    // make sure generate data version timestamp does not clash
    Thread.sleep(5);

    // simulate asset creation event
    assetService.handleEvent(DamEvent.assetCreated(VALID_DAM_PATH + "/images/image.jpg", null).toEvent());

    // do 2nd request
    MockSlingHttpServletRequest request2 = new MockSlingHttpServletRequest(context.bundleContext());
    request2.setResource(context.currentResource());
    MockSlingHttpServletResponse response2 = new MockSlingHttpServletResponse();

    underTest.doGet(request2, response2);
    assertEquals(HttpServletResponse.SC_OK, response2.getStatus());
    String dataVersion2 = getDataVersion(response2);

    assertNotEquals("data version", dataVersion1, dataVersion2);
  }

  private String getDataVersion(MockSlingHttpServletResponse response) throws JSONException {
    JSONObject json = new JSONObject(response.getOutputAsString());

    // asset non-caching header is set (pick only one header)
    assertEquals("no-cache", response.getHeader("Dispatcher"));

    return json.getString("dataVersion");
  }

}
