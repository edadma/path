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

  def normalize: Path = {
    val normalized = segments.foldLeft(Vector.empty[String]) {
      case (acc, "..") if acc.nonEmpty && acc.last != ".." => acc.dropRight(1)
      case (acc, ".")                                      => acc
      case (acc, segment)                                  => acc :+ segment
    }
    Path(normalized, isAbsolute)
  }

  // ===== FILE OPERATIONS (Delegate to cross-platform library) =====

  def exists(): Boolean      = cross_platform.exists(toPlatformString)
  def isFile(): Boolean      = cross_platform.isFile(toPlatformString)
  def isDirectory(): Boolean = cross_platform.isDirectory(toPlatformString)

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

  def size(): Long         = cross_platform.fileSize(toPlatformString)
  def lastModified(): Long = cross_platform.lastModified(toPlatformString)

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
  println("=== Pure Path Library Test ===\n")

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
    testFile.writeText("Hello, Path!\nThis is a test file.")
    println(s"Created test file: $testFile")
    println(s"File exists: ${testFile.exists()}")
    println(s"File size: ${testFile.size()} bytes")
    println(s"Is file: ${testFile.isFile()}")

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

  println("\n=== Test Complete ===")
}
