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
package nl.knaw.dans.easy.bagstoreindex.components

import java.util.UUID

import nl.knaw.dans.easy.bagstoreindex.BagStoreIndexDatabaseFixture
import nl.knaw.dans.lib.error.TraversableTryExtensions
import org.joda.time.DateTime

import scala.util.{ Failure, Success }

class DatabaseSpec extends BagStoreIndexDatabaseFixture with Database {

  "addBagRelation" should "insert a new bag relation into the database" in {
    val bagIds = List.fill(3)(UUID.randomUUID())
    val base = bagIds.head
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )

    bagIds.zip(times)
      .map { case (bagId, time) => addBagRelation(bagId, base, time) }
      .collectResults shouldBe a[Success[_]]

    inside(getAllBagRelations) {
      case Success(relations) =>
        bagIds.zip(times)
          .map { case (bagId, time) => Record(bagId, base, time) }
          .forall(relations.contains) shouldBe true
    }
  }

  it should "fail if inserting a bag relation twice" in {
    val bagId = UUID.randomUUID()
    val base = UUID.randomUUID()
    val time = DateTime.now()

    val result1 = addBagRelation(bagId, base, time)
    val result2 = addBagRelation(bagId, base, time)

    result1 shouldBe a[Success[_]]
    inside(result2) {
      case Failure(e) => e.getMessage should include ("UNIQUE constraint failed")
    }

    inside(getAllBagRelations) {
      case Success(relations) =>
        relations should have size 1
        relations.head shouldBe Record(bagId, base, time)
    }
  }

  "getBaseBagId" should "return the base of a specific bagId" in {
    val bagIds = List.fill(3)(UUID.randomUUID())
    val base = bagIds.head
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )

    bagIds.zip(times)
      .map { case (bagId, time) => addBagRelation(bagId, base, time) }
      .collectResults shouldBe a[Success[_]]

    for (bagId <- bagIds) {
      inside(getBaseBagId(bagId)) {
        case Success(Some(uuid)) => uuid shouldBe base
      }
    }
  }

  it should "return a Success with a None inside if the bagId is not present in the database" in {
    val bagId = UUID.randomUUID()
    val base = UUID.randomUUID()
    val time = DateTime.now()

    addBagRelation(bagId, base, time) shouldBe a[Success[_]]

    val someOtherBagId = UUID.randomUUID()
    inside(getBaseBagId(someOtherBagId)) {
      case Success(maybeBase) => maybeBase shouldBe empty
    }
  }
}
