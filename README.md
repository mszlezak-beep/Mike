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

The HEIC decode step uses the `heif-convert` CLI from libheif and then re-encodes the image in
Java to a PDFBox 2.x-compatible format.

Required runtime pieces:

- Java 21
- `heif-convert` available on `PATH`
- On Ubuntu, install it with `sudo apt-get install libheif-examples`
