# Path - Cross-Platform File System Operations for Scala

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

// Create directory structure
val buildDir = Path("target") / "classes"
buildDir.createDirectories()

// List files with patterns
val scalaFiles = projectDir.listDirectory("*.scala")
scalaFiles.foreach { entry =>
  println(s"Found: ${entry.name}")
}

// Relative path operations
val relative = configFile.relativeTo(Path("/etc"))
println(relative)  // myapp/config.json
```

## Core Features

### Path Operations
```scala
// Path construction and combination
val base = Path("/home/user")
val docs = base / "documents" / "projects"

// Relative paths between locations
val targetDir = Path("/home/user/projects/myapp/target")
val relative = targetDir.relativeTo(base)  // projects/myapp/target

// Path normalization
val messy = Path("src/../config/./app.conf")
val clean = messy.normalize  // config/app.conf

// Parent and filename
val file = Path("/home/user/document.pdf")
println(file.parent)    // Some(/home/user)
println(file.filename)  // document.pdf
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

// File metadata
println(s"Size: ${file.size()} bytes")
println(s"Modified: ${file.lastModified()}")
println(s"Exists: ${file.exists()}")
println(s"Is file: ${file.isFile()}")
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

## Perfect for Package Managers

Path was designed with package management in mind:

```scala
// Package installation structure
val packagesDir = Path("packages")
val myPackage = packagesDir / "lodash" / "4.17.21"
val binDir = myPackage / "bin"

// Create package structure
binDir.createDirectories()

// Install package files
val packageJson = myPackage / "package.json"
packageJson.writeText("""{"name": "lodash", "version": "4.17.21"}""")

// Find all JavaScript files
val jsFiles = myPackage.listDirectory("*.js")

// Resolve dependencies between packages
val utilsPackage = packagesDir / "utils" / "1.0.0"
val relativePath = utilsPackage.relativeTo(myPackage)
// Result: ../../utils/1.0.0
```

## Cross-Platform Architecture

Path provides a unified API while using the best platform-specific implementations:

- **JVM**: Uses `java.nio.files` for robust, high-performance file operations
- **Scala.js**: Will use Node.js `fs` and `path` modules (coming soon)
- **Scala Native**: Will use direct system calls (coming soon)

The same `Path` code compiles and runs identically across all platforms.

## Installation

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.edadma" %% "path" % "0.1.0"
```

For cross-platform projects:
```scala
// shared/src/main/scala - your cross-platform code using Path
// jvm/src/main/scala    - JVM-specific implementations  
// js/src/main/scala     - JS-specific implementations (coming soon)
// native/src/main/scala - Native-specific implementations (coming soon)
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

val config = configFile.readText()
```

### Build Tool Integration
```scala
val sourceDir = Path("src") / "main" / "scala"
val targetDir = Path("target") / "classes"

// Find all Scala source files
val scalaFiles = sourceDir.listDirectory("*.scala")

// Compile to target directory
targetDir.createDirectories()
scalaFiles.foreach { entry =>
  val sourcePath = sourceDir / entry.name
  println(s"Compiling: ${sourcePath}")
  // ... compilation logic
}
```

### Log File Management
```scala
val logDir = Path("logs")
logDir.createDirectories()

// Archive old logs
val oldLogs = logDir.listDirectory("*.log")
val archiveDir = logDir / "archive"
archiveDir.createDirectories()

oldLogs.foreach { entry =>
  val logFile = logDir / entry.name
  val archiveFile = archiveDir / entry.name
  logFile.moveTo(archiveFile)
}
```

## Testing

Path includes comprehensive tests covering:
- Path construction and manipulation
- File and directory operations
- Glob pattern matching
- Package manager scenarios
- Cross-platform compatibility

Run tests with:
```bash
sbt test
```

## Contributing

This library was built to solve real-world file system challenges in Scala. Contributions are welcome!

**Upcoming features:**
- Scala.js implementation using Node.js APIs
- Scala Native implementation using system calls
- Streaming operations for large files
- Watch APIs for file system monitoring

## License

MIT License - see LICENSE file for details.

---

*Finally, a file system library that doesn't make you hate working with files in Scala.*a