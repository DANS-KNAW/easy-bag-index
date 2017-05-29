/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.bagindex.components

import java.util.UUID

import nl.knaw.dans.easy.bagindex.{ BagIdNotFoundException, BagIndexDatabaseFixture, BagInfo }
import nl.knaw.dans.lib.error.TraversableTryExtensions
import org.joda.time.DateTime

import scala.util.{ Failure, Success }

class DatabaseSpec extends BagIndexDatabaseFixture with Database {

  "getBaseBagId" should "return the base of a specific bagId" in {
    val bagIds@(baseId :: _) = List.fill(3)(UUID.randomUUID())
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )
    val dois = List("10.5072/dans-x6f-kf6x", "10.5072/dans-x6f-kf66", "10.5072/dans-y7g-lg77")

    (bagIds, times, dois).zipped.toList
      .map { case (bagId, time, doi) => addBagInfo(bagId, baseId, time, doi) }
      .collectResults shouldBe a[Success[_]]

    for (bagId <- bagIds) {
      getBaseBagId(bagId) should matchPattern { case Success(`baseId`) => }
    }
  }

  it should "return a Failure with a BagIdNotFoundException inside if the bagId is not present in the database" in {
    // Note: the database is empty at this point!
    val someBagId = UUID.randomUUID()
    getBaseBagId(someBagId) should matchPattern { case Failure(BagIdNotFoundException(`someBagId`)) => }
  }

  "getAllBagsWithBase" should "return a sequence with only the baseId when there are no child bags declared" in {
    val bagId = UUID.randomUUID()
    val time = DateTime.now()
    val doi = "10.5072/dans-x6f-kf6x"

    addBagInfo(bagId, bagId, time, doi) shouldBe a[Success[_]]

    inside(getAllBagsWithBase(bagId)) {
      case Success(ids) => ids should (have size 1 and contain only bagId)
    }
  }

  it should "return a sequence with all bagIds with a certain baseId" in {
    val bagIds1@(baseId1 :: _) = List.fill(3)(UUID.randomUUID())
    val times1 = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )
    val dois1 = List("10.5072/dans-x6f-kf6x", "10.5072/dans-x6f-kf66", "10.5072/dans-y7g-lg77")

    val bagIds2@(baseId2 :: _) = List.fill(5)(UUID.randomUUID())
    val times2 = List(
      DateTime.parse("2001-09-11"),
      DateTime.parse("2017"),
      DateTime.parse("2017").plusDays(2),
      DateTime.parse("2017-03-09"),
      DateTime.parse("2018")
    )
    val dois2 = List("10.5072/dans-a1b-cd2e", "10.5072/dans-f3g-hi45", "10.5072/dans-j6k-lm78", "10.5072/dans-n9o-pq01", "10.5072/dans-r2s-tu34")

    List(
      ((bagIds1, times1, dois1).zipped.toList, baseId1),
      ((bagIds2, times2, dois2).zipped.toList, baseId2))
      .flatMap { case (xs, base) => xs.map { case (bagId, time, doi) => addBagInfo(bagId, base, time, doi) }}
      .collectResults shouldBe a[Success[_]]

    inside(getAllBagsWithBase(baseId1)) {
      case Success(ids) => ids should (have size 3 and contain theSameElementsAs bagIds1)
    }
    inside(getAllBagsWithBase(baseId2)) {
      case Success(ids) => ids should (have size 5 and contain theSameElementsAs bagIds2)
    }
  }

  "getBagInfo" should "return the relation object for the given bagId" in {
    val bagIds@(baseId :: _) = List.fill(3)(UUID.randomUUID())
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )
    val dois = List("10.5072/dans-x6f-kf6x", "10.5072/dans-x6f-kf66", "10.5072/dans-y7g-lg77")

    (bagIds, times, dois).zipped.toList
      .map { case (bagId, time, doi) => addBagInfo(bagId, baseId, time, doi) }
      .collectResults shouldBe a[Success[_]]

    for ((bagId, created, doi) <- (bagIds, times, dois).zipped.toList)
      getBagInfo(bagId) should matchPattern { case Success(BagInfo(`bagId`, `baseId`, `created`, `doi`)) => }
  }

  it should "return a BagIdNotFoundException when the given bagId does not exist in the database" in {
    // Note: the database is empty at this point!
    val someBagId = UUID.randomUUID()
    getBagInfo(someBagId) should matchPattern { case Failure(BagIdNotFoundException(`someBagId`)) => }
  }

  "addBagInfo" should "insert a new bag relation into the database" in {
    val bagIds@(baseId :: _) = List.fill(3)(UUID.randomUUID())
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )
    val dois = List("10.5072/dans-x6f-kf6x", "10.5072/dans-x6f-kf66", "10.5072/dans-y7g-lg77")

    (bagIds, times, dois).zipped.toList
      .map { case (bagId, time, doi) => addBagInfo(bagId, baseId, time, doi) }
      .collectResults shouldBe a[Success[_]]

    val rel1 :: rel2 :: rels = (bagIds, times, dois).zipped.toList.map { case (bagId, time, doi) => BagInfo(bagId, baseId, time, doi) }

    inside(getAllBagInfos) {
      case Success(relations) => relations should contain allOf(rel1, rel2, rels: _*)
    }
  }

  it should "fail if inserting a bag relation twice" in {
    val bagId = UUID.randomUUID()
    val baseId = UUID.randomUUID()
    val time = DateTime.now()
    val doi = "10.5072/dans-x6f-kf66"

    val result1 = addBagInfo(bagId, baseId, time, doi)
    val result2 = addBagInfo(bagId, baseId, time, doi)

    result1 shouldBe a[Success[_]]
    inside(result2) {
      case Failure(e) => e.getMessage should include ("UNIQUE constraint failed")
    }

    inside(getAllBagInfos) {
      case Success(relations) => relations should contain (BagInfo(bagId, baseId, time, doi))
    }
  }
}
