package io.github.edadma.path

import io.github.edadma.cross_platform
import io.github.edadma.cross_platform.{DirectoryEntry, FileType, nameSeparator}

case class Path(segments: Vector[String], isAbsolute: Boolean) {

  // ===== PATH OPERATIONS (Pure - no I/O) =====

  def /(other: String): Path =
    Path(segments :+ other, isAbsolute)

  def /(other: Path): Path =
    if (other.isAbsolute) other
    else Path(segments ++ other.segments, isAbsolute)

  // The opposite of / - get relative path from base to this
  def relativeTo(base: Path): Path = {
    if (isAbsolute != base.isAbsolute) {
      throw new IllegalArgumentException("Cannot get relative path between absolute and relative paths")
    }

    // Find common prefix
    val commonLength = segments.zip(base.segments).takeWhile { case (a, b) => a == b }.length
    val upSteps      = base.segments.length - commonLength
    val downSteps    = segments.drop(commonLength)

    val relativeSegments = Vector.fill(upSteps)("..") ++ downSteps
    Path(relativeSegments, isAbsolute = false)
  }

  def parent: Option[Path] =
    if (segments.isEmpty) None
    else Some(Path(segments.dropRight(1), isAbsolute))

  def filename: String = segments.lastOption.getOrElse("")

  def extension: String = {
    val name     = filename
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex > 0 && dotIndex < name.length - 1) name.substring(dotIndex)
    else ""
  }

  def nameWithoutExtension: String = {
    val name     = filename
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex > 0) name.substring(0, dotIndex)
    else name
  }

  def withExtension(ext: String): Path = {
    val newExt  = if (ext.startsWith(".")) ext else "." + ext
    val newName = nameWithoutExtension + newExt
    parent.map(_ / newName).getOrElse(Path(newName))
  }

  def startsWith(other: Path): Boolean = {
    if (isAbsolute != other.isAbsolute) false
    else segments.startsWith(other.segments)
  }

  def endsWith(other: Path): Boolean = {
    segments.endsWith(other.segments)
  }

  def subpath(start: Int, end: Int): Path = {
    require(
      start >= 0 && end <= segments.length && start <= end,
      s"Invalid subpath range: start=$start, end=$end, segments.length=${segments.length}",
    )
    Path(segments.slice(start, end), isAbsolute = false)
  }

  def toAbsolutePath(): Path = {
    if (isAbsolute) this
    else {
      val currentDir = Path(cross_platform.getCurrentDirectory)
      currentDir / this
    }
  }

  def normalize: Path = {
    val normalized = segments.foldLeft(Vector.empty[String]) {
      case (acc, "..") if acc.nonEmpty && acc.last != ".." => acc.dropRight(1)
      case (acc, ".")                                      => acc
      case (acc, segment)                                  => acc :+ segment
    }
    Path(normalized, isAbsolute)
  }

  // ===== FILE SYSTEM METADATA (I/O operations) =====

  def exists(): Boolean         = cross_platform.exists(toPlatformString)
  def isFile(): Boolean         = cross_platform.isFile(toPlatformString)
  def isDirectory(): Boolean    = cross_platform.isDirectory(toPlatformString)
  def isSymbolicLink(): Boolean = cross_platform.isSymbolicLink(toPlatformString)
  def isReadable(): Boolean     = cross_platform.isReadable(toPlatformString)
  def isWritable(): Boolean     = cross_platform.isWritable(toPlatformString)
  def isExecutable(): Boolean   = cross_platform.isExecutable(toPlatformString)

  def size(): Long         = cross_platform.fileSize(toPlatformString)
  def lastModified(): Long = cross_platform.lastModified(toPlatformString)

  def isEmpty(): Boolean = {
    if (!exists()) throw new IllegalArgumentException(s"Path does not exist: $this")
    if (isDirectory()) listDirectory().isEmpty
    else size() == 0
  }

  def isSameFile(other: Path): Boolean = cross_platform.isSameFile(toPlatformString, other.toPlatformString)

  // ===== FILE OPERATIONS (Delegate to cross-platform library) =====

  def readText(charset: String = "UTF-8"): String                 = cross_platform.readFile(toPlatformString)
  def writeText(content: String, charset: String = "UTF-8"): Unit = cross_platform.writeFile(toPlatformString, content)
  def readBytes(): Array[Byte]                                    = cross_platform.readBytes(toPlatformString)
  def writeBytes(data: Array[Byte]): Unit                         = cross_platform.writeBytes(toPlatformString, data)

  def listDirectory(pattern: String = "*"): Vector[DirectoryEntry] = {
    val entries = cross_platform.listDirectoryWithTypes(toPlatformString)
    if (pattern == "*") {
      entries
    } else {
      val regex = globToRegex(pattern)
      entries.filter(entry => entry.name.matches(regex))
    }
  }

  def createDirectory(): Unit   = cross_platform.createDirectory(toPlatformString)
  def createDirectories(): Unit = cross_platform.createDirectories(toPlatformString)

  def delete(): Unit             = cross_platform.deleteFile(toPlatformString)
  def copyTo(target: Path): Unit = cross_platform.copyFile(toPlatformString, target.toPlatformString)
  def moveTo(target: Path): Unit = cross_platform.moveFile(toPlatformString, target.toPlatformString)

  // ===== INTERNAL HELPERS =====

  def toPlatformString: String = {
    val pathStr = segments.mkString(nameSeparator)
    if (isAbsolute) {
      if (segments.nonEmpty && segments.head.matches("[A-Za-z]:")) {
        // Windows drive letter
        pathStr
      } else {
        // Unix-like absolute path
        nameSeparator + pathStr
      }
    } else {
      pathStr
    }
  }

  private def globToRegex(pattern: String): String = {
    pattern
      .replace(".", "\\.")
      .replace("*", ".*")
      .replace("?", ".")
  }

  override def toString: String = {
    val pathStr = segments.mkString("/")
    if (isAbsolute) "/" + pathStr else pathStr
  }
}

object Path {
  def apply(pathString: String): Path = {
    val isAbs    = pathString.startsWith("/") || pathString.matches("[A-Za-z]:[/\\\\].*")
    val cleaned  = pathString.replaceAll("[/\\\\]+", "/")
    val segments = cleaned.split("/").filter(_.nonEmpty).toVector
    Path(segments, isAbs)
  }
}

// Test Application
object PathTest extends App {
  println("=== Enhanced Path Library Test ===\n")

  // Test path operations
  println("--- Path Operations ---")
  val root       = Path("/")
  val home       = Path("/home")
  val userDir    = home / "user" / "documents"
  val configFile = userDir / "config.txt"

  println(s"Root: $root")
  println(s"Home: $home")
  println(s"User dir: $userDir")
  println(s"Config file: $configFile")
  println(s"Config parent: ${configFile.parent}")
  println(s"Config filename: ${configFile.filename}")

  // Test new path manipulation methods
  println(s"Config extension: '${configFile.extension}'")
  println(s"Config name without extension: '${configFile.nameWithoutExtension}'")
  println(s"Config with .json extension: ${configFile.withExtension("json")}")

  val projectPath = Path("src/main/scala/MyApp.scala")
  println(s"Project path: $projectPath")
  println(s"Extension: '${projectPath.extension}'")
  println(s"Name without extension: '${projectPath.nameWithoutExtension}'")

  // Test startsWith/endsWith
  val libPath = Path("lib/utils/helper.scala")
  println(s"$libPath starts with 'lib': ${libPath.startsWith(Path("lib"))}")
  println(s"$libPath ends with 'helper.scala': ${libPath.endsWith(Path("helper.scala"))}")

  // Test subpath
  val longPath = Path("a/b/c/d/e/f")
  println(s"$longPath subpath(1,4): ${longPath.subpath(1, 4)}")

  // Test relative paths
  val relPath = Path("src") / "main" / "scala"
  println(s"Relative path: $relPath")
  println(s"Is absolute: ${userDir.isAbsolute}, ${relPath.isAbsolute}")

  // Test the new relativeTo operation
  val base     = Path("/home/user")
  val target   = Path("/home/user/documents/config.txt")
  val relative = target.relativeTo(base)
  println(s"$target relative to $base = $relative")

  // Test path normalization
  val messyPath = Path("src/../config/./app/../settings.conf")
  println(s"Messy path: $messyPath")
  println(s"Normalized: ${messyPath.normalize}")

  println("\n--- File Operations ---")

  // Test with current directory
  val currentDir = Path(".")
  println(s"Current directory exists: ${currentDir.exists()}")
  println(s"Is directory: ${currentDir.isDirectory()}")

  if (currentDir.exists() && currentDir.isDirectory()) {
    println("\nFiles in current directory:")
    val entries = currentDir.listDirectory()
    entries.take(10).foreach { entry =>
      val typeStr = entry.fileType match {
        case FileType.File         => "FILE"
        case FileType.Directory    => "DIR "
        case FileType.SymbolicLink => "LINK"
        case FileType.Other        => "OTHER"
      }
      println(s"  $typeStr: ${entry.name}")
    }

    if (entries.length > 10) {
      println(s"  ... and ${entries.length - 10} more")
    }

    // Test new metadata methods
    println(s"\nCurrent directory metadata:")
    println(s"  Readable: ${currentDir.isReadable()}")
    println(s"  Writable: ${currentDir.isWritable()}")
    println(s"  Executable: ${currentDir.isExecutable()}")
    if (currentDir.isDirectory()) {
      println(s"  Directory empty: ${currentDir.isEmpty()}")
    }

    // Test pattern matching
    val scalaFiles = currentDir.listDirectory("*.scala")
    if (scalaFiles.nonEmpty) {
      println(s"\nScala files (*.scala): ${scalaFiles.map(_.name).mkString(", ")}")
    }
  }

  // Test file creation and manipulation
  println("\n--- File Creation Test ---")
  val testFile = Path("test-file.txt")

  try {
    // Create a test file
    testFile.writeText("Hello, Enhanced Path!\nThis is a test file.")
    println(s"Created test file: $testFile")
    println(s"File exists: ${testFile.exists()}")
    println(s"File size: ${testFile.size()} bytes")
    println(s"Is file: ${testFile.isFile()}")
    println(s"Is readable: ${testFile.isReadable()}")
    println(s"Is writable: ${testFile.isWritable()}")
    println(s"Is empty: ${testFile.isEmpty()}")

    // Test absolute path conversion
    println(s"Absolute path: ${testFile.toAbsolutePath()}")

    // Read it back
    val content = testFile.readText()
    println(s"File content:\n$content")

    // Clean up
    testFile.delete()
    println(s"Deleted test file. Exists: ${testFile.exists()}")

  } catch {
    case e: Exception =>
      println(s"Error during file test: ${e.getMessage}")
      // Try to clean up if file was created
      if (testFile.exists()) {
        try testFile.delete()
        catch { case _: Exception => }
      }
  }

  println("\n=== Enhanced Test Complete ===")
}
