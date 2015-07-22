## Asset Service REST Interface


### URL Syntax

```
<cms-hostname><asset_path>.wcm-io-asset-service.json?<url-params>
```

The asset path does not contain a CMS host name, this has to be added.

After the asset path this suffix `.wcm-io-asset-service.json` has to be added. This selector suffix can be changed in the OSGi configuration of the service "wcm.io DAM Asset Service".

The URL parameters are optional. Supported parameters:

| URL parameter | Multiple | Description
|---------------|:--------:|-------------
| `mediaFormat` | X        | Specifies internal media format name of the CMS application
| `width`       | X        | Requested width of the image (in px)
| `height`      | X        | Requested height of the image (in px)

Width and height have to be specified always together. If the asset does not comply to this with it is tried to generated a virtual rendition by resizing it. This is only possible if the width/height ratio matches and the asset is not smaller than the requested size.

The URL parameters can be specified multiple times. In this case multiple assets are resolved and returned as array.


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
