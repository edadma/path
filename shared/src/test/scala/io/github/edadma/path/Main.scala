package io.github.edadma.path

import io.github.edadma.cross_platform.FileType

@main def run(args: String*): Unit =
  println("=== Path Cross-Platform FileSystem Test ===\n")

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

  // Test the new relativeTo operation (opposite of /)
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
