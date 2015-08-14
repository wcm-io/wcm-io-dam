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

import io.wcm.wcm.commons.contenttype.FileExtension;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

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

  @Property(label = "DAM paths", description = "List of DAM paths for which the asset service should be active. "
      + "If not set, the service is active for all paths.",
      cardinality = Integer.MAX_VALUE)
  static final String DAM_PATHS_PROPERTY = "damPaths";

  private DamPathHandler damPathHandler;
  private BundleContext bundleContext;
  private ServiceRegistration assetRequestServletReg;

  @Activate
  protected void activate(ComponentContext componentContext) {
    bundleContext = componentContext.getBundleContext();
    Dictionary config = componentContext.getProperties();

    String assetServletSelector = PropertiesUtil.toString(config.get(ASSET_SERVLET_SELECTOR_PROPERTY), null);
    String dataVersionServletSelector = PropertiesUtil.toString(config.get(DATAVERSION_SERVLET_SELECTOR_PROPERTY), null);

    String[] damPaths = PropertiesUtil.toStringArray(config.get(DAM_PATHS_PROPERTY));
    damPathHandler = new DamPathHandler(damPaths);

    // register servlets to resource types to handle the JSON requests
    assetRequestServletReg = registerServlet(bundleContext, new AssetRequestServlet(damPathHandler),
        DamConstants.NT_DAM_ASSET, assetServletSelector);
  }

  @Deactivate
  protected void deactivate(ComponentContext componentContext) {
    assetRequestServletReg.unregister();
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
