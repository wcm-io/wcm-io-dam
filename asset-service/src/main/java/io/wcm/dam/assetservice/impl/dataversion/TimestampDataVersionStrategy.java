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

import java.util.Calendar;

import org.apache.jackrabbit.util.ISO8601;

import com.day.cq.dam.api.DamEvent;

/**
 * Simple strategy to generate data versions - on each DAM event a new timestamp is generated and
 * returned as data version. Please be aware that this does not produce stable data versions
 * across a cluster of AEM instances. It should only be used if there is only one AEM instance
 * generating the data version, or some sort of long-stable stickyness is applied on the load balancer.
 */
public class TimestampDataVersionStrategy extends DataVersionStrategy {

  /**
   * Data version strategy id for configuration persistence.
   */
  public static final String STRATEGY = "timestamp";

  private volatile String dataVersion;

  /**
   * Generate new data version on first instantiation.
   * @param damPath DAM root path
   */
  public TimestampDataVersionStrategy(String damPath) {
    super(damPath);
    generateNewDataVersion();
  }

  @Override
  public void handleDamEvent(DamEvent damEvent) {
    // generate a new data version on any DAM event affecting any of the configured paths
    generateNewDataVersion();
  }

  @Override
  public String getDataVersion() {
    return dataVersion;
  }

  /**
   * Generates a new data version based on current timestamp.
   */
  private void generateNewDataVersion() {
    // use timestamp as data version. clashing of versions if two are generated at exactly the same time point
    // is not the problem, because the data version can then be the same.
    dataVersion = ISO8601.format(Calendar.getInstance());
    log.debug("{} - Generated new data version: {}", damPath, dataVersion);
  }

}
