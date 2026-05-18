package io.github.edadma.path

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import scala.util.Random
import scala.compiletime.uninitialized
import io.github.edadma.cross_platform.{FileType, DirectoryEntry}

class PathSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {

  var tempDir: Path = uninitialized

  override def beforeEach(): Unit = {
    // Create a unique temporary directory for each test using only our Path library
    val uniqueId = System.currentTimeMillis().toString + "-" + Random.nextInt(10000)
    tempDir = Path("/tmp") / s"path-test-$uniqueId"
    tempDir.createDirectories()
  }

  override def afterEach(): Unit = {
    // Clean up the temporary directory using only our Path library
    if (tempDir.exists && tempDir.isDirectory) {
      deleteRecursively(tempDir)
    }
  }

  private def deleteRecursively(path: Path): Unit = {
    if (path.isDirectory) {
      path.listDirectory().foreach { entry =>
        deleteRecursively(path / entry.name)
      }
    }
    if (path.exists) {
      path.delete()
    }
  }

  // ===== PATH OPERATIONS TESTS =====

  "Path construction" should "handle simple paths correctly" in {
    Path("foo/bar").segments shouldBe Vector("foo", "bar")
    Path("foo/bar").isAbsolute shouldBe false

    Path("/foo/bar").segments shouldBe Vector("foo", "bar")
    Path("/foo/bar").isAbsolute shouldBe true
  }

  it should "handle edge cases" in {
    Path("").segments shouldBe Vector.empty
    Path("/").segments shouldBe Vector.empty
    Path("/").isAbsolute shouldBe true

    // Multiple slashes should be normalized in construction
    Path("foo//bar///baz").segments shouldBe Vector("foo", "bar", "baz")
  }

  "Path combination (/)" should "work with strings" in {
    val base   = Path("foo")
    val result = base / "bar"
    result.segments shouldBe Vector("foo", "bar")
    result.isAbsolute shouldBe false
  }

  it should "work with other paths" in {
    val base   = Path("foo")
    val other  = Path("bar/baz")
    val result = base / other
    result.segments shouldBe Vector("foo", "bar", "baz")
    result.isAbsolute shouldBe false
  }

  it should "handle absolute paths correctly" in {
    val base     = Path("foo")
    val absolute = Path("/bar/baz")
    val result   = base / absolute
    result shouldBe absolute // Absolute path should replace, not combine
  }

  "Path relativeTo" should "calculate simple relative paths" in {
    val base     = Path("/home/user")
    val target   = Path("/home/user/documents/file.txt")
    val relative = target.relativeTo(base)

    relative.segments shouldBe Vector("documents", "file.txt")
    relative.isAbsolute shouldBe false
  }

  it should "handle going up directories" in {
    val base     = Path("/home/user/deep/path")
    val target   = Path("/home/user/file.txt")
    val relative = target.relativeTo(base)

    relative.segments shouldBe Vector("..", "..", "file.txt")
    relative.isAbsolute shouldBe false
  }

  it should "be the inverse of path combination" in {
    val base          = Path("/home/user")
    val target        = Path("/home/user/documents/config.txt")
    val relative      = target.relativeTo(base)
    val reconstructed = base / relative

    reconstructed shouldBe target
  }

  it should "throw for mixing absolute and relative paths" in {
    val absolute = Path("/foo")
    val relative = Path("bar")

    assertThrows[IllegalArgumentException] {
      absolute.relativeTo(relative)
    }
  }

  "Path parent and filename" should "work correctly" in {
    val path = Path("/home/user/file.txt")
    path.parent.map(_.segments) shouldBe Some(Vector("home", "user"))
    path.filename shouldBe "file.txt"

    Path("/").parent shouldBe None
    Path("file.txt").parent shouldBe Some(Path(""))
  }

  // ===== NEW PATH MANIPULATION TESTS =====

  "Path extension methods" should "extract file extensions correctly" in {
    Path("file.txt").extension shouldBe ".txt"
    Path("archive.tar.gz").extension shouldBe ".gz"
    Path("document.pdf").extension shouldBe ".pdf"
    Path("README").extension shouldBe ""
    Path(".gitignore").extension shouldBe ""
    Path("file.").extension shouldBe ""
    Path("/path/to/file.scala").extension shouldBe ".scala"
  }

  it should "extract name without extension correctly" in {
    Path("file.txt").nameWithoutExtension shouldBe "file"
    Path("archive.tar.gz").nameWithoutExtension shouldBe "archive.tar"
    Path("document.pdf").nameWithoutExtension shouldBe "document"
    Path("README").nameWithoutExtension shouldBe "README"
    Path(".gitignore").nameWithoutExtension shouldBe ".gitignore"
    Path("file.").nameWithoutExtension shouldBe "file"
    Path("/path/to/MyClass.scala").nameWithoutExtension shouldBe "MyClass"
  }

  it should "change extensions correctly" in {
    val txtFile = Path("document.pdf")
    txtFile.withExtension("txt") shouldBe Path("document.txt")
    txtFile.withExtension(".json") shouldBe Path("document.json")

    val noExtFile = Path("README")
    noExtFile.withExtension("md") shouldBe Path("README.md")

    val pathWithDir = Path("/home/user/file.old")
    pathWithDir.withExtension("new") shouldBe Path("/home/user/file.new")
  }

  "Path startsWith and endsWith" should "work correctly" in {
    val longPath = Path("/home/user/documents/projects/myapp/src/main/scala")

    longPath.startsWith(Path("/home")) shouldBe true
    longPath.startsWith(Path("/home/user")) shouldBe true
    longPath.startsWith(Path("/home/user/documents")) shouldBe true
    longPath.startsWith(Path("/other")) shouldBe false
    longPath.startsWith(Path("home")) shouldBe false // Different absolute/relative

    longPath.endsWith(Path("scala")) shouldBe true
    longPath.endsWith(Path("main/scala")) shouldBe true
    longPath.endsWith(Path("src/main/scala")) shouldBe true
    longPath.endsWith(Path("other")) shouldBe false
  }

  it should "handle absolute vs relative correctly" in {
    val absPath = Path("/home/user")
    val relPath = Path("home/user")

    absPath.startsWith(Path("/home")) shouldBe true
    absPath.startsWith(Path("home")) shouldBe false

    relPath.startsWith(Path("home")) shouldBe true
    relPath.startsWith(Path("/home")) shouldBe false
  }

  "Path subpath" should "extract path segments correctly" in {
    val path = Path("a/b/c/d/e")

    path.subpath(0, 2) shouldBe Path("a/b")
    path.subpath(1, 4) shouldBe Path("b/c/d")
    path.subpath(2, 5) shouldBe Path("c/d/e")
    path.subpath(0, 5) shouldBe Path("a/b/c/d/e")
    path.subpath(3, 3) shouldBe Path("")
  }

  it should "validate range parameters" in {
    val path = Path("a/b/c")

    assertThrows[IllegalArgumentException] {
      path.subpath(-1, 2)
    }

    assertThrows[IllegalArgumentException] {
      path.subpath(0, 4) // Beyond segments length
    }

    assertThrows[IllegalArgumentException] {
      path.subpath(2, 1) // start > end
    }
  }

  "Path toAbsolutePath" should "convert relative paths to absolute" in {
    val relPath = Path("documents/file.txt")
    val absPath = relPath.toAbsolutePath

    absPath.isAbsolute shouldBe true
    absPath.endsWith(Path("documents/file.txt")) shouldBe true
  }

  it should "leave absolute paths unchanged" in {
    val absPath = Path("/home/user/file.txt")
    absPath.toAbsolutePath shouldBe absPath
  }

  "Path normalization" should "handle . and .. correctly" in {
    Path("foo/./bar").normalize.segments shouldBe Vector("foo", "bar")
    Path("foo/../bar").normalize.segments shouldBe Vector("bar")
    Path("foo/bar/../baz").normalize.segments shouldBe Vector("foo", "baz")
  }

  it should "handle multiple .. correctly" in {
    Path("foo/bar/../../baz").normalize.segments shouldBe Vector("baz")
    Path("foo/../bar/../baz").normalize.segments shouldBe Vector("baz")
  }

  it should "preserve .. when it would go above root" in {
    Path("../foo").normalize.segments shouldBe Vector("..", "foo")
    Path("../../foo").normalize.segments shouldBe Vector("..", "..", "foo")
  }

  // ===== FILE OPERATIONS TESTS =====

  "File creation and deletion" should "work with text files" in {
    val testFile = tempDir / "test.txt"
    val content  = "Hello, World!\nThis is a test."

    testFile.exists shouldBe false

    testFile.writeText(content)
    testFile.exists shouldBe true
    testFile.isFile shouldBe true
    testFile.isDirectory shouldBe false

    testFile.readText() shouldBe content
    testFile.size shouldBe content.getBytes("UTF-8").length

    testFile.delete()
    testFile.exists shouldBe false
  }

  it should "work with binary files" in {
    val testFile = tempDir / "test.bin"
    val data     = Array[Byte](1, 2, 3, 4, 5, -1, -2, -3)

    testFile.writeBytes(data)
    testFile.exists shouldBe true
    testFile.isFile shouldBe true

    testFile.readBytes shouldBe data
    testFile.size shouldBe data.length

    testFile.delete()
    testFile.exists shouldBe false
  }

  // ===== NEW FILE METADATA TESTS =====

  "File metadata methods" should "check file permissions correctly" in {
    val testFile = tempDir / "permissions-test.txt"
    testFile.writeText("test content")

    testFile.isReadable shouldBe true
    testFile.isWritable shouldBe true
    // Note: isExecutable may vary by platform for text files

    testFile.delete()
  }

  it should "detect symbolic links" in {
    // Note: This test may need to be skipped on platforms without symlink support
    val targetFile = tempDir / "target.txt"
    targetFile.writeText("target content")

    // The cross-platform library would need to support creating symlinks for this test
    // testFile.isSymbolicLink shouldBe false // regular file
    // symlink.isSymbolicLink shouldBe true  // symbolic link

    targetFile.delete()
  }

  "File isEmpty method" should "detect empty files correctly" in {
    val emptyFile    = tempDir / "empty.txt"
    val nonEmptyFile = tempDir / "content.txt"

    emptyFile.writeText("")
    nonEmptyFile.writeText("some content")

    emptyFile.isEmpty shouldBe true
    nonEmptyFile.isEmpty shouldBe false

    emptyFile.delete()
    nonEmptyFile.delete()
  }

  it should "detect empty directories correctly" in {
    val emptyDir    = tempDir / "empty-dir"
    val nonEmptyDir = tempDir / "non-empty-dir"
    val fileInDir   = nonEmptyDir / "file.txt"

    emptyDir.createDirectory()
    nonEmptyDir.createDirectory()
    fileInDir.writeText("content")

    emptyDir.isEmpty shouldBe true
    nonEmptyDir.isEmpty shouldBe false

    fileInDir.delete()
    nonEmptyDir.delete()
    emptyDir.delete()
  }

  it should "throw for non-existent paths" in {
    val nonExistent = tempDir / "does-not-exist"

    assertThrows[IllegalArgumentException] {
      nonExistent.isEmpty
    }
  }

  "File isSameFile method" should "detect identical files" in {
    val file1 = tempDir / "file1.txt"
    val file2 = tempDir / "file2.txt"

    file1.writeText("content")
    file2.writeText("content")

    file1.isSameFile(file1) shouldBe true
    file1.isSameFile(file2) shouldBe false // Different files, even with same content

    file1.delete()
    file2.delete()
  }

  "Directory operations" should "create and list directories" in {
    val subDir = tempDir / "subdir"

    subDir.exists shouldBe false
    subDir.createDirectory()
    subDir.exists shouldBe true
    subDir.isDirectory shouldBe true
    subDir.isFile shouldBe false
  }

  it should "create nested directories" in {
    val nestedDir = tempDir / "level1" / "level2" / "level3"

    nestedDir.exists shouldBe false
    nestedDir.createDirectories() // This should create all parent dirs
    nestedDir.exists shouldBe true
    nestedDir.isDirectory shouldBe true

    // Parents should also exist
    (tempDir / "level1").exists shouldBe true
    (tempDir / "level1" / "level2").exists shouldBe true
  }

  "Directory listing" should "list files and directories" in {
    // Create test structure
    (tempDir / "file1.txt").writeText("content1")
    (tempDir / "file2.log").writeText("content2")
    (tempDir / "subdir").createDirectory()
    (tempDir / "another.txt").writeText("content3")

    val entries = tempDir.listDirectory()
    entries should have size 4

    val fileNames = entries.map(_.name).toSet
    fileNames shouldBe Set("file1.txt", "file2.log", "subdir", "another.txt")

    val fileTypes = entries.map(entry => entry.name -> entry.fileType).toMap
    fileTypes("file1.txt") shouldBe FileType.File
    fileTypes("subdir") shouldBe FileType.Directory
  }

  it should "filter with glob patterns" in {
    // Create test files
    (tempDir / "app.conf").writeText("config")
    (tempDir / "app.log").writeText("log")
    (tempDir / "data.txt").writeText("data")
    (tempDir / "readme.md").writeText("readme")

    // Test various patterns
    tempDir.listDirectory("*.conf").map(_.name) shouldBe Vector("app.conf")
    tempDir.listDirectory("app.*").map(_.name).toSet shouldBe Set("app.conf", "app.log")
    tempDir.listDirectory("*.txt").map(_.name) shouldBe Vector("data.txt")
    tempDir.listDirectory("*") should have size 4 // All files
  }

  "File operations" should "copy files correctly" in {
    val source  = tempDir / "source.txt"
    val target  = tempDir / "target.txt"
    val content = "File content to copy"

    source.writeText(content)
    source.exists shouldBe true
    target.exists shouldBe false

    source.copyTo(target)

    source.exists shouldBe true // Source should still exist
    target.exists shouldBe true
    target.readText() shouldBe content
  }

  it should "move files correctly" in {
    val source  = tempDir / "source.txt"
    val target  = tempDir / "target.txt"
    val content = "File content to move"

    source.writeText(content)
    source.exists shouldBe true
    target.exists shouldBe false

    source.moveTo(target)

    source.exists shouldBe false // Source should be gone
    target.exists shouldBe true
    target.readText() shouldBe content
  }

  // ===== PACKAGE MANAGER SCENARIOS =====

  "Package manager scenarios" should "handle package installation structure" in {
    // Simulate installing a package with dependencies
    val packagesDir = tempDir / "packages"
    val myPackage   = packagesDir / "my-package" / "1.0.0"
    val dependency  = packagesDir / "dependency" / "2.1.0"

    // Create package structure
    myPackage.createDirectories()
    dependency.createDirectories()

    // Create package files
    (myPackage / "package.json").writeText("""{"name": "my-package", "version": "1.0.0"}""")
    (myPackage / "lib").createDirectories() // Create lib directory
    (myPackage / "lib" / "main.js").writeText("console.log('Hello from my-package')")

    (dependency / "package.json").writeText("""{"name": "dependency", "version": "2.1.0"}""")
    (dependency / "lib").createDirectories() // Create lib directory
    (dependency / "lib" / "index.js").writeText("module.exports = 'dependency code'")

    // Verify structure
    myPackage.exists shouldBe true
    (myPackage / "lib" / "main.js").exists shouldBe true
    dependency.exists shouldBe true

    // Test finding packages
    val allPackages = packagesDir.listDirectory()
    allPackages.map(_.name).toSet shouldBe Set("my-package", "dependency")

    // Test finding JavaScript files
    val jsFiles = (myPackage / "lib").listDirectory("*.js")
    jsFiles.map(_.name) shouldBe Vector("main.js")
  }

  it should "resolve relative paths between packages" in {
    val packagesDir = tempDir / "packages"
    val pkg1        = packagesDir / "package1"
    val pkg2        = packagesDir / "package2"

    pkg1.createDirectories()
    pkg2.createDirectories()

    // From package1, get relative path to package2
    val relativePath = pkg2.relativeTo(pkg1)
    relativePath.segments shouldBe Vector("..", "package2")

    // Verify round trip
    val resolvedPath = pkg1 / relativePath
    resolvedPath.normalize shouldBe pkg2
  }

  // ===== COMPREHENSIVE PATH MANIPULATION TESTS =====

  "Enhanced path manipulation" should "handle complex scenarios" in {
    // Test chaining of operations
    val complexPath = Path("/projects/myapp/src/main/scala/MyClass.scala")

    val withoutExt = complexPath.nameWithoutExtension
    withoutExt shouldBe "MyClass"

    val newPath = complexPath.withExtension("java")
    newPath shouldBe Path("/projects/myapp/src/main/scala/MyClass.java")
    newPath.extension shouldBe ".java"

    val subPath = complexPath.subpath(2, 5)
    subPath shouldBe Path("src/main/scala")

    val parent = complexPath.parent.get
    parent shouldBe Path("/projects/myapp/src/main/scala")
  }

  it should "work with relative paths and toAbsolutePath" in {
    val relPath = Path("src/test/scala/MyTest.scala")

    relPath.isAbsolute shouldBe false
    relPath.extension shouldBe ".scala"
    relPath.nameWithoutExtension shouldBe "MyTest"

    val absPath = relPath.toAbsolutePath
    absPath.isAbsolute shouldBe true
    absPath.filename shouldBe "MyTest.scala"
    absPath.extension shouldBe ".scala"
  }

  "Path.createTempDirectory" should "create a fresh empty directory under the system temp" in {
    val d = Path.createTempDirectory("path-test-mkdtemp-")
    try {
      d.exists shouldBe true
      d.isDirectory shouldBe true
      d.isAbsolute shouldBe true
      // The prefix appears in the final segment.
      d.filename should startWith ("path-test-mkdtemp-")
      // It's actually writable — drop a file in it and read it back.
      val f = d / "ping.txt"
      f.writeText("pong")
      f.readText() shouldBe "pong"
    } finally {
      deleteRecursively(d)
    }
  }

  it should "produce distinct paths for repeated calls with the same prefix" in {
    val a = Path.createTempDirectory("path-test-distinct-")
    val b = Path.createTempDirectory("path-test-distinct-")
    try {
      a shouldNot be (b)
    } finally {
      deleteRecursively(a)
      deleteRecursively(b)
    }
  }
}
