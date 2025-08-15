# Path

![Maven Central](https://img.shields.io/maven-central/v/io.github.edadma/path_sjs1_3)
[![Last Commit](https://img.shields.io/github/last-commit/edadma/path)](https://github.com/edadma/path/commits)
![GitHub](https://img.shields.io/github/license/edadma/path)
![Scala Version](https://img.shields.io/badge/Scala-3.7.2-blue.svg)
![ScalaJS Version](https://img.shields.io/badge/Scala.js-1.19.0-blue.svg)
![Scala Native Version](https://img.shields.io/badge/Scala_Native-0.5.8-blue.svg)

A clean, modern file system library for Scala that works consistently across JVM, Scala.js, and Scala Native.

## Why Path?

Java's file operations are verbose and clunky:
```scala
// Java way - verbose and error-prone
val configPath = Paths.get("config").resolve("app.conf")
if (Files.exists(configPath)) {
  val content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8)
}

// Path way - clean and fluent
val configPath = Path("config") / "app.conf"
if (configPath.exists()) {
  val content = configPath.readText()
}
```

**Path gives you:**
- **Fluent API**: Chain operations naturally with `/`
- **Cross-platform**: Same code works on JVM, JS, and Native
- **Method-based**: File operations as methods, not static functions
- **Rich path manipulation**: Extensions, subpaths, absolute conversion
- **Complete metadata**: Permissions, file types, comparisons
- **Glob support**: Built-in pattern matching for file listing
- **Type safety**: Case class benefits (equality, hashing, pattern matching)

## Quick Start

```scala
import io.github.edadma.path._

// Create paths naturally
val projectDir = Path("src") / "main" / "scala"
val configFile = Path("/etc") / "myapp" / "config.json"

// File operations as methods (the way it should be!)
if (configFile.exists()) {
  val config = configFile.readText()
  println(s"Config: $config")
}

// Rich path manipulation
val scalaFile = Path("MyClass.scala")
println(s"Extension: ${scalaFile.extension}")           // .scala
println(s"Name: ${scalaFile.nameWithoutExtension}")     // MyClass
val javaFile = scalaFile.withExtension("java")          // MyClass.java

// Convert to absolute paths
val absolutePath = projectDir.toAbsolutePath()
println(s"Absolute: $absolutePath")

// Create directory structure
val buildDir = Path("target") / "classes"
buildDir.createDirectories()

// List files with patterns
val scalaFiles = projectDir.listDirectory("*.scala")
scalaFiles.foreach { entry =>
  println(s"Found: ${entry.name}")
}

// Check file permissions and metadata
if (configFile.isReadable() && !configFile.isEmpty()) {
  println("Config file is readable and not empty")
}
```

## Core Features

### Path Construction and Manipulation
```scala
// Path construction and combination
val base = Path("/home/user")
val docs = base / "documents" / "projects"

// Rich path manipulation
val sourceFile = Path("/projects/myapp/src/main/scala/MyClass.scala")

// File extensions
println(sourceFile.extension)              // .scala
println(sourceFile.nameWithoutExtension)   // MyClass
val testFile = sourceFile.withExtension("test.scala")  // MyClass.test.scala

// Path segments and subpaths
println(sourceFile.subpath(2, 5))          // src/main/scala
println(sourceFile.startsWith(Path("/projects")))  // true
println(sourceFile.endsWith(Path("MyClass.scala"))) // true

// Convert relative to absolute
val relPath = Path("src/main/scala")
val absPath = relPath.toAbsolutePath()     // /current/working/dir/src/main/scala

// Relative paths between locations
val targetDir = Path("/home/user/projects/myapp/target")
val relative = targetDir.relativeTo(base)  // projects/myapp/target

// Path normalization
val messy = Path("src/../config/./app.conf")
val clean = messy.normalize                 // config/app.conf

// Parent and filename
val file = Path("/home/user/document.pdf")
println(file.parent)    // Some(/home/user)
println(file.filename)  // document.pdf
```

### File System Metadata
```scala
val file = Path("important-document.pdf")

// Existence and type checking
println(s"Exists: ${file.exists()}")
println(s"Is file: ${file.isFile()}")
println(s"Is directory: ${file.isDirectory()}")
println(s"Is symbolic link: ${file.isSymbolicLink()}")

// Permission checking
println(s"Readable: ${file.isReadable()}")
println(s"Writable: ${file.isWritable()}")
println(s"Executable: ${file.isExecutable()}")

// File properties
println(s"Size: ${file.size()} bytes")
println(s"Modified: ${file.lastModified()}")
println(s"Empty: ${file.isEmpty()}")

// Compare files
val backup = Path("document-backup.pdf")
if (file.isSameFile(backup)) {
  println("Files are identical (same inode/reference)")
}
```

### File Operations
```scala
val file = Path("data.txt")

// Text files
file.writeText("Hello, World!")
val content = file.readText()

// Binary files  
val data = Array[Byte](1, 2, 3, 4)
file.writeBytes(data)
val bytes = file.readBytes()

// Check if file/directory is empty
if (file.isEmpty()) {
  println("File has no content")
}
```

### Directory Operations
```scala
val dir = Path("build")

// Create directories
dir.createDirectories()  // Creates parents too

// List contents with filtering
val allFiles = dir.listDirectory()
val jsonFiles = dir.listDirectory("*.json")
val appFiles = dir.listDirectory("app*")

allFiles.foreach { entry =>
  val typeStr = entry.fileType match {
    case FileType.File => "FILE"
    case FileType.Directory => "DIR"
    case FileType.SymbolicLink => "LINK"
    case FileType.Other => "OTHER"
  }
  println(s"$typeStr: ${entry.name}")
}

// Check if directory is empty
if (dir.isEmpty()) {
  println("Directory contains no files")
}
```

### File Management
```scala
val source = Path("document.pdf")
val backup = Path("backup") / "document.pdf"
val archive = Path("archive") / "document.pdf"

// Copy and move operations
source.copyTo(backup)    // Copy to backup location
source.moveTo(archive)   // Move to archive

// Cleanup
backup.delete()
```

## Advanced Path Operations

### Working with Extensions
```scala
// Handle various file types
val files = Vector(
  Path("README.md"),
  Path("app.conf"),
  Path("data.json"),
  Path("script.sh"),
  Path("archive.tar.gz")
)

files.foreach { file =>
  println(s"File: ${file.filename}")
  println(s"  Extension: '${file.extension}'")
  println(s"  Without ext: '${file.nameWithoutExtension}'")
  println(s"  As backup: ${file.withExtension(file.extension + ".bak")}")
  println()
}
```

### Path Hierarchy Navigation
```scala
val projectRoot = Path("/projects/myapp")
val sourceDir = projectRoot / "src" / "main" / "scala"
val testDir = projectRoot / "src" / "test" / "scala"

// Check relationships
println(sourceDir.startsWith(projectRoot))  // true
println(sourceDir.endsWith(Path("scala")))  // true

// Get relative paths between directories
val relativeToTest = sourceDir.relativeTo(testDir)
println(relativeToTest)  // ../../main/scala

// Extract specific path segments
val pathSegments = sourceDir.subpath(1, 4)  // projects/myapp/src
```

### File System Analysis
```scala
def analyzeDirectory(dir: Path): Unit = {
  if (!dir.exists() || !dir.isDirectory()) {
    println(s"$dir is not a valid directory")
    return
  }
  
  val entries = dir.listDirectory()
  val (files, dirs, links) = entries.partition(_.fileType).fold(
    (Vector.empty, Vector.empty, Vector.empty)
  ) { case ((f, d, l), entry) =>
    entry.fileType match {
      case FileType.File => (f :+ entry, d, l)
      case FileType.Directory => (f, d :+ entry, l)
      case FileType.SymbolicLink => (f, d, l :+ entry)
      case FileType.Other => (f, d, l)
    }
  }
  
  println(s"Analysis of $dir:")
  println(s"  Files: ${files.length}")
  println(s"  Directories: ${dirs.length}")
  println(s"  Symbolic links: ${links.length}")
  println(s"  Empty: ${dir.isEmpty()}")
  
  // Find largest files
  val fileSizes = files.map { entry =>
    val filePath = dir / entry.name
    (entry.name, filePath.size())
  }.sortBy(-_._2)
  
  println("  Largest files:")
  fileSizes.take(3).foreach { case (name, size) =>
    println(s"    $name: $size bytes")
  }
}
```

## Cross-Platform Architecture

Path provides a unified API while using the best platform-specific implementations:

- **JVM**: Uses `java.nio.files` for robust, high-performance file operations
- **Scala.js**: Uses Node.js `fs` and `path` modules for full file system access
- **Scala Native**: Uses Java NIO compatibility layer for native performance

The same `Path` code compiles and runs identically across all platforms.

## Installation

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.edadma" %% "path" % "0.0.2"
```

For cross-platform projects:
```scala
// shared/src/main/scala - your cross-platform code using Path
// jvm/src/main/scala    - JVM-specific implementations  
// js/src/main/scala     - JS-specific implementations
// native/src/main/scala - Native-specific implementations
```

## Examples

### Configuration Management
```scala
val configDir = Path(System.getProperty("user.home")) / ".myapp"
configDir.createDirectories()

val configFile = configDir / "config.json"
if (!configFile.exists()) {
  configFile.writeText("""{"theme": "dark", "autoSave": true}""")
}

// Validate config file
if (configFile.isReadable() && !configFile.isEmpty()) {
  val config = configFile.readText()
  println(s"Loaded config: ${config.length} characters")
}
```

### Build Tool Integration
```scala
val sourceDir = Path("src") / "main" / "scala"
val targetDir = Path("target") / "classes"

// Find all Scala source files
val scalaFiles = sourceDir.listDirectory("*.scala")
println(s"Found ${scalaFiles.length} Scala files")

// Analyze source files
scalaFiles.foreach { entry =>
  val sourcePath = sourceDir / entry.name
  println(s"${sourcePath.filename}: ${sourcePath.size()} bytes")
  
  // Convert to target path
  val targetPath = targetDir / sourcePath.nameWithoutExtension.withExtension("class")
  println(s"  → ${targetPath}")
}

// Compile to target directory
targetDir.createDirectories()
```

### Log File Management
```scala
val logDir = Path("logs")
logDir.createDirectories()

// Find and archive old logs
val logFiles = logDir.listDirectory("*.log")
val archiveDir = logDir / "archive"

if (logFiles.nonEmpty) {
  archiveDir.createDirectories()
  
  logFiles.foreach { entry =>
    val logFile = logDir / entry.name
    
    // Check if log file should be archived (e.g., older than 30 days)
    val ageMs = System.currentTimeMillis() - logFile.lastModified()
    val ageDays = ageMs / (1000 * 60 * 60 * 24)
    
    if (ageDays > 30) {
      val archiveFile = archiveDir / entry.name
      println(s"Archiving ${logFile.filename} (${ageDays} days old)")
      logFile.moveTo(archiveFile)
    }
  }
}

// Clean up empty directories
if (logDir.isEmpty()) {
  println("Log directory is empty")
}
```

### File System Utilities
```scala
def findLargestFiles(dir: Path, count: Int = 10): Vector[(Path, Long)] = {
  if (!dir.exists() || !dir.isDirectory()) {
    return Vector.empty
  }
  
  val allFiles = dir.listDirectory()
    .filter(_.fileType == FileType.File)
    .map(entry => dir / entry.name)
    .filter(_.isReadable())
  
  allFiles
    .map(file => (file, file.size()))
    .sortBy(-_._2)
    .take(count)
}

def findFilesByExtension(dir: Path, extension: String): Vector[Path] = {
  val pattern = if (extension.startsWith(".")) s"*$extension" else s"*.$extension"
  
  dir.listDirectory(pattern)
    .map(entry => dir / entry.name)
    .filter(_.isFile())
}

// Usage
val projectDir = Path(".")
val largestFiles = findLargestFiles(projectDir)
val scalaFiles = findFilesByExtension(projectDir, ".scala")

println("Largest files:")
largestFiles.foreach { case (file, size) =>
  println(s"  ${file.filename}: $size bytes")
}

println(s"\nFound ${scalaFiles.length} Scala files")
```

## Testing

Path includes comprehensive tests covering:
- Path construction and manipulation
- File and directory operations
- Extension handling and path conversion
- Permission and metadata checking
- Cross-platform compatibility
- Package manager scenarios

Run tests with:
```bash
sbt test
```

## Contributing

This library was built to solve real-world file system challenges in Scala. Contributions are welcome!

**Upcoming features:**
- Watch APIs for file system monitoring
- Streaming operations for large files
- Advanced glob patterns with recursion
- File system event notifications

## License

ISC License - see LICENSE file for details.

---

*Finally, a file system library that doesn't make you hate working with files in Scala.*