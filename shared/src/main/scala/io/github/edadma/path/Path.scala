package io.github.edadma.path

sealed trait FileType
object FileType {
  case object File         extends FileType
  case object Directory    extends FileType
  case object SymbolicLink extends FileType
  case object Other        extends FileType
}

case class DirectoryEntry(name: String, fileType: FileType)

case class Path(segments: Vector[String], isAbsolute: Boolean) {

  // Path operations (platform-independent)
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

  // File operations (delegate to platform implementation)
  def exists(): Boolean      = PlatformFileSystem.exists(this)
  def isFile(): Boolean      = PlatformFileSystem.isFile(this)
  def isDirectory(): Boolean = PlatformFileSystem.isDirectory(this)

  def readText(charset: String = "UTF-8"): String                 = PlatformFileSystem.readText(this, charset)
  def writeText(content: String, charset: String = "UTF-8"): Unit = PlatformFileSystem.writeText(this, content, charset)
  def readBytes(): Array[Byte]                                    = PlatformFileSystem.readBytes(this)
  def writeBytes(data: Array[Byte]): Unit                         = PlatformFileSystem.writeBytes(this, data)

  def listDirectory(pattern: String = "*"): Vector[DirectoryEntry] = PlatformFileSystem.listDirectory(this, pattern)
  def createDirectory(): Unit                                      = PlatformFileSystem.createDirectory(this)
  def createDirectories(): Unit                                    = PlatformFileSystem.createDirectories(this)

  def delete(): Unit             = PlatformFileSystem.delete(this)
  def copyTo(target: Path): Unit = PlatformFileSystem.copyTo(this, target)
  def moveTo(target: Path): Unit = PlatformFileSystem.moveTo(this, target)

  def size(): Long         = PlatformFileSystem.size(this)
  def lastModified(): Long = PlatformFileSystem.lastModified(this)

  // Convert to platform-specific path string
  def toPlatformString: String = PlatformFileSystem.toPlatformString(this)

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

// Platform abstraction
trait FileSystemOps {
  def exists(path: Path): Boolean
  def isFile(path: Path): Boolean
  def isDirectory(path: Path): Boolean
  def readText(path: Path, charset: String): String
  def writeText(path: Path, content: String, charset: String): Unit
  def readBytes(path: Path): Array[Byte]
  def writeBytes(path: Path, data: Array[Byte]): Unit
  def listDirectory(path: Path, pattern: String): Vector[DirectoryEntry]
  def createDirectory(path: Path): Unit
  def createDirectories(path: Path): Unit
  def delete(path: Path): Unit
  def copyTo(source: Path, target: Path): Unit
  def moveTo(source: Path, target: Path): Unit
  def size(path: Path): Long
  def lastModified(path: Path): Long
  def toPlatformString(path: Path): String
}
