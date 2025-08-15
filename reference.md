# Path Library - Complete API Documentation

## Overview

The Path library provides a cross-platform file system API for Scala that works identically on JVM, Scala.js, and Scala Native. It offers a fluent, type-safe interface for path manipulation and file operations.

## Core Types

### Path Case Class
```scala
case class Path(segments: Vector[String], isAbsolute: Boolean)
```

**Properties:**
- `segments: Vector[String]` - Path components (directories/filename)
- `isAbsolute: Boolean` - Whether path is absolute or relative

**Benefits of case class:**
- Structural equality (`==` works correctly)
- Immutability (all operations return new instances)
- Pattern matching support
- Built-in `hashCode` and `toString`

### DirectoryEntry
```scala
case class DirectoryEntry(name: String, fileType: FileType)
```

**Properties:**
- `name: String` - File or directory name
- `fileType: FileType` - Type of file system entry

### FileType Enumeration
```scala
sealed trait FileType
object FileType {
  case object File         extends FileType  // Regular file
  case object Directory    extends FileType  // Directory/folder
  case object SymbolicLink extends FileType  // Symbolic link
  case object Other        extends FileType  // Device files, pipes, etc.
}
```

## Path Construction

### Primary Constructor
```scala
Path(pathString: String): Path
```
**Purpose:** Create Path from string representation  
**Examples:**
```scala
Path("/home/user")           // Absolute path
Path("src/main/scala")       // Relative path  
Path("file.txt")             // Single file
Path("")                     // Empty path
Path("a//b///c")             // Normalizes multiple slashes
```

### Case Class Constructor
```scala
Path(segments: Vector[String], isAbsolute: Boolean): Path
```
**Purpose:** Direct construction with segments  
**Examples:**
```scala
Path(Vector("home", "user"), isAbsolute = true)   // /home/user
Path(Vector("src", "main"), isAbsolute = false)   // src/main
Path(Vector.empty, isAbsolute = true)             // /
```

## Path Operations (Pure - No I/O)

### Path Combination
```scala
def /(other: String): Path
def /(other: Path): Path
```
**Purpose:** Combine paths using `/` operator  
**Behavior:** If `other` is absolute, it replaces current path  
**Examples:**
```scala
Path("home") / "user"                    // home/user
Path("home") / Path("user/docs")         // home/user/docs
Path("home") / Path("/etc")              // /etc (absolute replaces)
```

### Relative Path Calculation
```scala
def relativeTo(base: Path): Path
```
**Purpose:** Calculate relative path from base to this path  
**Requirements:** Both paths must have same absolute/relative nature  
**Examples:**
```scala
Path("/a/b/c").relativeTo(Path("/a"))         // b/c
Path("/a/file").relativeTo(Path("/a/b/c"))    // ../../file
Path("a/b").relativeTo(Path("a"))             // b
```

### Path Hierarchy
```scala
def parent: Option[Path]
def filename: String
```
**Purpose:** Navigate path hierarchy  
**Examples:**
```scala
Path("/home/user/file.txt").parent    // Some(/home/user)
Path("/").parent                      // None
Path("file.txt").parent               // Some("")
Path("/home/user/file.txt").filename  // "file.txt"
Path("/home/user").filename           // "user"
Path("").filename                     // ""
```

### File Extensions
```scala
def extension: String
def nameWithoutExtension: String  
def withExtension(ext: String): Path
```
**Purpose:** Handle file extensions  
**Extension rules:** Must have dot and content after dot (not at start/end)  
**Examples:**
```scala
Path("file.txt").extension                    // ".txt"
Path("archive.tar.gz").extension              // ".gz"
Path("README").extension                      // ""
Path(".gitignore").extension                  // ""
Path("file.").extension                       // ""

Path("file.txt").nameWithoutExtension         // "file"
Path("archive.tar.gz").nameWithoutExtension   // "archive.tar"
Path("README").nameWithoutExtension           // "README"

Path("file.txt").withExtension("json")        // file.json
Path("file.txt").withExtension(".json")       // file.json (dot optional)
Path("README").withExtension("md")            // README.md
```

### Path Relationships
```scala
def startsWith(other: Path): Boolean
def endsWith(other: Path): Boolean
```
**Purpose:** Check path relationships  
**Behavior:** Compares segments, respects absolute/relative nature  
**Examples:**
```scala
Path("/a/b/c").startsWith(Path("/a"))         // true
Path("/a/b/c").startsWith(Path("a"))          // false (different types)
Path("a/b/c").endsWith(Path("c"))             // true
Path("a/b/c").endsWith(Path("b/c"))           // true
```

### Path Segments
```scala
def subpath(start: Int, end: Int): Path
```
**Purpose:** Extract portion of path segments  
**Behavior:** Returns relative path with segments[start:end]  
**Validation:** Throws `IllegalArgumentException` for invalid ranges  
**Examples:**
```scala
Path("a/b/c/d").subpath(1, 3)    // b/c
Path("a/b/c/d").subpath(0, 4)    // a/b/c/d
Path("a/b/c/d").subpath(2, 2)    // "" (empty)
```

### Path Conversion
```scala
def toAbsolutePath: Path
def normalize: Path
def toString: String
def toPlatformString: String
```
**Purpose:** Convert and normalize paths  
**Examples:**
```scala
// toAbsolutePath (requires getCurrentDirectory from cross-platform)
Path("src/main").toAbsolutePath             // /current/dir/src/main
Path("/abs/path").toAbsolutePath            // /abs/path (unchanged)

// normalize - resolve . and ..
Path("a/./b").normalize                       // a/b
Path("a/../b").normalize                      // b  
Path("a/b/../c").normalize                    // a/c
Path("../a").normalize                        // ../a (preserved)

// toString - always uses / separator
Path("/home/user").toString                   // "/home/user"
Path("src/main").toString                     // "src/main"

// toPlatformString - uses platform separator
Path("/home/user").toPlatformString           // "/home/user" (Unix) or "\\home\\user" (Windows)
```

## File System Metadata (I/O Operations)

### Existence and Type Checking
```scala
def exists: Boolean
def isFile: Boolean
def isDirectory: Boolean
def isSymbolicLink: Boolean
```
**Purpose:** Check file system entry existence and type  
**Examples:**
```scala
Path("file.txt").exists         // true if exists
Path("file.txt").isFile         // true if regular file
Path("dir").isDirectory         // true if directory
Path("link").isSymbolicLink     // true if symbolic link
```

### Permission Checking
```scala
def isReadable: Boolean
def isWritable: Boolean  
def isExecutable: Boolean
```
**Purpose:** Check file system permissions  
**Platform behavior:** Uses platform-specific permission systems  
**Examples:**
```scala
Path("script.sh").isExecutable  // true if can execute
Path("readonly.txt").isWritable // false if read-only
Path("file.txt").isReadable     // true if can read
```

### File Properties
```scala
def size: Long
def lastModified: Long
def isEmpty: Boolean
```
**Purpose:** Get file metadata  
**isEmpty behavior:** For files: size == 0, for directories: no entries  
**Examples:**
```scala
Path("file.txt").size           // File size in bytes
Path("file.txt").lastModified   // Timestamp in milliseconds
Path("file.txt").isEmpty        // true if zero bytes
Path("dir").isEmpty             // true if no files/subdirs
```

### File Comparison
```scala
def isSameFile(other: Path): Boolean
```
**Purpose:** Check if two paths refer to same file system entry  
**Implementation:** Uses platform-specific file identity (inodes, etc.)  
**Examples:**
```scala
Path("file.txt").isSameFile(Path("./file.txt"))     // true
Path("file.txt").isSameFile(Path("copy.txt"))       // false
Path("link.txt").isSameFile(Path("target.txt"))     // true if link to target
```

## File Operations (I/O)

### Text File Operations
```scala
def readText(charset: String = "UTF-8"): String
def writeText(content: String, charset: String = "UTF-8"): Unit
```
**Purpose:** Read/write text files with encoding support  
**Default:** UTF-8 encoding  
**Examples:**
```scala
Path("config.txt").writeText("key=value")
val content = Path("config.txt").readText()
Path("latin.txt").readText("ISO-8859-1")      // Custom encoding
```

### Binary File Operations
```scala
def readBytes: Array[Byte]
def writeBytes(data: Array[Byte]): Unit
```
**Purpose:** Read/write binary data  
**Examples:**
```scala
val imageData = Array[Byte](0xFF, 0xD8, 0xFF) // JPEG header
Path("image.jpg").writeBytes(imageData)
val data = Path("image.jpg").readBytes
```

### Directory Listing
```scala
def listDirectory(pattern: String = "*"): Vector[DirectoryEntry]
```
**Purpose:** List directory contents with optional glob filtering  
**Pattern support:** `*` (any chars), `?` (single char), literal text  
**Returns:** Vector of DirectoryEntry with name and type  
**Examples:**
```scala
Path("src").listDirectory()           // All entries
Path("src").listDirectory("*.scala")  // Scala files only
Path("src").listDirectory("test*")    // Files starting with "test"

// Process results
val entries = Path(".").listDirectory()
entries.foreach { entry =>
  println(s"${entry.fileType}: ${entry.name}")
}
```

### Directory Management
```scala
def createDirectory(): Unit
def createDirectories(): Unit
```
**Purpose:** Create directories  
**Difference:** `createDirectory` fails if parent doesn't exist  
**Examples:**
```scala
Path("newdir").createDirectory()              // Create single directory
Path("a/b/c").createDirectories()             // Create entire path
```

### File Management
```scala
def delete(): Unit
def copyTo(target: Path): Unit
def moveTo(target: Path): Unit
```
**Purpose:** File and directory management  
**Examples:**
```scala
Path("file.txt").delete()                     // Delete file
Path("source.txt").copyTo(Path("backup.txt")) // Copy file
Path("temp.txt").moveTo(Path("final.txt"))    // Move/rename file
```

## Error Handling

### Common Exceptions
- **`IllegalArgumentException`**: Invalid path operations, ranges, mixing absolute/relative
- **Platform-specific I/O exceptions**: File not found, permission denied, disk full
- **`UnsupportedOperationException`**: Platform limitations (e.g., symlinks on some systems)

### Examples
```scala
// These throw IllegalArgumentException:
Path("/abs").relativeTo(Path("rel"))          // Mixed absolute/relative
Path("a/b").subpath(-1, 2)                    // Invalid range
Path("nonexistent").isEmpty                 // File doesn't exist

// Platform I/O exceptions:
Path("/protected/file").readText()            // Permission denied
Path("nonexistent.txt").readText()            // File not found
```

## Cross-Platform Behavior

### Path Separators
- **Construction:** Always accepts `/` or `\` in string input
- **toString:** Always uses `/` for consistency
- **toPlatformString:** Uses platform-appropriate separator

### Platform-Specific Features
| Feature | JVM | JavaScript | Native |
|---------|-----|------------|--------|
| Symbolic links | Full support | Full support | Full support |
| Permissions | Full support | Node.js permissions | Full support |
| File identity | inode comparison | dev/inode comparison | inode comparison |
| Performance | High | Node.js dependent | High |

### Limitations
- **Browser JavaScript:** No file system access (Node.js only)
- **Permissions:** May vary by platform and file system
- **Symbolic links:** Platform and file system dependent

## Performance Characteristics

### Memory Usage
- **Immutable:** Each operation creates new Path instance
- **Efficient:** Shares underlying Vector storage where possible
- **Lightweight:** Only stores segments and boolean flag

### I/O Operations
- **Blocking:** All file operations are synchronous
- **Buffered:** Reads entire files into memory
- **Platform-optimized:** Uses best APIs for each platform

## Usage Patterns

### Recommended
```scala
// Chain operations fluently
val configPath = Path("config") / "app.conf"
if (configPath.exists && configPath.isReadable) {
  val config = configPath.readText()
}

// Use pattern matching with case class
path match {
  case Path(Vector(single), false) => // Single relative file
  case Path(segments, true) if segments.startsWith(Vector("etc")) => // /etc/...
}

// Handle errors appropriately
try {
  val content = path.readText()
} catch {
  case _: IOException => // Handle file errors
  case _: IllegalArgumentException => // Handle path errors
}
```

### Anti-patterns
```scala
// Don't concatenate strings manually
val bad = Path(s"$base/$file")  // Use base / file instead

// Don't ignore file existence
path.readText()  // Check exists first

// Don't mix absolute/relative carelessly
absPath.relativeTo(relPath)  // Will throw exception
```

## Thread Safety

- **Path instances:** Immutable and thread-safe
- **File operations:** Not synchronized (standard I/O behavior)
- **Static methods:** Thread-safe

## Integration

### With Java
```scala
import java.nio.file.{Paths, Files}

// Convert to/from Java Path
val javaPath = Paths.get(path.toPlatformString)
val pathFromJava = Path(javaPath.toString)

// Use with Java APIs
Files.walk(Paths.get(path.toPlatformString))
```

### With OS-Lib (Alternative)
```scala
// Path provides similar functionality to os.Path
// but with cross-platform support and different API design
```

### Build Tool Integration
```scala
// sbt sourceDirectories
sourceDirectories in Compile := Seq(
  (Path("src") / "main" / "scala").toPlatformString
).map(file)
```