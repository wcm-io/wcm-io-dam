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
import com.day.cq.dam.api.DamConstants;
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
    setSha1Checksum(asset11, "123");
    asset12 = context.create().asset(VALID_PATH_1 + "/asset12", 100, 50, ContentType.JPEG);
    setSha1Checksum(asset12, "456");
    asset21 = context.create().asset(VALID_PATH_2 + "/asset21", 100, 50, ContentType.JPEG);
    setSha1Checksum(asset21, "789");

    ResourceResolverFactory resourceResolverFactory = context.getService(ResourceResolverFactory.class);
    underTest = new DamPathHandler(new String[] {
        VALID_PATH_1,
        VALID_PATH_2
    }, ChecksumDataVersionStrategy.STRATEGY, 1, resourceResolverFactory);

    // wait until first data versions are generated
    Thread.sleep(500);
  }

  private void setSha1Checksum(Asset asset, String checksum) {
    String metadataPath = asset.getPath() + "/jcr:content/metadata";
    Resource metadata = context.resourceResolver().getResource(asset.getPath() + "/jcr:content/metadata");
    if (metadata == null) {
      metadata = context.create().resource(metadataPath);
    }
    ModifiableValueMap props = metadata.adaptTo(ModifiableValueMap.class);
    props.put(DamConstants.PN_SHA1, checksum);
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

    setSha1Checksum(asset11, "999");
    underTest.handleDamEvent(DamEvent.metadataUpdated(asset11.getPath(), null));

    // data version is generated asynchronously
    Thread.sleep(2000);

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

    underTest.handleDamEvent(DamEvent.assetCreated(INVALID_PATH + "/asset1.jpg", null));

    // data version is generated asynchronously
    Thread.sleep(2000);

    String dataVersion2 = underTest.getDataVersion(VALID_PATH_1);
    assertNotNull(dataVersion2);
    assertEquals("data version", dataVersion1, dataVersion2);
  }

}
