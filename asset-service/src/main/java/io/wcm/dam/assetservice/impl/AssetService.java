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

import io.wcm.dam.assetservice.impl.dataversion.ChecksumDataVersionStrategy;
import io.wcm.dam.assetservice.impl.dataversion.TimestampDataVersionStrategy;
import io.wcm.wcm.commons.contenttype.FileExtension;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.DamEvent;

/**
 * Implements a simple REST interface that allows resolving DAM asset paths to URLs.
 * For image assets resolving to specific dimensions is supported.
 */
@Component(immediate = true, metatype = true,
label = "wcm.io DAM Asset Service",
description = "A RESTful service for resolving URLs to DAM assets and renditions.")
@Service({
  AssetService.class, EventHandler.class
})
@Property(name = EventConstants.EVENT_TOPIC, value = DamEvent.EVENT_TOPIC, propertyPrivate = true)
public class AssetService implements EventHandler {

  @Property(label = "Asset Selector", description = "Selector for attaching REST service to DAM asset paths.",
      value = AssetService.ASSET_SERVLET_SELECTOR_PROPERTY_DEFAULT)
  static final String ASSET_SERVLET_SELECTOR_PROPERTY = "assetServletSelector";
  static final String ASSET_SERVLET_SELECTOR_PROPERTY_DEFAULT = "wcm-io-asset-service";

  @Property(label = "Data Version Selector", description = "Selector for attaching REST service to DAM folder for getting data version.",
      value = AssetService.DATAVERSION_SERVLET_SELECTOR_PROPERTY_DEFAULT)
  static final String DATAVERSION_SERVLET_SELECTOR_PROPERTY = "dataVersionServletSelector";
  static final String DATAVERSION_SERVLET_SELECTOR_PROPERTY_DEFAULT = "wcm-io-asset-service-dataversion";

  @Property(label = "Data Version Strategy", description = "Strategy for building the data versions. See documentation for details.",
      value = AssetService.DATAVERSION_STRATEGY_DEFAULT,
      options = {
      @PropertyOption(name = "Timestamp of last DAM event", value = TimestampDataVersionStrategy.STRATEGY),
      @PropertyOption(name = "Aggregated checksum of DAM assets", value = ChecksumDataVersionStrategy.STRATEGY)
  })
  static final String DATAVERSION_STRATEGY_PROPERTY = "dataVersionStrategy";
  static final String DATAVERSION_STRATEGY_DEFAULT = TimestampDataVersionStrategy.STRATEGY;

  @Property(label = "Update Interval (sec)", description = "Updating interval for calculating data versions in seconds. "
      + "If multiple changes to the DAM folders contents are detected within this interval they are collected. "
      + "This is only used by the 'aggregated checksum' strategy.",
      intValue = AssetService.DATAVERSION_UPDATE_INTERVAL_SEC_DEFAULT)
  static final String DATAVERSION_UPDATE_INTERVAL_SEC_PROPERTY = "dataVersionUpdateIntervalSec";
  static final int DATAVERSION_UPDATE_INTERVAL_SEC_DEFAULT = 2 * 60;

  @Property(label = "DAM paths", description = "List of DAM paths for which the asset service should be active. "
      + "If not set, the service is active for all paths.",
      cardinality = Integer.MAX_VALUE)
  static final String DAM_PATHS_PROPERTY = "damPaths";

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  private DamPathHandler damPathHandler;
  private BundleContext bundleContext;
  private ServiceRegistration assetRequestServletReg;
  private ServiceRegistration dataVersionServletReg;

  private static final Logger log = LoggerFactory.getLogger(AssetService.class);

  @Activate
  protected void activate(ComponentContext componentContext) {
    log.info("Start wcm.io DAM Asset Service.");

    bundleContext = componentContext.getBundleContext();
    Dictionary config = componentContext.getProperties();

    String assetServletSelector = PropertiesUtil.toString(config.get(ASSET_SERVLET_SELECTOR_PROPERTY),
        ASSET_SERVLET_SELECTOR_PROPERTY_DEFAULT);
    String dataVersionServletSelector = PropertiesUtil.toString(config.get(DATAVERSION_SERVLET_SELECTOR_PROPERTY),
        DATAVERSION_SERVLET_SELECTOR_PROPERTY_DEFAULT);
    int dataVersionUpdateIntervalSec = PropertiesUtil.toInteger(config.get(DATAVERSION_UPDATE_INTERVAL_SEC_PROPERTY),
        DATAVERSION_UPDATE_INTERVAL_SEC_DEFAULT);

    String[] damPaths = PropertiesUtil.toStringArray(config.get(DAM_PATHS_PROPERTY));
    String dataVersionStrategyId = PropertiesUtil.toString(config.get(DATAVERSION_STRATEGY_PROPERTY), DATAVERSION_STRATEGY_DEFAULT);
    damPathHandler = new DamPathHandler(damPaths, dataVersionStrategyId, dataVersionUpdateIntervalSec, resourceResolverFactory);

    // register servlets to resource types to handle the JSON requests
    // they are registered dynamically because the selectors are configurable
    assetRequestServletReg = registerServlet(bundleContext, new AssetRequestServlet(damPathHandler),
        DamConstants.NT_DAM_ASSET, assetServletSelector);
    dataVersionServletReg = registerServlet(bundleContext, new DataVersionServlet(damPathHandler),
        "sling:OrderedFolder", dataVersionServletSelector);
  }

  @Deactivate
  protected void deactivate(ComponentContext componentContext) {
    log.info("Shutdown wcm.io DAM Asset Service.");

    assetRequestServletReg.unregister();
    dataVersionServletReg.unregister();
    damPathHandler.shutdown();
  }

  @Override
  public void handleEvent(Event event) {
    if (!StringUtils.equals(event.getTopic(), DamEvent.EVENT_TOPIC)) {
      return;
    }
    DamEvent damEvent = DamEvent.fromEvent(event);
    damPathHandler.handleDamEvent(damEvent);
  }

  AssetRequestServlet getAssetRequestServlet() {
    return (AssetRequestServlet)bundleContext.getService(assetRequestServletReg.getReference());
  }

  DataVersionServlet getDataVersionServlet() {
    return (DataVersionServlet)bundleContext.getService(dataVersionServletReg.getReference());
  }

  private static <T extends Servlet> ServiceRegistration registerServlet(BundleContext bundleContext, T servletInstance,
      String resourceType, String selector) {
    if (StringUtils.isEmpty(selector)) {
      throw new IllegalArgumentException("No selector defined for " + servletInstance.getClass().getName() + " - skipping servlet registration.");
    }
    Dictionary<String, Object> config = new Hashtable<String, Object>();
    config.put("sling.servlet.resourceTypes", resourceType);
    config.put("sling.servlet.selectors", selector);
    config.put("sling.servlet.extensions", FileExtension.JSON);
    return bundleContext.registerService(Servlet.class.getName(), servletInstance, config);
  }

}
