# Mike

## DirectoryToXmlBase64

This repository now includes a small C# console application that:

- walks a directory and all of its sub-directories
- reads every file it finds
- converts file contents to Base64
- writes the full directory snapshot to an XML file

### Project location

`DirectoryToXmlBase64/`

### Usage

After installing the .NET 8 SDK, run:

```bash
dotnet run --project DirectoryToXmlBase64 -- "/path/to/input-directory" "/path/to/output.xml"
```

### XML structure

The generated XML contains nested `<Directory>` elements and `<File>` elements. Each file node includes:

- `name`
- `relativePath`
- `size`
- `<Base64Content>` with the file data encoded as Base64

If a file or directory cannot be read, an `<Error>` element is written instead.
