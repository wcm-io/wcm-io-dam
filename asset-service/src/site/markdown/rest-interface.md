## Asset Service REST Interface


### Resolve assets

To get the asset metadata for a certain asset path use an URL with this synatx:

```
<cms-hostname><asset_path>.wcm-io-asset-service.json?<url-params>
```

After the asset path the suffix `.wcm-io-asset-service.json` has to be added. This selector suffix can be changed in the [configuration][configuration].

The URL parameters are optional. Supported parameters:

| URL parameter | Multiple | Description
|---------------|:--------:|-------------
| `mediaFormat` | X        | Specifies internal media format name of the CMS application
| `width`       | X        | Requested width of the image (in px)
| `height`      | X        | Requested height of the image (in px)

Width and height have to be specified always together. If the asset does not comply to this with it is tried to generated a virtual rendition by resizing it. This is only possible if the width/height ratio matches and the asset is not smaller than the requested size.

The URL parameters can be specified multiple times. In this case multiple assets are resolved and returned as array.


### Data version

To get the data version for all assets use an URL with this synatx:

```
<cms-hostname><asset_root_path>.wcm-io-asset-service-dataversion.json?<url-params>
```

The `asset_root_path` is by default /content/dam. If you specified specific subpaths in the [configuration][configuration] you have to use on of those paths to get the matching data version.

After the asset root path this suffix `.wcm-io-asset-service-dataversion.json` has to be added. This selector suffix can be changed in the [configuration][configuration].

Currently a timestamp is returned as data version reflecting the latest DAM event. But you should not rely on this and take it just as an arbitrary string. You should not compare data versions, it is not guaranteed that the lexical ordering of two data versions matches with the temporal order.


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
    "fileExtension": "pdf"
  }
]
```

#### Image Asset with multiple sizes

URL: `http://localhost:4503/content/dam/sample/myteaser.jpg.wcm-io-asset-service.json?width=273&height=154&width=546&height=307`

Response:

```json
[
  {
    "assetPath": "/content/dam/sample/myteaser.jpg",
    "url": "/content/dam/sample/myteaser.jpg/_jcr_content/renditions/myteaser.jpg.image_file.273.154.file/myteaser.jpg",
    "width": 273,
    "height": 154,
    "fileExtension": "jpg"
  },
  {
    "assetPath": "/content/dam/sample/myteaser.jpg",
    "url": "/content/dam/sample/myteaser.jpg/_jcr_content/renditions/original./myteaser.jpg",
    "width": 546,
    "height": 307,
    "fileSize": 89479,
    "fileExtension": "jpg"
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
