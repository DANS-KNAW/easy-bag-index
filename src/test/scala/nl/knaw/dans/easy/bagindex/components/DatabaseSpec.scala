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

import nl.knaw.dans.easy.bagindex.{ BagAlreadyInIndexException, BagIdNotFoundException, BagIndexDatabaseFixture, BagInfo, TestSupportFixture }
import nl.knaw.dans.lib.error.TraversableTryExtensions
import org.joda.time.DateTime

import scala.util.{ Failure, Success }

class DatabaseSpec extends TestSupportFixture with BagIndexDatabaseFixture with DatabaseComponent {

  override val database = new Database {}

  "getBaseBagId" should "return the base of a specific bagId" in {
    val bagIds @ (baseId :: _) = List.fill(3)(UUID.randomUUID())
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )
    val dois = List("10.5072/dans-x6f-kf6x", "10.5072/dans-x6f-kf66", "10.5072/dans-y7g-lg77")
    val urns = List("urn:isan:0000-0000-2CEA-0000-1-0000-0000-Y", "urn:isbn:0451450523", "urn:nbn:de:bvb:19-146642")

    (bagIds zip times zip dois zip urns)
      .map { case (((bagId, time), doi), urn) => database.addBagInfo(bagId, baseId, time, doi, urn) }
      .collectResults shouldBe a[Success[_]]

    for (bagId <- bagIds) {
      database.getBaseBagId(bagId) should matchPattern { case Success(`baseId`) => }
    }
  }

  it should "return a Failure with a BagIdNotFoundException inside if the bagId is not present in the database" in {
    // Note: the database is empty at this point!
    val someBagId = UUID.randomUUID()
    database.getBaseBagId(someBagId) should matchPattern { case Failure(BagIdNotFoundException(`someBagId`)) => }
  }

  "getAllBagsWithBase" should "return a sequence with only the baseId when there are no child bags declared" in {
    val bagId = UUID.randomUUID()
    val time = DateTime.now()
    val doi = "10.5072/dans-x6f-kf6x"
    val urn = "urn:nbn:de:bvb:19-146642"

    database.addBagInfo(bagId, bagId, time, doi, urn) shouldBe a[Success[_]]

    inside(database.getAllBagsWithBase(bagId)) {
      case Success(ids) => ids should (have size 1 and contain only bagId)
    }
  }

  it should "return a sequence with all bagIds with a certain baseId" in {
    val bagIds1 @ (baseId1 :: _) = List.fill(3)(UUID.randomUUID())
    val times1 = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )
    val urns1 = List("urn:isan:0000-0000-2CEA-0000-1-0000-0000-Y", "urn:isbn:0451450523", "urn:nbn:de:bvb:19-146642")

    val bagIds2 @ (baseId2 :: _) = List.fill(5)(UUID.randomUUID())
    val times2 = List(
      DateTime.parse("2001-09-11"),
      DateTime.parse("2017"),
      DateTime.parse("2017").plusDays(2),
      DateTime.parse("2017-03-09"),
      DateTime.parse("2018")
    )
    val urns2 = List("urn:uuid:6e8bc430-9c3a-11d9-9669-0800200c9a66", "urn:isbn:0451450523", "urn:ISSN:0167-6423", "urn:ietf:rfc:2648", "urn:mpeg:mpeg7:schema:2001")

    List(
      ((bagIds1, times1, urns1).zipped.toList, baseId1),
      ((bagIds2, times2, urns2).zipped.toList, baseId2))
      .flatMap { case (xs, base) => xs.map { case (bagId, time, urn) => database.addBagInfo(bagId, base, time, "", urn) } }
      .collectResults shouldBe a[Success[_]]

    inside(database.getAllBagsWithBase(baseId1)) {
      case Success(ids) => ids should (have size 3 and contain theSameElementsAs bagIds1)
    }
    inside(database.getAllBagsWithBase(baseId2)) {
      case Success(ids) => ids should (have size 5 and contain theSameElementsAs bagIds2)
    }
  }

  "getBagInfo" should "return the relation object for the given bagId" in {
    val bagIds @ (baseId :: _) = List.fill(3)(UUID.randomUUID())
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )
    val dois = List("10.5072/dans-x6f-kf6x", "10.5072/dans-x6f-kf66", "10.5072/dans-y7g-lg77")
    val urns = List("urn:isan:0000-0000-2CEA-0000-1-0000-0000-Y", "urn:isbn:0451450523", "urn:nbn:de:bvb:19-146642")

    (bagIds zip times zip dois zip urns)
      .map { case (((bagId, time), doi), urn) => database.addBagInfo(bagId, baseId, time, doi, urn) }
      .collectResults shouldBe a[Success[_]]

    (bagIds zip times zip dois zip urns)
      .foreach { case (((bagId, created), doi), urn) => database.getBagInfo(bagId) should matchPattern { case Success(BagInfo(`bagId`, `baseId`, `created`, `doi`, `urn`)) => }
      }
  }

  it should "return a BagIdNotFoundException when the given bagId does not exist in the database" in {
    // Note: the database is empty at this point!
    val someBagId = UUID.randomUUID()
    database.getBagInfo(someBagId) should matchPattern { case Failure(BagIdNotFoundException(`someBagId`)) => }
  }

  "getBagsWithDoi" should "return all bags with a certain DOI" in {
    val bagIds @ bagId1 :: bagId2 :: Nil = List.fill(2)(UUID.randomUUID())
    val times @ time1 :: time2 :: Nil = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.now()
    )
    val dois @ doi1 :: _ = List.fill(2)("10.5072/dans-x6f-kf6x")

    (bagIds, times, dois).zipped.toList
      .map { case (bagId, time, doi) => database.addBagInfo(bagId, bagId, time, doi, "") }
      .collectResults shouldBe a[Success[_]]

    inside(database.getBagsWithIdentifier(doi1, "doi")) {
      case Success(bags) => bags should (have size 2 and contain only(BagInfo(bagId1, bagId1, time1, doi1, ""), BagInfo(bagId2, bagId2, time2, doi1, "")))
    }
  }

  it should "return the bag with a certain DOI if there is only one" in {
    val bagId = UUID.randomUUID()
    val time = DateTime.parse("1992-07-30T16:00:00")
    val doi = "10.5072/dans-x6f-kf6x"

    database.addBagInfo(bagId, bagId, time, doi, "") shouldBe a[Success[_]]

    inside(database.getBagsWithIdentifier(doi, "doi")) {
      case Success(bags) => bags should (have size 1 and contain only BagInfo(bagId, bagId, time, doi, ""))
    }
  }

  it should "return an empty sequence when the DOI isn't found" in {
    inside(database.getBagsWithIdentifier("10.5072/dans-x6f-kf6x", "doi")) {
      case Success(bags) => bags shouldBe empty
    }
  }

  "addBagInfo" should "insert a new bag relation into the database" in {
    val bagIds @ (baseId :: _) = List.fill(3)(UUID.randomUUID())
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )
    val dois = List("10.5072/dans-x6f-kf6x", "10.5072/dans-x6f-kf66", "10.5072/dans-y7g-lg77")
    val urns = List("urn:isan:0000-0000-2CEA-0000-1-0000-0000-Y", "urn:isbn:0451450523", "urn:nbn:de:bvb:19-146642")

    (bagIds zip times zip dois zip urns)
      .map { case (((bagId, time), doi), urn) => database.addBagInfo(bagId, baseId, time, doi, urn) }
      .collectResults shouldBe a[Success[_]]

    val rel1 :: rel2 :: rels = (bagIds zip times zip dois zip urns).map { case (((bagId, time), doi), urn) => BagInfo(bagId, baseId, time, doi, urn) }

    inside(database.getAllBagInfos) {
      case Success(relations) => relations should contain allOf(rel1, rel2, rels: _*)
    }
  }

  it should "fail if inserting a bag relation twice" in {
    val bagId = UUID.randomUUID()
    val baseId = UUID.randomUUID()
    val time = DateTime.now()
    val doi = "10.5072/dans-x6f-kf66"
    val urn = "urn:nbn:de:bvb:19-146642"

    val result1 = database.addBagInfo(bagId, baseId, time, doi, urn)
    val result2 = database.addBagInfo(bagId, baseId, time, doi, urn)

    result1 shouldBe a[Success[_]]
    result2 should matchPattern { case Failure(BagAlreadyInIndexException(`bagId`)) => }

    inside(database.getAllBagInfos) {
      case Success(relations) => relations should contain(BagInfo(bagId, baseId, time, doi, urn))
    }
  }
}
