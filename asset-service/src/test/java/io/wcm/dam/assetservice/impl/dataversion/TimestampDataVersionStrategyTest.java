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
package io.wcm.dam.assetservice.impl.dataversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import io.wcm.dam.assetservice.impl.DamPathHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.day.cq.dam.api.DamEvent;

public class TimestampDataVersionStrategyTest {

  private static final String VALID_PATH_1 = "/content/dam/path1";
  private static final String VALID_PATH_2 = "/content/dam/sub/path2";
  private static final String INVALID_PATH = "/content/dam/path3";

  private DamPathHandler underTest;

  @Before
  public void setUp() {
    underTest = new DamPathHandler(new String[] {
        VALID_PATH_1,
        VALID_PATH_2
    }, TimestampDataVersionStrategy.STRATEGY, 0, null);
  }

  @After
  public void tearDown() {
    underTest.shutdown();
  }

  @Test
  public void testNewDataVersionOnValidPathEvent() throws Exception {
    String dataVersion1 = underTest.getDataVersion(VALID_PATH_1);
    String dataVersion2 = underTest.getDataVersion(VALID_PATH_2);
    assertNotNull(dataVersion1);

    // make sure generate data version timestamp does not clash
    Thread.sleep(5);

    underTest.handleDamEvent(DamEvent.assetCreated(VALID_PATH_1 + "/asset1.jpg", null));

    // data version for path 1 should be changed
    String dataVersion1new = underTest.getDataVersion(VALID_PATH_1);
    assertNotNull(dataVersion1new);
    assertNotEquals("data version 1 changed", dataVersion1, dataVersion1new);

    // data version for path 2 should be unchanged
    String dataVersion2new = underTest.getDataVersion(VALID_PATH_2);
    assertNotNull(dataVersion2new);
    assertEquals("data version 2 unchanged", dataVersion2, dataVersion2new);
  }

  @Test
  public void testSameDataVersionOnInvalidPathEvent() throws Exception {
    String dataVersion1 = underTest.getDataVersion(VALID_PATH_1);
    assertNotNull(dataVersion1);

    // make sure generate data version timestamp does not clash
    Thread.sleep(5);

    underTest.handleDamEvent(DamEvent.assetCreated(INVALID_PATH + "/asset1.jpg", null));

    String dataVersion2 = underTest.getDataVersion(VALID_PATH_1);
    assertNotNull(dataVersion2);
    assertEquals("data version", dataVersion1, dataVersion2);
  }

}
