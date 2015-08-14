## Configuration


The DAM asset service can be configured via the "wcm.io DAM Asset Service" entry in the Felix Web Console.

Supported parameters:

* **Asset Selector**: Selector for attaching REST service to DAM asset paths.
* **Data Version Selector**: Selector for attaching REST service to DAM folder for getting data version.
* **DAM paths**: List of DAM paths for which the asset service should be active. If not set, the service is active for all paths.

If not path is given it defaults to `/content/dam`.
