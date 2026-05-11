# PDFBox PDF Tools

Java example code for composing PDFs with Apache PDFBox:

- merge multiple PDF files in order
- merge multiple Base64 PDF documents in order
- append new letter-sized pages
- write a header such as `Header test`
- draw images supplied as plain Base64 or `data:image/...;base64,...`

## Usage

```java
import com.example.pdf.Base64Image;
import com.example.pdf.NewPage;
import com.example.pdf.PdfBoxDocumentComposer;

PdfBoxDocumentComposer composer = new PdfBoxDocumentComposer();

List<Path> sources = List.of(
        Path.of("first.pdf"),
        Path.of("second.pdf"));

NewPage page = new NewPage(
        "Header test",
        List.of(new Base64Image(
                base64Png,
                72.0F,   // x, from left
                500.0F,  // y, from bottom
                160.0F,  // width
                90.0F))); // height

composer.mergeAndAppendPages(sources, Path.of("output.pdf"), List.of(page));
```

To merge PDFs that are already Base64 strings:

```java
List<String> sourcePdfBase64 = List.of(
        firstPdfBase64,
        "data:application/pdf;base64," + secondPdfBase64);

byte[] mergedPdf = composer.mergeBase64PdfsAndAppendPages(
        sourcePdfBase64,
        List.of(page));

String mergedPdfBase64 = composer.mergeBase64PdfsAndAppendPagesAsBase64(
        sourcePdfBase64,
        List.of(page));
```

Run tests with:

```bash
mvn test
```
