## About Asset Service

A RESTful service for resolving URLs to assets and renditions.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.dam.asset-service/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.dam.asset-service)


### Documentation

* [REST Interface][rest-interface]
* [Configuration][configuration]
* [Changelog][changelog]


### Overview

The DAM Asset Services publishes a REST interface which allows to check if a DAM asset path is valid and replicated to a AEM instance. It resolves URLs to real or virtual renditions rendered on-the-fly. A data version is generated for each configured DAM asset folder to check if any asset was changed since last check.

Internally the [wcm.io Media Handler][media-handler] is used for building the URLs to the renditions. They can be requested by dimensions or media formats.

See [REST Interface][rest-interface] for a description of the interface.


### AEM Version Support Matrix

|DAM Asset Service version |AEM version supported
|--------------------------|----------------------
|1.4.x or higher           |AEM 6.3+
|1.3.x                     |AEM 6.1+
|1.0.x - 1.2.x             |AEM 6.0+


### Dependencies

To use this module you have to deploy also:

|---|---|---|
| [wcm.io Sling Commons](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.sling.commons) | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.sling.commons/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.sling.commons) |
| [wcm.io AEM Sling Models Extensions](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.sling.models) | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.sling.models/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.sling.models) |
| [wcm.io WCM Commons](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.wcm.commons) | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.wcm.commons/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.wcm.commons) |
| [wcm.io Handler Commons](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.handler.commons) | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.handler.commons/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.handler.commons) |
| [wcm.io URL Handler](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.handler.url) | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.handler.url/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.handler.url) |
| [wcm.io Media Handler](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.handler.media) | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.handler.media/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.handler.media) |



[rest-interface]: rest-interface.html
[configuration]: configuration.html
[changelog]: changes-report.html
[media-handler]: https://wcm.io/handler/media/
