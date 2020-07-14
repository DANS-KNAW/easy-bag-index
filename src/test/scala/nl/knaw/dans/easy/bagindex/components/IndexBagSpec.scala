/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.bagindex.components

import java.util.UUID

import nl.knaw.dans.easy.bagindex._
import nl.knaw.dans.easy.bagindex.access.{ BagFacadeComponent, BagStoreAccessComponent }
import nl.knaw.dans.lib.error._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.util.{ Failure, Success }

class IndexBagSpec extends TestSupportFixture
  with BagIndexDatabaseFixture
  with BagStoreFixture
  with Bagit5Fixture
  with IndexBagComponent
  with BagStoreAccessComponent
  with BagFacadeComponent
  with DatabaseComponent {

  override val database: Database = new Database {}
  override val index: IndexBag = new IndexBag {}

  "getBagSequence" should "return a sequence with only the baseId when there are no child bags declared" in {
    val bagId = UUID.randomUUID()
    val time = DateTime.now()

    database.addBagInfo(bagId, bagId, time, testDoi, testUrn) shouldBe a[Success[_]]

    inside(index.getBagSequence(bagId)) {
      case Success(ids) => ids should (have size 1 and contain only bagId)
    }
  }

  it should "return a sequence with all bagIds with a certain baseId when given this baseId" in {
    val bagIds @ (baseId :: _) = List.fill(3)(UUID.randomUUID())
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )

    (bagIds zip times zip testDois zip testUrns)
      .map { case (((bagId, time), doi), urn) => database.addBagInfo(bagId, baseId, time, doi, urn) }
      .collectResults shouldBe a[Success[_]]

    inside(index.getBagSequence(baseId)) {
      case Success(ids) => ids should (have size 3 and contain theSameElementsInOrderAs bagIds)
    }
  }

  it should "return a sequence with all bagIds with a certain baseId when given any of the contained bagIds" in {
    val bagIds @ (baseId :: _) = List.fill(3)(UUID.randomUUID())
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )

    (bagIds zip times zip testDois zip testUrns)
      .map { case (((bagId, time), doi), urn) => database.addBagInfo(bagId, baseId, time, doi, urn) }
      .collectResults shouldBe a[Success[_]]

    for (bagId <- bagIds) {
      inside(index.getBagSequence(bagId)) {
        case Success(ids) => ids should (have size 3 and contain theSameElementsInOrderAs bagIds)
      }
    }
  }

  it should "fail if the given bagId is not present in the database" in {
    // Note: the database is empty at this point!
    val someOtherBagId = UUID.randomUUID()
    index.getBagSequence(someOtherBagId) should matchPattern { case Failure(BagIdNotFoundException(`someOtherBagId`)) => }
  }

  "add" should "put a relation from the bagId to itself in the database when no base is specified" in {
    val bagId = UUID.randomUUID()

    index.addBase(bagId, doi = testDoi, urn = testUrn) should matchPattern { case Success(`bagId`) => }

    inside(database.getAllBagInfos) {
      case Success(relations) => relations.map { case BagInfo(id, base, _, _, _) => (id, base) } should contain((bagId, bagId))
    }
  }

  it should "put a relation from the bagId to its base in the database when the baseId exists in the database" in {
    val baseId = UUID.randomUUID()
    val bagId = UUID.randomUUID()
    val doi = testDoi.replaceAll("6", "7")
    val urn = testUrn.replaceAll("1", "2")

    index.addBase(baseId, doi = testDoi, urn = testUrn) shouldBe a[Success[_]]
    index.add(bagId, baseId, doi = doi, urn = urn) should matchPattern { case Success(`baseId`) => }

    inside(database.getAllBagInfos) {
      case Success(relations) => relations.map { case BagInfo(id, base, _, _, _) => (id, base) } should contain((bagId, baseId))
    }
  }

  it should "put a relation from the bagId to a super-baseId in the database when the given baseId has another baseId" in {
    val superBaseId = UUID.randomUUID()
    val baseId = UUID.randomUUID()
    val bagId = UUID.randomUUID()
    val doi = testDoi.replaceAll("6", "8")
    val urn = testUrn.replaceAll("1", "2")

    index.addBase(superBaseId, doi = testDoi, urn = testUrn) shouldBe a[Success[_]]
    index.add(baseId, superBaseId, doi = doi, urn = urn) shouldBe a[Success[_]]
    index.add(bagId, baseId, doi = doi, urn = urn) should matchPattern { case Success(`superBaseId`) => }

    inside(database.getAllBagInfos) {
      case Success(relations) => relations.map { case BagInfo(id, base, _, _, _) => (id, base) } should contain((bagId, superBaseId))
    }
  }

  it should "fail with a BagIdNotFoundException when the specified baseId does not exist in the database" in {
    val bagId = UUID.randomUUID()
    val baseId = UUID.randomUUID()

    // assert that the base is not yet present in the database
    inside(database.getAllBagInfos) {
      case Success(relations) => relations.map(_.bagId) should not contain baseId
    }

    index.add(bagId, baseId, doi = testDoi, urn = testUrn) should matchPattern { case Failure(BagIdNotFoundException(`baseId`)) => }
  }

  private def assertBagInfoNotInDatabase(bagId: BagId): Unit = {
    database.getBagInfo(bagId) should matchPattern { case Failure(BagIdNotFoundException(`bagId`)) => }
  }

  private def assertBagInfoInDatabase(relation: BagInfo): Unit = {
    inside(database.getBagInfo(relation.bagId)) {
      case Success(BagInfo(id, base, created, doi, urn)) =>
        id shouldBe relation.bagId
        base shouldBe relation.baseId
        created.toString(dateTimeFormatter) shouldBe relation.created.toString(dateTimeFormatter)
        doi shouldBe relation.doi
        urn shouldBe relation.urn
    }
  }

  private def assertAdditionReturnedExpectedBaseId(bagId: BagId, baseId: BaseId): Unit = {
    index.addFromBagStore(bagId) should matchPattern { case Success(`baseId`) => }
  }

  // add base bag
  "addFromBagStore" should "add a bagId to the database as a base bagId when no IS_VERSION_OF is specified in the bag-info.txt" in {
    val bagId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    assertBagInfoNotInDatabase(bagId)

    assertAdditionReturnedExpectedBaseId(bagId, bagId)

    assertBagInfoInDatabase(BagInfo(bagId, bagId, DateTime.parse("2017-01-16T14:35:00.888+01:00", ISODateTimeFormat.dateTime()), doiMap(bagId), urnMap(bagId)))
  }

  // add revision with direct base
  it should "add a bagId to the database with a base bagId when the correct super-baseId is specified in the bag-info.txt" in {
    val baseId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val bagId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    assertBagInfoNotInDatabase(baseId)
    assertBagInfoNotInDatabase(bagId)

    assertAdditionReturnedExpectedBaseId(baseId, baseId)
    assertAdditionReturnedExpectedBaseId(bagId, baseId)

    assertBagInfoInDatabase(BagInfo(baseId, baseId, DateTime.parse("2017-01-16T14:35:00.888+01:00", ISODateTimeFormat.dateTime()), doiMap(baseId), urnMap(bagId)))
    assertBagInfoInDatabase(BagInfo(bagId, baseId, DateTime.parse("2017-01-17T14:35:00.888+01:00", ISODateTimeFormat.dateTime()), doiMap(bagId), urnMap(bagId)))
  }

  // add revision with indirect base
  it should "add a bagId to the database with another bagId as its base when the incorrect super-baseId is specified in the bag-info.txt" in {
    val superBaseId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val baseId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    val bagId = UUID.fromString("00000000-0000-0000-0000-000000000003")

    assertBagInfoNotInDatabase(superBaseId)
    assertBagInfoNotInDatabase(baseId)
    assertBagInfoNotInDatabase(bagId)

    assertAdditionReturnedExpectedBaseId(superBaseId, superBaseId)
    assertAdditionReturnedExpectedBaseId(baseId, superBaseId)
    assertAdditionReturnedExpectedBaseId(bagId, superBaseId)

    assertBagInfoInDatabase(BagInfo(superBaseId, superBaseId, DateTime.parse("2017-01-16T14:35:00.888+01:00", ISODateTimeFormat.dateTime()), doiMap(superBaseId), urnMap(superBaseId)))
    assertBagInfoInDatabase(BagInfo(baseId, superBaseId, DateTime.parse("2017-01-17T14:35:00.888+01:00", ISODateTimeFormat.dateTime()), doiMap(baseId), urnMap(baseId)))
    assertBagInfoInDatabase(BagInfo(bagId, superBaseId, DateTime.parse("2017-01-18T14:35:00.888+01:00", ISODateTimeFormat.dateTime()), doiMap(bagId), urnMap(bagId)))
  }

  // add with invalid bagId
  it should "fail when the bagId is not found in the bagstore" in {
    val bagId = UUID.randomUUID()
    index.addFromBagStore(bagId) should matchPattern { case Failure(BagNotFoundException(`bagId`)) => }
  }
}
