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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.DamEvent;

import io.wcm.sling.commons.adapter.AdaptTo;

/**
 * Strategy that generates a checksum bases on all DAM asset's path and last modified dates within the DAM asset folder.
 * The aggregated checksum is built by executing a JCR query using the AEM-predefined OAK index
 * <code>damAssetLucene</code>. Executing the query does not touch the JCR content at all, it only reads
 * JCR path and sha-1 string from the lucene index. This query is executed max. once during the "update interval",
 * and only if DAM events occurred since the last checksum generation.
 */
public class ChecksumDataVersionStrategy extends DataVersionStrategy {

  /**
   * Data version strategy id for configuration persistence.
   */
  public static final String STRATEGY = "checksum";

  /**
   * Default data version that is returned when no data version was yet calculated.
   */
  private static final String DATAVERSION_NOT_CALCULATED = "unknown";

  /**
   * DAM asset property containing the last modified date.
   */
  private static final String LAST_MODIFIED_PROPERTY = JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_LASTMODIFIED;

  private final long dataVersionUpdateIntervalMs;
  private final String dataVersionQueryString;
  private final ResourceResolverFactory resourceResolverFactory;

  private volatile String dataVersion;
  private volatile long dataVersionLastUpdate;
  private volatile long damEventLastOccurence;

  /**
   * @param damPath DAM root path
   * @param dataVersionUpdateIntervalSec Data version update interval
   * @param resourceResolverFactory Resource resolver factory
   * @param executor Shared executor service instance
   */
  public ChecksumDataVersionStrategy(String damPath,
      int dataVersionUpdateIntervalSec,
      ResourceResolverFactory resourceResolverFactory,
      ScheduledExecutorService executor) {
    super(damPath);

    this.dataVersionUpdateIntervalMs = dataVersionUpdateIntervalSec * DateUtils.MILLIS_PER_SECOND;
    this.resourceResolverFactory = resourceResolverFactory;
    this.dataVersionQueryString = buildDataVersionQueryString(damPath);

    this.dataVersion = DATAVERSION_NOT_CALCULATED;

    if (dataVersionUpdateIntervalSec <= 0) {
      log.warn("{} - Invalid data version update interval: {} sec", damPath, dataVersionUpdateIntervalSec);
    }
    else {
      Runnable task = new UpdateDataVersionTask();
      executor.scheduleWithFixedDelay(task, 0, dataVersionUpdateIntervalSec, TimeUnit.SECONDS);
    }
  }

  /**
   * Builds query string to fetch properties for all DAM assets lying in on of the configured DAM asset folders.
   * @param damPath DAM root path
   * @return SQL2 query string
   */
  private static String buildDataVersionQueryString(String damPath) {
    return "select [" + JcrConstants.JCR_PATH + "], [" + LAST_MODIFIED_PROPERTY + "] "
        + "from [" + DamConstants.NT_DAM_ASSET + "] as a "
        + "where isdescendantnode(a, '" + damPath + "') "
        + "order by [" + JcrConstants.JCR_PATH + "]";
  }

  @Override
  public void handleDamEvent(DamEvent damEvent) {
    damEventLastOccurence = System.currentTimeMillis();
  }

  @Override
  public String getDataVersion() {
    return dataVersion;
  }


  /**
   * Scheduled task to generate a new data version via JCR query.
   */
  private class UpdateDataVersionTask implements Runnable {

    @Override
    public void run() {
      if (!isDataVersionStale()) {
        log.debug("{} - Data version '{}' is not stale, skip generation of new data version.", damPath, dataVersion);
        return;
      }
      try {
        log.debug("{} - Data version '{}' is stale, start generation of new data version.", damPath, dataVersion);
        generateDataVersion();
      }
      catch (LoginException ex) {
        log.error("{} - Unable to get service resource resolver, please check service user configuration: {}", damPath, ex.getMessage());
      }
      /*CHECKSTYLE:OFF*/ catch (Exception ex) { /*CHECKSTYLE:ON*/
        log.error("{} - Error generating data version: {}", damPath, ex.getMessage(), ex);
      }
    }

    private boolean isDataVersionStale() {
      if (dataVersionLastUpdate == 0) {
        return true;
      }
      // mark data version is stale if last update was after last DAM event
      // add an additional interval's length to the comparison because the lucene is updated asynchronously
      // and thus the DAM event may arrive before the updated properties are available in the index
      return (dataVersionLastUpdate < damEventLastOccurence + dataVersionUpdateIntervalMs);
    }

    /**
     * Generates a data version by fetching all paths and properties from DAM asset folders (lucene index).
     * The data version is a check sum over all path and selected properties found.
     */
    @SuppressWarnings("null")
    private void generateDataVersion() throws LoginException, RepositoryException {
      log.trace("{} - Start data version generation.", damPath);
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
          String path = getStringValue(row, JcrConstants.JCR_PATH);
          Calendar lastModified = getCalendarValue(row, LAST_MODIFIED_PROPERTY);
          log.trace("{} - Found sha-1 at {}: {}", damPath, path, lastModified);

          hashCodeBuilder.append(path);
          if (lastModified != null) {
            hashCodeBuilder.append(lastModified);
          }
          else {
            log.debug("{} - No last modified date found for {}", damPath, path);
          }
          assetCount++;
        }

        dataVersion = Integer.toString(hashCodeBuilder.build());
        dataVersionLastUpdate = System.currentTimeMillis();

        stopwatch.stop();
        log.info("{} - Generated new data version {} for {} assets (duration: {}ms).",
            damPath, dataVersion, assetCount, stopwatch.getTime());
      }
      finally {
        resourceResolver.close();
      }
    }

    private String getStringValue(Row row, String property) throws RepositoryException {
      Value value = row.getValue(property);
      if (value != null) {
        return value.getString();
      }
      else {
        return null;
      }
    }

    private Calendar getCalendarValue(Row row, String property) throws RepositoryException {
      Value value = row.getValue(property);
      if (value != null) {
        return value.getDate();
      }
      else {
        return null;
      }
    }

  }

}
