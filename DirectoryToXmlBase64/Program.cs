using System.Globalization;
using System.Xml;

if (args.Length < 2)
{
    Console.Error.WriteLine("Usage: DirectoryToXmlBase64 <source-directory> <output-xml>");
    return 1;
}

var sourceDirectory = Path.GetFullPath(args[0]);
var outputXmlPath = Path.GetFullPath(args[1]);

if (!Directory.Exists(sourceDirectory))
{
    Console.Error.WriteLine($"Source directory does not exist: {sourceDirectory}");
    return 1;
}

var snapshot = DirectorySnapshotBuilder.Build(sourceDirectory, outputXmlPath);
DirectorySnapshotWriter.Write(snapshot, sourceDirectory, outputXmlPath);

Console.WriteLine($"XML snapshot written to: {outputXmlPath}");
return 0;

internal static class DirectorySnapshotBuilder
{
    public static DirectoryNode Build(string sourceDirectory, string outputXmlPath)
    {
        return BuildDirectory(sourceDirectory, sourceDirectory, outputXmlPath);
    }

    private static DirectoryNode BuildDirectory(string directoryPath, string rootPath, string outputXmlPath)
    {
        var fullDirectoryPath = Path.GetFullPath(directoryPath);
        var normalizedDirectoryPath = fullDirectoryPath.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
        var directoryName = Path.GetFileName(normalizedDirectoryPath);

        var directory = new DirectoryNode
        {
            Name = string.IsNullOrEmpty(directoryName)
                ? Path.GetPathRoot(fullDirectoryPath) ?? fullDirectoryPath
                : directoryName,
            RelativePath = GetRelativePath(rootPath, directoryPath)
        };

        try
        {
            foreach (var childDirectoryPath in Directory.EnumerateDirectories(directoryPath).OrderBy(path => path, StringComparer.OrdinalIgnoreCase))
            {
                directory.Directories.Add(BuildDirectory(childDirectoryPath, rootPath, outputXmlPath));
            }
        }
        catch (Exception ex)
        {
            directory.Errors.Add($"Could not enumerate sub-directories: {ex.Message}");
        }

        try
        {
            foreach (var filePath in Directory.EnumerateFiles(directoryPath).OrderBy(path => path, StringComparer.OrdinalIgnoreCase))
            {
                if (PathsEqual(filePath, outputXmlPath))
                {
                    continue;
                }

                directory.Files.Add(BuildFile(filePath, rootPath));
            }
        }
        catch (Exception ex)
        {
            directory.Errors.Add($"Could not enumerate files: {ex.Message}");
        }

        return directory;
    }

    private static FileNode BuildFile(string filePath, string rootPath)
    {
        var fileInfo = new FileInfo(filePath);
        var file = new FileNode
        {
            Name = fileInfo.Name,
            RelativePath = GetRelativePath(rootPath, filePath),
            Size = fileInfo.Exists ? fileInfo.Length : 0
        };

        try
        {
            var bytes = File.ReadAllBytes(filePath);
            file.Base64Content = Convert.ToBase64String(bytes);
        }
        catch (Exception ex)
        {
            file.Error = $"Could not read file: {ex.Message}";
        }

        return file;
    }

    private static string GetRelativePath(string rootPath, string path)
    {
        var relativePath = Path.GetRelativePath(rootPath, path);
        return string.IsNullOrEmpty(relativePath) || relativePath == "."
            ? "."
            : relativePath.Replace(Path.DirectorySeparatorChar, '/');
    }

    private static bool PathsEqual(string left, string right)
    {
        return string.Equals(
            Path.GetFullPath(left),
            Path.GetFullPath(right),
            OperatingSystem.IsWindows() ? StringComparison.OrdinalIgnoreCase : StringComparison.Ordinal);
    }
}

internal static class DirectorySnapshotWriter
{
    public static void Write(DirectoryNode rootDirectory, string sourceDirectory, string outputXmlPath)
    {
        var outputDirectory = Path.GetDirectoryName(outputXmlPath);
        if (!string.IsNullOrWhiteSpace(outputDirectory))
        {
            Directory.CreateDirectory(outputDirectory);
        }

        var settings = new XmlWriterSettings
        {
            Indent = true,
            Encoding = new System.Text.UTF8Encoding(encoderShouldEmitUTF8Identifier: false)
        };

        using var writer = XmlWriter.Create(outputXmlPath, settings);
        writer.WriteStartDocument();
        writer.WriteStartElement("DirectorySnapshot");
        writer.WriteAttributeString("sourcePath", sourceDirectory);
        writer.WriteAttributeString("generatedUtc", DateTime.UtcNow.ToString("O", CultureInfo.InvariantCulture));
        WriteDirectory(writer, rootDirectory);
        writer.WriteEndElement();
        writer.WriteEndDocument();
    }

    private static void WriteDirectory(XmlWriter writer, DirectoryNode directory)
    {
        writer.WriteStartElement("Directory");
        writer.WriteAttributeString("name", directory.Name);
        writer.WriteAttributeString("relativePath", directory.RelativePath);

        foreach (var error in directory.Errors)
        {
            writer.WriteElementString("Error", error);
        }

        foreach (var file in directory.Files)
        {
            writer.WriteStartElement("File");
            writer.WriteAttributeString("name", file.Name);
            writer.WriteAttributeString("relativePath", file.RelativePath);
            writer.WriteAttributeString("size", file.Size.ToString(CultureInfo.InvariantCulture));

            if (!string.IsNullOrWhiteSpace(file.Error))
            {
                writer.WriteElementString("Error", file.Error);
            }
            else
            {
                writer.WriteElementString("Base64Content", file.Base64Content);
            }

            writer.WriteEndElement();
        }

        foreach (var childDirectory in directory.Directories)
        {
            WriteDirectory(writer, childDirectory);
        }

        writer.WriteEndElement();
    }
}

internal sealed class DirectoryNode
{
    public string Name { get; init; } = string.Empty;

    public string RelativePath { get; init; } = ".";

    public List<DirectoryNode> Directories { get; } = [];

    public List<FileNode> Files { get; } = [];

    public List<string> Errors { get; } = [];
}

internal sealed class FileNode
{
    public string Name { get; init; } = string.Empty;

    public string RelativePath { get; init; } = string.Empty;

    public long Size { get; init; }

    public string Base64Content { get; set; } = string.Empty;

    public string? Error { get; set; }
}
