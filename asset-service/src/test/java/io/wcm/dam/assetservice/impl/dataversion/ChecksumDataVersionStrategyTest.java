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
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.wcm.commons.contenttype.ContentType;

import java.util.Calendar;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamEvent;

public class ChecksumDataVersionStrategyTest {

  @Rule
  public AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private static final String VALID_PATH_1 = "/content/dam/path1";
  private static final String VALID_PATH_2 = "/content/dam/sub/path2";
  private static final String INVALID_PATH = "/content/dam/path3";

  private DamPathHandler underTest;

  private Asset asset11;
  private Asset asset12;
  private Asset asset21;

  @Before
  public void setUp() throws Exception {

    // create some sample assets with SHA-1 checksums
    asset11 = context.create().asset(VALID_PATH_1 + "/asset11", 100, 50, ContentType.JPEG);
    setLastModified(asset11, 15 * DateUtils.MILLIS_PER_DAY);
    asset12 = context.create().asset(VALID_PATH_1 + "/asset12", 100, 50, ContentType.JPEG);
    setLastModified(asset12, 16 * DateUtils.MILLIS_PER_DAY);
    asset21 = context.create().asset(VALID_PATH_2 + "/asset21", 100, 50, ContentType.JPEG);
    setLastModified(asset21, 17 * DateUtils.MILLIS_PER_DAY);

    ResourceResolverFactory resourceResolverFactory = context.getService(ResourceResolverFactory.class);
    underTest = new DamPathHandler(new String[] {
        VALID_PATH_1,
        VALID_PATH_2
    }, ChecksumDataVersionStrategy.STRATEGY, 1, resourceResolverFactory);

    // wait until first data versions are generated
    Thread.sleep(500);
  }

  private void setLastModified(Asset asset, long time) {
    Resource metadata = context.resourceResolver().getResource(asset.getPath() + "/jcr:content");
    ModifiableValueMap props = metadata.adaptTo(ModifiableValueMap.class);
    Calendar lastModified = Calendar.getInstance();
    lastModified.setTimeInMillis(time);
    props.put(JcrConstants.JCR_LASTMODIFIED, lastModified);
    try {
      context.resourceResolver().commit();
    }
    catch (PersistenceException ex) {
      throw new RuntimeException(ex);
    }
  }

  @After
  public void tearDown() throws Exception {
    underTest.shutdown();
  }

  @Test
  public void testNewDataVersionOnValidPathEvent() throws Exception {
    String dataVersion1 = underTest.getDataVersion(VALID_PATH_1);
    String dataVersion2 = underTest.getDataVersion(VALID_PATH_2);
    assertNotNull(dataVersion1);

    setLastModified(asset11, 30 * DateUtils.MILLIS_PER_DAY);
    underTest.handleDamEvent(DamEvent.metadataUpdated(asset11.getPath(), null));

    // data version is generated asynchronously
    Thread.sleep(1000);

    // data version for path 1 should be changed
    String dataVersion1a = underTest.getDataVersion(VALID_PATH_1);
    assertNotNull(dataVersion1a);
    assertNotEquals("data version 1 changed", dataVersion1, dataVersion1a);

    // data version for path 2 should not be unchanged
    String dataVersion2a = underTest.getDataVersion(VALID_PATH_2);
    assertNotNull(dataVersion2a);
    assertEquals("data version 2 unchanged", dataVersion2, dataVersion2a);

    // wait a bit more and test again
    Thread.sleep(1000);

    // data version for path 1 should not be changed
    String dataVersion1b = underTest.getDataVersion(VALID_PATH_1);
    assertNotNull(dataVersion1b);
    assertEquals("data version 1 changed", dataVersion1a, dataVersion1b);

    // data version for path 2 should not be unchanged
    String dataVersion2b = underTest.getDataVersion(VALID_PATH_2);
    assertNotNull(dataVersion2b);
    assertEquals("data version 2 unchanged", dataVersion2a, dataVersion2b);

  }

  @Test
  public void testSameDataVersionOnInvalidPathEvent() throws Exception {
    String dataVersion1 = underTest.getDataVersion(VALID_PATH_1);
    assertNotNull(dataVersion1);

    underTest.handleDamEvent(DamEvent.assetCreated(INVALID_PATH + "/asset1.jpg", null));

    // data version is generated asynchronously
    Thread.sleep(1000);

    String dataVersion2 = underTest.getDataVersion(VALID_PATH_1);
    assertNotNull(dataVersion2);
    assertEquals("data version", dataVersion1, dataVersion2);
  }

}
