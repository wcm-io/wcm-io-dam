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

import static io.wcm.dam.assetservice.impl.AssetRequestProcessor.RP_HEIGHT;
import static io.wcm.dam.assetservice.impl.AssetRequestProcessor.RP_MEDIAFORMAT;
import static io.wcm.dam.assetservice.impl.AssetRequestProcessor.RP_WIDTH;
import static org.junit.Assert.assertEquals;
import io.wcm.dam.assetservice.impl.testcontext.AppAemContext;
import io.wcm.sling.commons.resource.ImmutableValueMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import java.io.ByteArrayInputStream;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class AssetServiceServletTest {

  private static final String DAM_PATH = "/content/dam/sample";

  private static final String DOWNLOAD_ASSET_PATH = DAM_PATH + "/downloads/sample.pdf";
  private static final String IMAGE_ASSET_PATH = DAM_PATH + "/images/image.jpg";

  private static final byte[] DOWNLOAD_BYTES = new byte[] {
    0x01, 0x02, 0x03, 0x04, 0x05
  };
  private static final byte[] IMAGE_BYTES = new byte[] {
    0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08
  };

  @Rule
  public AemContext context = AppAemContext.newAemContext();

  private AssetServiceServlet underTest;

  @Before
  public void setUp() {
    context.load().json("/dam-sample-content.json", DAM_PATH);
    context.load().binaryFile(new ByteArrayInputStream(DOWNLOAD_BYTES), DOWNLOAD_ASSET_PATH + "/jcr:content/renditions/original");
    context.load().binaryFile(new ByteArrayInputStream(IMAGE_BYTES), IMAGE_ASSET_PATH + "/jcr:content/renditions/original");

    context.registerInjectActivateService(new DamPathHandler());
    underTest = context.registerInjectActivateService(new AssetServiceServlet());
  }

  @Test
  public void testInvalidResource() throws Exception {
    context.currentResource(context.create().resource(DAM_PATH + "/invalid"));
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());
  }

  @Test
  public void testDownload() throws Exception {
    context.currentResource(context.resourceResolver().getResource(DOWNLOAD_ASSET_PATH));
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

    String expected = "["
        + "{'assetPath':'" + DAM_PATH + "/downloads/sample.pdf',"
        + "'url':'" + DAM_PATH + "/downloads/sample.pdf/_jcr_content/renditions/original./sample.pdf',"
        + "'fileSize':" + DOWNLOAD_BYTES.length + ","
        + "'fileExtension':'pdf'}"
        + "]";
    JSONAssert.assertEquals(expected, context.response().getOutputAsString(), true);
  }

  @Test
  public void testImage() throws Exception {
    context.currentResource(context.resourceResolver().getResource(IMAGE_ASSET_PATH));
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

    String expected = "["
        + "{'assetPath':'" + DAM_PATH + "/images/image.jpg',"
        + "'url':'" + DAM_PATH + "/images/image.jpg/_jcr_content/renditions/original./image.jpg',"
        + "'width':1920,"
        + "'height':540,"
        + "'fileSize':" + IMAGE_BYTES.length + ","
        + "'fileExtension':'jpg'}"
        + "]";
    JSONAssert.assertEquals(expected, context.response().getOutputAsString(), true);
  }

  @Test
  public void testImage_ValidMediaFormat() throws Exception {
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
        + "'fileExtension':'jpg'}"
        + "]";
    JSONAssert.assertEquals(expected, context.response().getOutputAsString(), true);
  }

  @Test
  public void testImage_InvalidMediaFormat() throws Exception {
    context.currentResource(context.resourceResolver().getResource(IMAGE_ASSET_PATH));
    context.request().setParameterMap(ImmutableValueMap.builder()
        .put(RP_MEDIAFORMAT, "format_4_3")
        .build());
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());
  }

  @Test
  public void testImage_ValidSize() throws Exception {
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
        + "'fileExtension':'jpg'}"
        + "]";
    JSONAssert.assertEquals(expected, context.response().getOutputAsString(), true);
  }

  @Test
  public void testImage_InvalidSize() throws Exception {
    context.currentResource(context.resourceResolver().getResource(IMAGE_ASSET_PATH));
    context.request().setParameterMap(ImmutableValueMap.builder()
        .put(RP_WIDTH, 960)
        .put(RP_HEIGHT, 960)
        .build());
    underTest.doGet(context.request(), context.response());

    assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());
  }

  @Test
  public void testImage_MultipleSizes() throws Exception {
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
        + "'fileExtension':'jpg'},"
        + "{'assetPath':'" + DAM_PATH + "/images/image.jpg',"
        + "'url':'" + DAM_PATH + "/images/image.jpg/_jcr_content/renditions/original.image_file.640.180.file/image.jpg',"
        + "'width':640,"
        + "'height':180,"
        + "'fileExtension':'jpg'}"
        + "]";
    JSONAssert.assertEquals(expected, context.response().getOutputAsString(), true);
  }

}
