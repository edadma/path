package io.github.edadma.path

import java.nio.file.{Files, Paths, StandardCopyOption}
import java.nio.charset.Charset
import scala.jdk.CollectionConverters._

object PlatformFileSystem {

  private def toJavaPath(path: Path): java.nio.file.Path = {
    Paths.get(toPlatformString(path))
  }

  def toPlatformString(path: Path): String = {
    val pathStr = path.segments.mkString(java.io.File.separator)
    if (path.isAbsolute) {
      if (System.getProperty("os.name").toLowerCase.contains("win")) {
        // Windows absolute path
        if (path.segments.nonEmpty && path.segments.head.matches("[A-Za-z]:")) {
          pathStr
        } else {
          "C:" + java.io.File.separator + pathStr
        }
      } else {
        // Unix-like absolute path
        java.io.File.separator + pathStr
      }
    } else {
      pathStr
    }
  }

  def exists(path: Path): Boolean =
    Files.exists(toJavaPath(path))

  def isFile(path: Path): Boolean =
    Files.isRegularFile(toJavaPath(path))

  def isDirectory(path: Path): Boolean =
    Files.isDirectory(toJavaPath(path))

  def readText(path: Path, charset: String): String =
    new String(Files.readAllBytes(toJavaPath(path)), Charset.forName(charset))

  def writeText(path: Path, content: String, charset: String): Unit =
    Files.write(toJavaPath(path), content.getBytes(Charset.forName(charset)))

  def readBytes(path: Path): Array[Byte] =
    Files.readAllBytes(toJavaPath(path))

  def writeBytes(path: Path, data: Array[Byte]): Unit =
    Files.write(toJavaPath(path), data)

  def listDirectory(path: Path, pattern: String): Vector[DirectoryEntry] = {
    val javaPath = toJavaPath(path)
    if (!Files.isDirectory(javaPath)) {
      throw new IllegalArgumentException(s"Path is not a directory: $path")
    }

    val entries = Files.list(javaPath).iterator().asScala.toVector.map { entry =>
      val name     = entry.getFileName.toString
      val fileType = if (Files.isDirectory(entry)) FileType.Directory
      else if (Files.isSymbolicLink(entry)) FileType.SymbolicLink
      else if (Files.isRegularFile(entry)) FileType.File
      else FileType.Other
      DirectoryEntry(name, fileType)
    }

    if (pattern == "*") {
      entries
    } else {
      val regex = globToRegex(pattern)
      entries.filter(entry => entry.name.matches(regex))
    }
  }

  private def globToRegex(pattern: String): String = {
    pattern
      .replace(".", "\\.")
      .replace("*", ".*")
      .replace("?", ".")
  }

  def createDirectory(path: Path): Unit =
    Files.createDirectory(toJavaPath(path))

  def createDirectories(path: Path): Unit =
    Files.createDirectories(toJavaPath(path))

  def delete(path: Path): Unit =
    Files.delete(toJavaPath(path))

  def copyTo(source: Path, target: Path): Unit =
    Files.copy(toJavaPath(source), toJavaPath(target), StandardCopyOption.REPLACE_EXISTING)

  def moveTo(source: Path, target: Path): Unit =
    Files.move(toJavaPath(source), toJavaPath(target), StandardCopyOption.REPLACE_EXISTING)

  def size(path: Path): Long =
    Files.size(toJavaPath(path))

  def lastModified(path: Path): Long =
    Files.getLastModifiedTime(toJavaPath(path)).toMillis
}
