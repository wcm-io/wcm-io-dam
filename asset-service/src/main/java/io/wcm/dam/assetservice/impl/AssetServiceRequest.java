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

import io.wcm.handler.media.Media;
import io.wcm.handler.media.MediaHandler;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Holds parameters for an asset service request;
 */
class AssetServiceRequest {

  private final String assetPath;
  private final String mediaFormatName;
  private final long width;
  private final long height;

  public AssetServiceRequest(String assetPath, String mediaFormatName, long width, long height) {
    this.assetPath = assetPath;
    this.mediaFormatName = mediaFormatName;
    this.width = width;
    this.height = height;
  }

  public Media resolve(MediaHandler mediaHandler) {
    return mediaHandler.get(assetPath)
        .mediaFormatName(mediaFormatName)
        .fixedDimension(width, height)
        .build();
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
