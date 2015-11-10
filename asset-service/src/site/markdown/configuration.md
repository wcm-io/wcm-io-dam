## Configuration


### OSGi configuration

The DAM asset service can be configured via the "wcm.io DAM Asset Service" entry in the Felix Web Console.

Supported parameters:

* **Asset Selector**: Selector for attaching REST service to DAM asset paths.
* **Data Version Selector**: Selector for attaching REST service to DAM folder for getting data version.
* **Data Version Strategy**: Strategy for building the data versions (see below)
* **Update Interval (sec)**: Updating interval for calculating data versions in seconds. If multiple changes to the DAM folders contents are detected within this interval they are collected. This is only used by the 'aggregated checksum' strategy.
* **DAM paths**: List of DAM paths for which the asset service should be active. If not set, the service is active for all paths.

If not path is given it defaults to `/content/dam`.


### Data Version Strategies

The DAM asset service generates a data version for each configured DAM asset path. The data version is updated whenever an asset was changed within this subtree of DAM. There are multiple strategies to generate such a data version available.

Notice on handling data versions within in your application: You should not try to interpret the data version string but take it just as an arbitrary string. You should not compare data versions, it is not guaranteed that the lexical ordering of two data versions matches with the temporal order.


#### timestamp Data Version Strategy (default)

Simple strategy to generate data versions - on each DAM event a new timestamp is generated and returned as data version. Please be aware that this does not produce stable data versions across a cluster of AEM instances.

It should only be used if there is only one AEM instance generating the data version, or some sort of long-stable stickyness is applied on the load balancer.


#### checksum Data Version Strategy

Strategy that generates a checksum bases on all DAM asset's path and SHA-1 checksums within the DAM asset folder.

The aggregated checksum is built by executing a JCR query using the AEM-predefined OAK index `damAssetLucene`. Executing the query does not touch the JCR content at all, it only reads JCR path and sha-1 string from the lucene index. This query is executed max. once during the "update interval", and only if DAM events occurred since the last checksum generation.

This strategy produced consistent data versions across multiple AEM instances.

To use this strategy a service use has to be configured, see below.


### Service User Configuration

If the 'checksum' data version strategy is used the DAM asset service needs a service user mapping for the factory configuration `org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended` with an entry like this:

```
user.mapping="[io.wcm.dam.asset-service=idsjobprocessor]"
```

This configuration is only required on Author instances. In this example an existing user "idsjobprocessor" from AEM 6.1 is re-used, you should create a new system user that has read permissions on `/content/dam`.
