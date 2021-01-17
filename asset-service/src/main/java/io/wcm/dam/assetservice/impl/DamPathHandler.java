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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.DamEvent;
import com.google.common.collect.ImmutableSet;

import io.wcm.dam.assetservice.impl.dataversion.ChecksumDataVersionStrategy;
import io.wcm.dam.assetservice.impl.dataversion.DataVersionStrategy;
import io.wcm.dam.assetservice.impl.dataversion.TimestampDataVersionStrategy;

/**
 * Handles list of configured DAM paths and listens to DAM events on this paths to generate
 * a new data version on each DAM content change relevant for the DAM asset services consumers.
 * Make sure you call the shutdown method when the instance is no longer needed.
 */
public class DamPathHandler {

  /**
   * Full DAM path is used if not DAM path is given in configuration.
   */
  private static final String DEFAULT_DAM_PATH = DamConstants.MOUNTPOINT_ASSETS;

  private final Set<String> damPaths;
  private final Pattern damPathsPattern;
  private final ScheduledExecutorService executor;
  private final Map<String, DataVersionStrategy> dataVersionStrategies;

  private static final Logger log = LoggerFactory.getLogger(DamPathHandler.class);

  /**
   * @param configuredDamPaths Configured DAM paths
   * @param dataVersionStrategyId Data version strategy
   * @param dataVersionUpdateIntervalSec Update interface
   * @param resourceResolverFactory Resource resolver factory
   */
  public DamPathHandler(final String[] configuredDamPaths,
      String dataVersionStrategyId,
      int dataVersionUpdateIntervalSec,
      ResourceResolverFactory resourceResolverFactory) {


    this.damPaths = validateDamPaths(configuredDamPaths);
    this.damPathsPattern = buildDamPathsPattern(this.damPaths);

    log.debug("Start executor for DamPathHandler");
    this.executor = Executors.newSingleThreadScheduledExecutor();

    dataVersionStrategies = new HashMap<>();
    for (String damPath : this.damPaths) {
      DataVersionStrategy dataVersionStrategy = getDataVersionStrategy(damPath, dataVersionStrategyId,
          dataVersionUpdateIntervalSec, resourceResolverFactory, this.executor);
      dataVersionStrategies.put(damPath, dataVersionStrategy);
    }
  }

  private static DataVersionStrategy getDataVersionStrategy(String damPath, String dataVersionStrategyId,
      int dataVersionUpdateIntervalSec, ResourceResolverFactory resourceResolverFactory,
      ScheduledExecutorService executor) {
    if (StringUtils.equals(dataVersionStrategyId, TimestampDataVersionStrategy.STRATEGY)) {
      return new TimestampDataVersionStrategy(damPath);
    }
    if (StringUtils.equals(dataVersionStrategyId, ChecksumDataVersionStrategy.STRATEGY)) {
      return new ChecksumDataVersionStrategy(damPath, dataVersionUpdateIntervalSec, resourceResolverFactory, executor);
    }
    throw new IllegalArgumentException("Invalid data version strategy: " + dataVersionStrategyId);
  }

  /**
   * Shuts down the executor service.
   */
  public void shutdown() {
    log.debug("Shutdown executor for DamPathHandler");
    this.executor.shutdownNow();
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
   * @return Regex pattern to match content paths
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
    if (assetPath == null) {
      return false;
    }
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
  public String getDataVersion(String damPath) {
    DataVersionStrategy dataVersionStrategy = this.dataVersionStrategies.get(damPath);
    if (dataVersionStrategy != null) {
      return dataVersionStrategy.getDataVersion();
    }
    else {
      return null;
    }
  }

  /**
   * Handle DAM event.
   * @param event DAM event
   */
  public void handleDamEvent(DamEvent event) {
    if (isAllowedAssetPath(event.getAssetPath())) {
      // route event to matching data version strategy instance
      DataVersionStrategy dataVersionStrategy = getMatchingDataVersionStrategy(event.getAssetPath());
      if (dataVersionStrategy != null) {
        dataVersionStrategy.handleDamEvent(event);
      }
    }
  }

  private DataVersionStrategy getMatchingDataVersionStrategy(String path) {
    // shortcut if there is only one path configured
    if (dataVersionStrategies.size() == 1) {
      return dataVersionStrategies.values().iterator().next();
    }
    // find matching strategy for path
    for (DataVersionStrategy dataVersionStrategy : this.dataVersionStrategies.values()) {
      if (dataVersionStrategy.matches(path)) {
        return dataVersionStrategy;
      }
    }
    return null;
  }

}
