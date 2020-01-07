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

import static io.wcm.dam.assetservice.impl.AssetRequestParser.RP_HEIGHT;
import static io.wcm.dam.assetservice.impl.AssetRequestParser.RP_MEDIAFORMAT;
import static io.wcm.dam.assetservice.impl.AssetRequestParser.RP_WIDTH;
import static org.apache.sling.api.resource.ResourceResolver.PROPERTY_RESOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.google.common.collect.ImmutableMap;

import io.wcm.dam.assetservice.impl.testcontext.AppAemContext;
import io.wcm.sling.commons.resource.ImmutableValueMap;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * Test {@link AssetRequestServlet} using the old REST API with parameters in URL parameters.
 */
@ExtendWith(AemContextExtension.class)
class AssetRequestServletUrlParamsTest {

  private static final String DAM_PATH = "/content/dam/sample";

  private static final String DOWNLOAD_ASSET_PATH = DAM_PATH + "/downloads/sample.pdf";
  private static final String IMAGE_ASSET_PATH = DAM_PATH + "/images/image.jpg";

  private static final byte[] DOWNLOAD_BYTES = new byte[] {
    0x01, 0x02, 0x03, 0x04, 0x05
  };
  private static final byte[] IMAGE_BYTES = new byte[] {
    0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08
  };

  private final AemContext context = AppAemContext.newAemContext();

  private AssetService assetService;
  private AssetRequestServlet underTest;

  @BeforeEach
  void setUp() {
    context.load().json("/dam-sample-content.json", DAM_PATH);
    context.load().binaryFile(new ByteArrayInputStream(DOWNLOAD_BYTES), DOWNLOAD_ASSET_PATH + "/jcr:content/renditions/original");
    context.load().binaryFile(new ByteArrayInputStream(IMAGE_BYTES), IMAGE_ASSET_PATH + "/jcr:content/renditions/original");

    assetService = context.registerInjectActivateService(new AssetService());
    underTest = assetService.getAssetRequestServlet();
  }

  @AfterEach
  void tearDown() {
    MockOsgi.deactivate(assetService, context.bundleContext(), ImmutableMap.<String, Object>of());
  }

  @Test
  void testInvalidResource() throws Exception {
    context.currentResource(context.create().resource(DAM_PATH + "/invalid",
        PROPERTY_RESOURCE_TYPE, "/dummy/resourcetype"));
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());
  }

  @Test
  void testDownload() throws Exception {
    context.currentResource(context.resourceResolver().getResource(DOWNLOAD_ASSET_PATH));
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

    String expected = "["
        + "{'assetPath':'" + DAM_PATH + "/downloads/sample.pdf',"
        + "'url':'" + DAM_PATH + "/downloads/sample.pdf/_jcr_content/renditions/original./sample.pdf',"
        + "'fileSize':" + DOWNLOAD_BYTES.length + ","
        + "'fileExtension':'pdf',"
        + "'mimeType':'application/octet-stream'}"
        + "]";
    JSONAssert.assertEquals(expected, context.response().getOutputAsString(), true);
  }

  @Test
  void testImage() throws Exception {
    context.currentResource(context.resourceResolver().getResource(IMAGE_ASSET_PATH));
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

    String expected = "["
        + "{'assetPath':'" + DAM_PATH + "/images/image.jpg',"
        + "'url':'" + DAM_PATH + "/images/image.jpg/_jcr_content/renditions/original./image.jpg',"
        + "'width':1920,"
        + "'height':540,"
        + "'fileSize':" + IMAGE_BYTES.length + ","
        + "'fileExtension':'jpg',"
        + "'mimeType':'application/octet-stream'}"
        + "]";
    JSONAssert.assertEquals(expected, context.response().getOutputAsString(), true);
  }

  @Test
  void testImage_ValidMediaFormat() throws Exception {
    context.currentResource(context.resourceResolver().getResource(IMAGE_ASSET_PATH));
    context.request().setParameterMap(ImmutableValueMap.builder()
        .put(RP_MEDIAFORMAT, "format_32_9")
        .build());
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

    String expected = "["
        + "{'assetPath':'" + DAM_PATH + "/images/image.jpg',"
        + "'url':'" + DAM_PATH + "/images/image.jpg/_jcr_content/renditions/original./image.jpg',"
        + "'width':1920,"
        + "'height':540,"
        + "'fileSize':" + IMAGE_BYTES.length + ","
        + "'fileExtension':'jpg',"
        + "'mimeType':'application/octet-stream'}"
        + "]";
    JSONAssert.assertEquals(expected, context.response().getOutputAsString(), true);
  }

  @Test
  void testImage_InvalidMediaFormat() throws Exception {
    context.currentResource(context.resourceResolver().getResource(IMAGE_ASSET_PATH));
    context.request().setParameterMap(ImmutableValueMap.builder()
        .put(RP_MEDIAFORMAT, "format_4_3")
        .build());
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());
  }

  @Test
  void testImage_ValidSize() throws Exception {
    context.currentResource(context.resourceResolver().getResource(IMAGE_ASSET_PATH));
    context.request().setParameterMap(ImmutableValueMap.builder()
        .put(RP_WIDTH, 960)
        .put(RP_HEIGHT, 270)
        .build());
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

    String expected = "["
        + "{'assetPath':'" + DAM_PATH + "/images/image.jpg',"
        + "'url':'" + DAM_PATH + "/images/image.jpg/_jcr_content/renditions/original.image_file.960.270.file/image.jpg',"
        + "'width':960,"
        + "'height':270,"
        + "'fileExtension':'jpg',"
        + "'mimeType':'application/octet-stream'}"
        + "]";
    JSONAssert.assertEquals(expected, context.response().getOutputAsString(), true);
  }

  @Test
  void testImage_InvalidSize() throws Exception {
    context.currentResource(context.resourceResolver().getResource(IMAGE_ASSET_PATH));
    context.request().setParameterMap(ImmutableValueMap.builder()
        .put(RP_WIDTH, 960)
        .put(RP_HEIGHT, 960)
        .build());
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());
  }

  @Test
  void testImage_MultipleSizes() throws Exception {
    context.currentResource(context.resourceResolver().getResource(IMAGE_ASSET_PATH));
    context.request().setParameterMap(ImmutableValueMap.builder()
        .put(RP_WIDTH, new String[] {
            "960", "640", "10", "5"
        })
        .put(RP_HEIGHT, new String[] {
            "270", "180", "10"
        })
        .build());
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

    String expected = "["
        + "{'assetPath':'" + DAM_PATH + "/images/image.jpg',"
        + "'url':'" + DAM_PATH + "/images/image.jpg/_jcr_content/renditions/original.image_file.960.270.file/image.jpg',"
        + "'width':960,"
        + "'height':270,"
        + "'fileExtension':'jpg',"
        + "'mimeType':'application/octet-stream'},"
        + "{'assetPath':'" + DAM_PATH + "/images/image.jpg',"
        + "'url':'" + DAM_PATH + "/images/image.jpg/_jcr_content/renditions/original.image_file.640.180.file/image.jpg',"
        + "'width':640,"
        + "'height':180,"
        + "'fileExtension':'jpg',"
        + "'mimeType':'application/octet-stream'}"
        + "]";
    JSONAssert.assertEquals(expected, context.response().getOutputAsString(), true);
  }

}
