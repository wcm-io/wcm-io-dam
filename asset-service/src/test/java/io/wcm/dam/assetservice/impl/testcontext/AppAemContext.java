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
package io.wcm.dam.assetservice.impl.testcontext;

import io.wcm.config.spi.ApplicationProvider;
import io.wcm.handler.media.spi.MediaFormatProvider;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;
import io.wcm.testing.mock.wcmio.handler.MockHandler;

import java.io.IOException;

import org.apache.sling.api.resource.PersistenceException;

/**
 * Sets up {@link AemContext} for unit tests in this application.
 */
public final class AppAemContext {

  public static final String APPLICATION_ID = "/apps/testapp";

  private AppAemContext() {
    // static methods only
  }

  public static AemContext newAemContext() {
    return new AemContext(new SetUpCallback());
  }

  /**
   * Custom set up rules required in all unit tests.
   */
  private static final class SetUpCallback implements AemContextCallback {

    @Override
    public void execute(AemContext context) throws PersistenceException, IOException {
      context.registerService(ApplicationProvider.class, new ApplicationProviderImpl());
      context.registerService(MediaFormatProvider.class, new MediaFormatProviderImpl());
      MockHandler.setUp(context);
    }

  }

}
