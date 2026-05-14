# Mike

Minimal Maven project containing a Java utility that converts a HEIC/HEIF image provided as a
base64 string into a PDFBox 2.x-compatible image base64 string.

## Utility

`com.mike.pdfbox.HeicBase64ImageConverter`

```java
String pngBase64 = HeicBase64ImageConverter.convertHeicBase64ToPdfBoxImageBase64(heicBase64);
String jpegBase64 = HeicBase64ImageConverter.convertHeicBase64ToPdfBoxImageBase64(heicBase64, "jpeg");
String tiffBase64 = HeicBase64ImageConverter.convertHeicBase64ToPdfBoxImageBase64(heicBase64, "tiff");
```

Supported target formats:

- `jpg` / `jpeg`
- `png`
- `tif` / `tiff`
- `gif`
- `bmp`

The returned string is raw base64 without a data URI prefix. Input may be raw base64 or a full
data URI such as `data:image/heic;base64,...`.

## Runtime requirements

The HEIC decoder uses `com.github.gotson.nightmonkeys:imageio-heif`, which requires:

- Java 21
- JVM flags: `--enable-preview --enable-native-access=ALL-UNNAMED`
- `libheif` installed on the host system

The tests in this repository already set the required JVM flags through Maven Surefire.
