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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.day.cq.dam.api.DamEvent;

public class DamPathHandlerTest {

  private static final String VALID_PATH_1 = "/content/dam/path1";
  private static final String VALID_PATH_2 = "/content/dam/sub/path2";
  private static final String INVALID_PATH = "/content/dam/path3";

  private DamPathHandler underTest;

  @Before
  public void setUp() {
    underTest = new DamPathHandler(new String[] {
        VALID_PATH_1,
        VALID_PATH_2
    });
  }

  @Test
  public void testIsAllowedAssetPath() throws Exception {
    // valid asset paths
    assertTrue(underTest.isAllowedAssetPath(VALID_PATH_1 + "/asset1.jpg"));
    assertTrue(underTest.isAllowedAssetPath(VALID_PATH_2 + "/sub2/asset2.jpg"));

    // invalid asset paths
    assertFalse(underTest.isAllowedAssetPath(INVALID_PATH + "/asset1.jpg"));
    assertFalse(underTest.isAllowedAssetPath(VALID_PATH_1));
  }

  @Test
  public void testIsAllowedDataVersionPath() throws Exception {
    // valid root paths
    assertTrue(underTest.isAllowedDataVersionPath(VALID_PATH_1));
    assertTrue(underTest.isAllowedDataVersionPath(VALID_PATH_2));

    // invalid root path
    assertFalse(underTest.isAllowedDataVersionPath(INVALID_PATH));

    // asset paths are invalid as well
    assertFalse(underTest.isAllowedDataVersionPath(VALID_PATH_1 + "/asset1.jpg"));
    assertFalse(underTest.isAllowedDataVersionPath(VALID_PATH_2 + "/sub2/asset2.jpg"));
  }

  @Test
  public void testNewDataVersionOnValidPathEvent() throws Exception {
    String dataVersion1 = underTest.getDataVersion();
    assertNotNull(dataVersion1);

    // make sure generate data version timestamp does not clash
    Thread.sleep(5);

    underTest.handleDamEvent(DamEvent.assetCreated(VALID_PATH_1 + "/asset1.jpg", null));

    String dataVersion2 = underTest.getDataVersion();
    assertNotNull(dataVersion2);
    assertNotEquals("data version", dataVersion1, dataVersion2);
  }

  @Test
  public void testSameDataVersionOnInvalidPathEvent() throws Exception {
    String dataVersion1 = underTest.getDataVersion();
    assertNotNull(dataVersion1);

    // make sure generate data version timestamp does not clash
    Thread.sleep(5);

    underTest.handleDamEvent(DamEvent.assetCreated(INVALID_PATH + "/asset1.jpg", null));

    String dataVersion2 = underTest.getDataVersion();
    assertNotNull(dataVersion2);
    assertEquals("data version", dataVersion1, dataVersion2);
  }

  @Test
  public void testWithNullPaths() {
    DamPathHandler handler = new DamPathHandler(null);
    assertTrue(handler.isAllowedAssetPath(VALID_PATH_1 + "/asset1.jpg"));
    assertTrue(handler.isAllowedAssetPath(INVALID_PATH + "/asset1.jpg"));
  }

  @Test
  public void testWithEmptyArray() {
    DamPathHandler handler = new DamPathHandler(new String[0]);
    assertTrue(handler.isAllowedAssetPath(VALID_PATH_1 + "/asset1.jpg"));
    assertTrue(handler.isAllowedAssetPath(INVALID_PATH + "/asset1.jpg"));
  }

  @Test
  public void testWithEmptyPaths() {
    DamPathHandler handler = new DamPathHandler(new String[] {
      ""
    });
    assertTrue(handler.isAllowedAssetPath(VALID_PATH_1 + "/asset1.jpg"));
    assertTrue(handler.isAllowedAssetPath(INVALID_PATH + "/asset1.jpg"));
  }

}
