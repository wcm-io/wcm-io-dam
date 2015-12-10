## Asset Service REST Interface


### Resolve assets

To get the asset metadata for a certain asset path use an URL with this syntax:

```
<cms-hostname><asset_path>.wcm-io-asset-service.json/<suffix-params-1>/.../<suffix-params-n>.json
```

After the asset path the selector `.wcm-io-asset-service.json` has to be added. This selector name can be changed in the [configuration][configuration].

Optionally a set of further suffix parameters each separated by '`/`' can be added. Each set consists of key-value pairs. Each pair is separated by '`,`', the key and value are separated by '`=`'. The whole suffix string is terminated with another `.json`. Example:

```
<cms-hostname><asset_path>.wcm-io-asset-service.json/width=200,height=100/mediaFormat=mediaformat1.json
```

The list of supported parameters in the suffix sets:

| Parameter     | Multiple | Description
|---------------|:--------:|-------------
| `mediaFormat` | X        | Specifies internal media format name of the CMS application
| `width`       | X        | Requested width of the image (in px)
| `height`      | X        | Requested height of the image (in px)

Width and height have to be specified always together. If the asset does not comply to this with it is tried to generated a virtual rendition by resizing it. This is only possible if the width/height ratio matches and the asset is not smaller than the requested size.

If multiple suffix parameter sets are specified multiple assets are resolved and returned as array.


### Data version

To get the data version for all assets use an URL with this syntax:

```
<cms-hostname><asset_root_path>.wcm-io-asset-service-dataversion.json
```

The `asset_root_path` is by default /content/dam. If you specified specific sub paths in the [configuration][configuration] you have to use one of those paths to get the matching data version. The strategy how data versions are generated can be configured there as well.

After the asset root path this suffix `.wcm-io-asset-service-dataversion.json` has to be added. This selector suffix can be changed in the [configuration][configuration].


### Examples

#### Download asset

URL: `http://localhost:4503/content/dam/sample/sample.pdf.wcm-io-asset-service.json`

Response:

```json
[
  {
    "assetPath": "/content/dam/sample/sample.pdf",
    "url": "/content/dam/sample/sample.pdf/_jcr_content/renditions/original./sample.pdf",
    "fileSize": 105990,
    "fileExtension": "pdf",
    "mimeType": "application/pdf"
  }
]
```

#### Image Asset with multiple sizes

URL: `http://localhost:4503/content/dam/sample/myteaser.jpg.wcm-io-asset-service.json/width=273,height=154/width=546,height=307.json`

Response:

```json
[
  {
    "assetPath": "/content/dam/sample/myteaser.jpg",
    "url": "/content/dam/sample/myteaser.jpg/_jcr_content/renditions/myteaser.jpg.image_file.273.154.file/myteaser.jpg",
    "width": 273,
    "height": 154,
    "fileExtension": "jpg",
    "mimeType": "image/jpeg"
  },
  {
    "assetPath": "/content/dam/sample/myteaser.jpg",
    "url": "/content/dam/sample/myteaser.jpg/_jcr_content/renditions/original./myteaser.jpg",
    "width": 546,
    "height": 307,
    "fileSize": 89479,
    "fileExtension": "jpg",
    "mimeType": "image/jpeg"
  }
]
```


#### Get data version

URL: `http://localhost:4503/content/dam.wcm-io-asset-service-dataversion.json`

Response:

```json
{
  "dataVersion":"2015-08-14T16:50:19.997+02:00"
}
```


[configuration]: configuration.html



### Deprecated REST Interface syntax

DAM asset service up to version 1.1.0 supported only an old-style REST interface using URL parameters instead of suffix-encoded parameters. This syntax is still supported, but is deprecated because it is not dispatcher-friendly.

Example URL: `http://localhost:4503/content/dam/sample/myteaser.jpg.wcm-io-asset-service.json?width=273&height=154&width=546&height=307`
