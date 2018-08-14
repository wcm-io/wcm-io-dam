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

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.DamEvent;

import io.wcm.dam.assetservice.impl.dataversion.ChecksumDataVersionStrategy;
import io.wcm.dam.assetservice.impl.dataversion.TimestampDataVersionStrategy;
import io.wcm.wcm.commons.contenttype.FileExtension;

/**
 * Implements a simple REST interface that allows resolving DAM asset paths to URLs.
 * For image assets resolving to specific dimensions is supported.
 */
@Component(immediate = true, service = { AssetService.class, EventHandler.class },
    property = EventConstants.EVENT_TOPIC + "=" + DamEvent.EVENT_TOPIC)
@Designate(ocd = AssetService.Config.class)
public class AssetService implements EventHandler {

  @ObjectClassDefinition(name = "wcm.io DAM Asset Service",
      description = "A RESTful service for resolving URLs to DAM assets and renditions.")
  static @interface Config {

    @AttributeDefinition(name = "Asset Selector", description = "Selector for attaching REST service to DAM asset paths.")
    String assetServletSelector() default "wcm-io-asset-service";

    @AttributeDefinition(name = "Data Version Selector", description = "Selector for attaching REST service to DAM folder for getting data version.")
    String dataVersionServletSelector() default "wcm-io-asset-service-dataversion";

    @AttributeDefinition(name = "Data Version Strategy", description = "Strategy for building the data versions. See documentation for details.",
        options = {
            @Option(label = TimestampDataVersionStrategy.STRATEGY + ": Timestamp of last DAM event", value = TimestampDataVersionStrategy.STRATEGY),
            @Option(label = ChecksumDataVersionStrategy.STRATEGY + ": Aggregated checksum of DAM assets", value = ChecksumDataVersionStrategy.STRATEGY)
        })
    String dataVersionStrategy() default TimestampDataVersionStrategy.STRATEGY;

    @AttributeDefinition(name = "Update Interval (sec)", description = "Updating interval for calculating data versions in seconds. "
        + "If multiple changes to the DAM folders contents are detected within this interval they are collected. "
        + "This is only used by the 'aggregated checksum' strategy.")
    int dataVersionUpdateIntervalSec() default 60;

    @AttributeDefinition(name = "DAM paths", description = "List of DAM paths for which the asset service should be active. "
        + "If not set, the service is active for all paths.")
    String[] damPaths();

  }

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  private DamPathHandler damPathHandler;
  private BundleContext bundleContext;
  private ServiceRegistration<Servlet> assetRequestServletReg;
  private ServiceRegistration<Servlet> dataVersionServletReg;

  private static final Logger log = LoggerFactory.getLogger(AssetService.class);

  @Activate
  protected void activate(BundleContext context, Config config) {
    log.info("Start wcm.io DAM Asset Service.");

    this.bundleContext = context;

    String assetServletSelector = config.assetServletSelector();
    String dataVersionServletSelector = config.dataVersionServletSelector();
    int dataVersionUpdateIntervalSec = config.dataVersionUpdateIntervalSec();

    String[] damPaths = config.damPaths();
    String dataVersionStrategyId = config.dataVersionStrategy();
    damPathHandler = new DamPathHandler(damPaths, dataVersionStrategyId, dataVersionUpdateIntervalSec, resourceResolverFactory);

    // register servlets to resource types to handle the JSON requests
    // they are registered dynamically because the selectors are configurable
    assetRequestServletReg = registerServlet(context, new AssetRequestServlet(damPathHandler),
        DamConstants.NT_DAM_ASSET, assetServletSelector);
    dataVersionServletReg = registerServlet(context, new DataVersionServlet(damPathHandler),
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

  @SuppressWarnings("null")
  private static <T extends Servlet> ServiceRegistration<Servlet> registerServlet(BundleContext bundleContext, T servletInstance,
      String resourceType, String selector) {
    if (StringUtils.isEmpty(selector)) {
      throw new IllegalArgumentException("No selector defined for " + servletInstance.getClass().getName() + " - skipping servlet registration.");
    }
    Dictionary<String, Object> config = new Hashtable<String, Object>();
    config.put("sling.servlet.resourceTypes", resourceType);
    config.put("sling.servlet.selectors", selector);
    config.put("sling.servlet.extensions", FileExtension.JSON);
    return bundleContext.registerService(Servlet.class, servletInstance, config);
  }

}
