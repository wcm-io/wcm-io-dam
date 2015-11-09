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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.ISO8601;

import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.DamEvent;
import com.google.common.collect.ImmutableSet;

/**
 * Handles list of configured DAM paths and listens to DAM events on this paths to generate
 * a new data version on each DAM content change relevant for the DAM asset services consumers.
 */
class DamPathHandler {

  /**
   * Full DAM path is used if not DAM path is given in configuration.
   */
  private static final String DEFAULT_DAM_PATH = DamConstants.MOUNTPOINT_ASSETS;

  private final Set<String> damPaths;
  private final Pattern damPathsPattern;

  private volatile String dataVersion;

  public DamPathHandler(final String[] configuredDamPaths) {
    this.damPaths = validateDamPaths(configuredDamPaths);
    this.damPathsPattern = buildDamPathsPattern(this.damPaths);
    generateNewDataVersion();
  }

  private static Set<String> validateDamPaths(String[] damPaths) {
    Set<String> paths = new HashSet<>();
    if (damPaths != null) {
      for (String path : damPaths) {
        if (StringUtils.isNotBlank(path)) {
          paths.add(path);
        }
      }
    }
    if (paths.isEmpty()) {
      paths.add(DEFAULT_DAM_PATH);
    }
    return ImmutableSet.copyOf(paths);
  }

  /**
   * Set DAM paths that should be handled. Only called once by {@link AssetRequestServlet}.
   * @param damPaths DAM folder paths or empty/null if all should be handled.
   */
  private static Pattern buildDamPathsPattern(Set<String> damPaths) {
    StringBuilder pattern = new StringBuilder();
    pattern.append("^(");
    Iterator<String> paths = damPaths.iterator();
    while (paths.hasNext()) {
      pattern.append(Pattern.quote(paths.next()));
      pattern.append("/.*");
      if (paths.hasNext()) {
        pattern.append("|");
      }
    }
    pattern.append(")$");
    return Pattern.compile(pattern.toString());
  }

  /**
   * Checks if the given DAM asset is allowed to process.
   * @param assetPath Asset path
   * @return true if processing is allowed.
   */
  public boolean isAllowedAssetPath(String assetPath) {
    return damPathsPattern.matcher(assetPath).matches();
  }

  /**
   * Checks if the given folder path is allowed to get a data version.
   * @param path DAM folder path
   * @return true if getting data version is allowed for this path.
   */
  public boolean isAllowedDataVersionPath(String path) {
    return damPaths.contains(path);
  }

  /**
   * Get current data version for all allowed assets.
   * @return Data version
   */
  public String getDataVersion() {
    return dataVersion;
  }

  public void handleDamEvent(DamEvent event) {
    if (isAllowedAssetPath(event.getAssetPath())) {
      // generate a new data version on any DAM event affecting any of the configured paths
      generateNewDataVersion();
    }
  }

  /**
   * Generates a new data version based on current timestamp.
   */
  private void generateNewDataVersion() {
    // use timestamp as data version. clashing of versions if two are generated at exactly the same time point
    // is not the problem, because the data version can then be the same.
    dataVersion = ISO8601.format(Calendar.getInstance());
  }

}
