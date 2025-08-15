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
    if (tempDir.exists() && tempDir.isDirectory()) {
      deleteRecursively(tempDir)
    }
  }

  private def deleteRecursively(path: Path): Unit = {
    if (path.isDirectory()) {
      path.listDirectory().foreach { entry =>
        deleteRecursively(path / entry.name)
      }
    }
    if (path.exists()) {
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

  "Path parent and filename" should "work correctly" in {
    val path = Path("/home/user/file.txt")
    path.parent.map(_.segments) shouldBe Some(Vector("home", "user"))
    path.filename shouldBe "file.txt"

    Path("/").parent shouldBe None
    Path("file.txt").parent shouldBe Some(Path(""))
  }

  // ===== FILE OPERATIONS TESTS =====

  "File creation and deletion" should "work with text files" in {
    val testFile = tempDir / "test.txt"
    val content  = "Hello, World!\nThis is a test."

    testFile.exists() shouldBe false

    testFile.writeText(content)
    testFile.exists() shouldBe true
    testFile.isFile() shouldBe true
    testFile.isDirectory() shouldBe false

    testFile.readText() shouldBe content
    testFile.size() shouldBe content.getBytes("UTF-8").length

    testFile.delete()
    testFile.exists() shouldBe false
  }

  it should "work with binary files" in {
    val testFile = tempDir / "test.bin"
    val data     = Array[Byte](1, 2, 3, 4, 5, -1, -2, -3)

    testFile.writeBytes(data)
    testFile.exists() shouldBe true
    testFile.isFile() shouldBe true

    testFile.readBytes() shouldBe data
    testFile.size() shouldBe data.length

    testFile.delete()
    testFile.exists() shouldBe false
  }

  "Directory operations" should "create and list directories" in {
    val subDir = tempDir / "subdir"

    subDir.exists() shouldBe false
    subDir.createDirectory()
    subDir.exists() shouldBe true
    subDir.isDirectory() shouldBe true
    subDir.isFile() shouldBe false
  }

  it should "create nested directories" in {
    val nestedDir = tempDir / "level1" / "level2" / "level3"

    nestedDir.exists() shouldBe false
    nestedDir.createDirectories() // This should create all parent dirs
    nestedDir.exists() shouldBe true
    nestedDir.isDirectory() shouldBe true

    // Parents should also exist
    (tempDir / "level1").exists() shouldBe true
    (tempDir / "level1" / "level2").exists() shouldBe true
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
    source.exists() shouldBe true
    target.exists() shouldBe false

    source.copyTo(target)

    source.exists() shouldBe true // Source should still exist
    target.exists() shouldBe true
    target.readText() shouldBe content
  }

  it should "move files correctly" in {
    val source  = tempDir / "source.txt"
    val target  = tempDir / "target.txt"
    val content = "File content to move"

    source.writeText(content)
    source.exists() shouldBe true
    target.exists() shouldBe false

    source.moveTo(target)

    source.exists() shouldBe false // Source should be gone
    target.exists() shouldBe true
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
    myPackage.exists() shouldBe true
    (myPackage / "lib" / "main.js").exists() shouldBe true
    dependency.exists() shouldBe true

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
}
