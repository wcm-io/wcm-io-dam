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

import io.wcm.sling.commons.adapter.AdaptTo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.DamEvent;
import com.google.common.collect.ImmutableSet;

/**
 * Handles list of configured DAM paths and listens to DAM events on this paths to generate
 * a new data version on each DAM content change relevant for the DAM asset services consumers.
 * Make sure you call the shutdown method when the instance is no longer needed.
 */
class DamPathHandler {

  /**
   * Full DAM path is used if not DAM path is given in configuration.
   */
  private static final String DEFAULT_DAM_PATH = DamConstants.MOUNTPOINT_ASSETS;

  /**
   * Default data version that is returned when no data version was yet calculated.
   */
  private static final String DATAVERSION_NOT_CALCULATED = "-1";

  private final Set<String> damPaths;
  private final Pattern damPathsPattern;
  private final String dataVersionQueryString;
  private final ResourceResolverFactory resourceResolverFactory;
  private final ScheduledExecutorService executor;

  private volatile String dataVersion;
  private volatile boolean dataVersionIsStale;

  private static final Logger log = LoggerFactory.getLogger(DamPathHandler.class);

  public DamPathHandler(final String[] configuredDamPaths, int dataVersionUpdateIntervalSec,
      ResourceResolverFactory resourceResolverFactory) {
    this.damPaths = validateDamPaths(configuredDamPaths);
    this.damPathsPattern = buildDamPathsPattern(this.damPaths);
    this.dataVersionQueryString = buildDataVersionQueryString(this.damPaths);
    this.resourceResolverFactory = resourceResolverFactory;
    this.executor = Executors.newSingleThreadScheduledExecutor();

    this.dataVersion = DATAVERSION_NOT_CALCULATED;
    this.dataVersionIsStale = true;

    if (this.damPaths.size() == 0) {
      log.warn("No DAM paths configured.");
    }
    else if (dataVersionUpdateIntervalSec <= 0) {
      log.warn("Invalid data version update interval: " + dataVersionUpdateIntervalSec + " sec");
    }
    else {
      Runnable task = new UpdateDataVersionTask();
      this.executor.scheduleWithFixedDelay(task, 0, dataVersionUpdateIntervalSec, TimeUnit.SECONDS);
    }
  }

  /**
   * Shuts down the executor service.
   */
  public void shutdown() {
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
   * Builds query string to fetch SHA-1 hashsums for all DAM assets lying in on of the configured DAM asset folders.
   * @param damPaths DAM folder paths or empty/null if all should be handled.
   * @return SQL2 query string
   */
  private static String buildDataVersionQueryString(Set<String> damPaths) {
    StringBuilder statement = new StringBuilder("select [" + JcrConstants.JCR_PATH + "], [jcr:content/metadata/dam:sha1] " +
        " from [" + DamConstants.NT_DAM_ASSET + "] as a " +
        " where ");
    Iterator<String> damPathsIterator = damPaths.iterator();
    while (damPathsIterator.hasNext()) {
      String damPath = damPathsIterator.next();
      statement.append(" isdescendantnode(a, '" + damPath + "')");
      if (damPathsIterator.hasNext()) {
        statement.append(" or");
      }
    }
    statement.append(" order by [" + JcrConstants.JCR_PATH + "]");
    return statement.toString();
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
      // mark data version as stale on any DAM event affecting any of the configured paths
      dataVersionIsStale = true;
    }
  }


  /**
   * Scheduled task to generate a new data version via JCR query.
   */
  private class UpdateDataVersionTask implements Runnable {

    @Override
    public void run() {
      if (!dataVersionIsStale) {
        log.debug("Data version '{}' is not stale, skip generation of new data version.", dataVersion);
      }
      try {
        log.debug("Data version '{}' is stale, start generation of new data version.", dataVersion);
        generateDataVersion();
      }
      catch (LoginException ex) {
        log.error("Unable to get service resource resolver, please check service user configuration: " + ex.getMessage());
      }
      catch (Throwable ex) {
        log.error("Error generating data version: " + ex.getMessage(), ex);
      }
    }

    /**
     * Generates a data version by fetching all paths and SHA-1 strings from DAM asset folders (lucene index).
     * The data version is a check sum over all path and SHA-1 strings found.
     * @throws LoginException
     * @throws RepositoryException
     */
    private void generateDataVersion() throws LoginException, RepositoryException {
      ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(null);
      try {
        Session session = AdaptTo.notNull(resourceResolver, Session.class);
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(dataVersionQueryString, Query.JCR_SQL2);
        QueryResult result = query.execute();
        RowIterator rows = result.getRows();

        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        int assetCount = 0;
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();

        while (rows.hasNext()) {
          Row row = rows.nextRow();
          String path = row.getValue(JcrConstants.JCR_PATH).getString();
          String sha1 = row.getValue("jcr:content/metadata/dam:sha1").getString();
          log.trace("Found sha-1 at {}: {}", path, sha1);

          hashCodeBuilder.append(path);
          hashCodeBuilder.append(sha1);
          assetCount++;
        }

        dataVersion = Integer.toString(hashCodeBuilder.build());
        dataVersionIsStale = false;

        stopwatch.stop();
        log.info("Generated new data version {} from {} assets (duration: {}ms).", dataVersion, assetCount, stopwatch.getTime());
      }
      finally {
        resourceResolver.close();
      }
    }

  }

}
