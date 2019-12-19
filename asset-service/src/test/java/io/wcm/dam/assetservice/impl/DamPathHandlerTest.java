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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.wcm.dam.assetservice.impl.dataversion.TimestampDataVersionStrategy;

class DamPathHandlerTest {

  private static final String VALID_PATH_1 = "/content/dam/path1";
  private static final String VALID_PATH_2 = "/content/dam/sub/path2";
  private static final String INVALID_PATH = "/content/dam/path3";

  private DamPathHandler underTest;

  @AfterEach
  void tearDown() {
    underTest.shutdown();
  }

  @Test
  void testIsAllowedAssetPath() throws Exception {
    underTest = new DamPathHandler(new String[] {
        VALID_PATH_1,
        VALID_PATH_2
    }, TimestampDataVersionStrategy.STRATEGY, 0, null);

    // valid asset paths
    assertTrue(underTest.isAllowedAssetPath(VALID_PATH_1 + "/asset1.jpg"));
    assertTrue(underTest.isAllowedAssetPath(VALID_PATH_2 + "/sub2/asset2.jpg"));

    // invalid asset paths
    assertFalse(underTest.isAllowedAssetPath(INVALID_PATH + "/asset1.jpg"));
    assertFalse(underTest.isAllowedAssetPath(VALID_PATH_1));
    assertFalse(underTest.isAllowedAssetPath(null));
  }

  @Test
  void testIsAllowedDataVersionPath() throws Exception {
    underTest = new DamPathHandler(new String[] {
        VALID_PATH_1,
        VALID_PATH_2
    }, TimestampDataVersionStrategy.STRATEGY, 0, null);

    // valid root paths
    assertTrue(underTest.isAllowedDataVersionPath(VALID_PATH_1));
    assertTrue(underTest.isAllowedDataVersionPath(VALID_PATH_2));

    // invalid root path
    assertFalse(underTest.isAllowedDataVersionPath(INVALID_PATH));

    // asset paths are invalid as well
    assertFalse(underTest.isAllowedDataVersionPath(VALID_PATH_1 + "/asset1.jpg"));
    assertFalse(underTest.isAllowedDataVersionPath(VALID_PATH_2 + "/sub2/asset2.jpg"));
    assertFalse(underTest.isAllowedDataVersionPath(null));
  }

  @Test
  void testWithNullPaths() {
    underTest = new DamPathHandler(null, TimestampDataVersionStrategy.STRATEGY, 0, null);
    assertTrue(underTest.isAllowedAssetPath(VALID_PATH_1 + "/asset1.jpg"));
    assertTrue(underTest.isAllowedAssetPath(INVALID_PATH + "/asset1.jpg"));
  }

  @Test
  void testWithEmptyArray() {
    underTest = new DamPathHandler(new String[0], TimestampDataVersionStrategy.STRATEGY, 0, null);
    assertTrue(underTest.isAllowedAssetPath(VALID_PATH_1 + "/asset1.jpg"));
    assertTrue(underTest.isAllowedAssetPath(INVALID_PATH + "/asset1.jpg"));
  }

  @Test
  void testWithEmptyPaths() {
    underTest = new DamPathHandler(new String[] {
        ""
    }, TimestampDataVersionStrategy.STRATEGY, 0, null);
    assertTrue(underTest.isAllowedAssetPath(VALID_PATH_1 + "/asset1.jpg"));
    assertTrue(underTest.isAllowedAssetPath(INVALID_PATH + "/asset1.jpg"));
  }

}
