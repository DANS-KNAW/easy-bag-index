package nl.knaw.dans.easy.bagindex.components

import java.util.UUID

import nl.knaw.dans.easy.bagindex.{ BagNotFoundInBagStoreException, BagStoreFixture }

import scala.util.{ Failure, Success }

class BagStoreAccessSpec extends BagStoreFixture {

  "toLocation" should "resolve the path to the actual bag identified with a bagId" in {
    val bagId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    inside(toLocation(bagId)) {
      case Success(path) => path shouldBe bagStoreBaseDir.resolve("00/000000000000000000000000000001/bag-revision-1")
    }
  }

  it should "fail with a BagNotFoundInBagStoreException when the bag is not in the bagstore" in {
    val bagId = UUID.randomUUID()
    inside(toLocation(bagId)) {
      case Failure(BagNotFoundInBagStoreException(id, baseDir)) =>
        id shouldBe bagId
        baseDir shouldBe bagStoreBaseDir
    }
  }

  "toContainer" should "resolve the path to the bag's container identified with a bagId" in {
    val bagId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    inside(toContainer(bagId)) {
      case Success(path) => path shouldBe bagStoreBaseDir.resolve("00/000000000000000000000000000001")
    }
  }

  it should "return a None if the bag is not in the bagstore" in {
    val bagId = UUID.randomUUID()
    inside(toContainer(bagId)) {
      case Failure(BagNotFoundInBagStoreException(id, baseDir)) =>
        id shouldBe bagId
        baseDir shouldBe bagStoreBaseDir
    }
  }
}
