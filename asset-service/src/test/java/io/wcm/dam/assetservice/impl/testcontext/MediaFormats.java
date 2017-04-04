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

import static io.wcm.handler.media.format.MediaFormatBuilder.create;

import io.wcm.handler.media.format.MediaFormat;

/**
 * Media formats
 */
public final class MediaFormats {

  private MediaFormats() {
    // constants only
  }

  public static final MediaFormat FORMAT_4_3 = create("format_4_3")
      .label("Media Format 4:3")
      .minWidth(1900)
      .minHeight(1425)
      .ratio(4, 3)
      .extensions("gif", "jpg", "jpeg", "png")
      .build();

  public static final MediaFormat FORMAT_32_9 = create("format_32_9")
      .label("Media Format 32:9")
      .minWidth(1920)
      .minHeight(540)
      .ratio(32, 9)
      .extensions("gif", "jpg", "jpeg", "png")
      .build();

}
