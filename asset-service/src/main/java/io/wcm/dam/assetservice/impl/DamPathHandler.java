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

import java.util.Calendar;
import java.util.regex.Pattern;

import org.apache.jackrabbit.util.ISO8601;

import com.day.cq.dam.api.DamEvent;

/**
 * Handles list of configured DAM paths and listens to DAM events on this paths to generate
 * a new data version on each DAM content change relevant for the DAM asset services consumers.
 */
class DamPathHandler {

  private volatile Pattern damPathsPattern;
  private volatile String dataVersion;

  public DamPathHandler(String[] damPaths) {
    damPathsPattern = buildDamPathsPattern(damPaths);
    generateNewDataVersion();
  }

  /**
   * Set DAM paths that should be handled. Only called once by {@link AssetRequestServlet}.
   * @param damPaths DAM folder paths or empty/null if all should be handled.
   */
  private static Pattern buildDamPathsPattern(String[] damPaths) {
    if (damPaths == null || damPaths.length == 0) {
      return null;
    }
    else {
      StringBuilder pattern = new StringBuilder();
      pattern.append("^(");
      for (int i = 0; i < damPaths.length; i++) {
        if (i > 0) {
          pattern.append("|");
        }
        pattern.append(Pattern.quote(damPaths[i]));
        pattern.append("/.*");
      }
      pattern.append(")$");
      return Pattern.compile(pattern.toString());
    }
  }

  /**
   * Checks if the given DAM asset is allowed to process.
   * @param assetPath Asset path
   * @return true if processing is allowed.
   */
  public boolean isAllowedAsset(String assetPath) {
    if (damPathsPattern == null) {
      return true;
    }
    else {
      return damPathsPattern.matcher(assetPath).matches();
    }
  }

  /**
   * Get current data version for all allowed assets.
   * @return Data version
   */
  public String getDataVersion() {
    return dataVersion;
  }

  public void handleDamEvent(DamEvent event) {
    if (isAllowedAsset(event.getAssetPath())) {
      // generate a new data version on any DAM event affecting any of the configured paths
      generateNewDataVersion();
    }
  }

  /**
   * Generates a new data version based on current timestamp.
   */
  private void generateNewDataVersion() {
    // use timestamp as data version. clashing of versions if two are generated at exaclty the same time point
    // is not the problem, because the data version can than be the same.
    dataVersion = ISO8601.format(Calendar.getInstance());
  }

}
