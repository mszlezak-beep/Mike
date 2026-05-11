# PDFBox PDF Tools

Java example code for composing PDFs with Apache PDFBox:

- merge multiple PDF files in order
- append new letter-sized pages
- write a header such as `Header test`
- draw images supplied as plain Base64 or `data:image/...;base64,...`

## Usage

```java
PdfBoxDocumentComposer composer = new PdfBoxDocumentComposer();

List<Path> sources = List.of(
        Path.of("first.pdf"),
        Path.of("second.pdf"));

PdfBoxDocumentComposer.NewPage page = new PdfBoxDocumentComposer.NewPage(
        "Header test",
        List.of(new PdfBoxDocumentComposer.Base64Image(
                base64Png,
                72.0F,   // x, from left
                500.0F,  // y, from bottom
                160.0F,  // width
                90.0F))); // height

composer.mergeAndAppendPages(sources, Path.of("output.pdf"), List.of(page));
```

Run tests with:

```bash
mvn test
```
