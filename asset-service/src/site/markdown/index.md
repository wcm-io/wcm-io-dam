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


[rest-interface]: rest-interface.html
[configuration]: configuration.html
[changelog]: changes-report.html
[media-handler]: http://wcm.io/handler/media/
