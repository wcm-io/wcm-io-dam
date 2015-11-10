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

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.DamEvent;

/**
 * Common functionality for data version strategy implementations.
 */
public abstract class DataVersionStrategy {

  protected final String damPath;
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Pattern pathPattern;

  /**
   * @param damPath DAM root path
   */
  public DataVersionStrategy(String damPath) {
    this.damPath = damPath;
    this.pathPattern = Pattern.compile("^" + Pattern.quote(damPath) + "(/.*)?$");
  }

  /**
   * @param path DAM asset or asset folder path
   * @return true if path matches.
   */
  public final boolean matches(String path) {
    return pathPattern.matcher(path).matches();
  }

  /**
   * Is called when a DAM event affecting any asset within the DAM path occurs.
   * @param damEvent DAM event
   */
  public abstract void handleDamEvent(DamEvent damEvent);

  /**
   * Returns data version for this DAM root path
   * @return Data version. Never null.
   */
  public abstract String getDataVersion();

}
