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

import nl.knaw.dans.easy.bagindex._
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

    bagIds.zip(times)
      .map { case (bagId, time) => addBagRelation(bagId, baseId, time) }
      .collectResults shouldBe a[Success[_]]

    for (bagId <- bagIds) {
      inside(getBaseBagId(bagId)) {
        case Success(base) => base shouldBe baseId
      }
    }
  }

  it should "return a Failure with a BagIdNotFoundException inside if the bagId is not present in the database" in {
    // Note: the database is empty at this point!
    val someBagId = UUID.randomUUID()
    inside(getBaseBagId(someBagId)) {
      case Failure(BagIdNotFoundException(id)) => id shouldBe someBagId
    }
  }

  "getAllBagsWithBase" should "return a sequence with only the baseId when there are no child bags declared" in {
    val bagId = UUID.randomUUID()
    val time = DateTime.now()

    addBagRelation(bagId, bagId, time) shouldBe a[Success[_]]

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

    val bagIds2@(baseId2 :: _) = List.fill(5)(UUID.randomUUID())
    val times2 = List(
      DateTime.parse("2001-09-11"),
      DateTime.parse("2017"),
      DateTime.parse("2017").plusDays(2),
      DateTime.parse("2017-03-09"),
      DateTime.parse("2018")
    )

    List(
      (bagIds1.zip(times1), baseId1),
      (bagIds2.zip(times2), baseId2))
      .flatMap { case (xs, base) => xs.map { case (bagId, time) => addBagRelation(bagId, base, time) }}
      .collectResults shouldBe a[Success[_]]

    inside(getAllBagsWithBase(baseId1)) {
      case Success(ids) => ids should (have size 3 and contain theSameElementsAs bagIds1)
    }
    inside(getAllBagsWithBase(baseId2)) {
      case Success(ids) => ids should (have size 5 and contain theSameElementsAs bagIds2)
    }
  }

  "getBagRelation" should "return the relation object for the given bagId" in {
    val bagIds@(baseId :: _) = List.fill(3)(UUID.randomUUID())
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )

    bagIds.zip(times)
      .map { case (bagId, time) => addBagRelation(bagId, baseId, time) }
      .collectResults shouldBe a[Success[_]]

    for ((bagId, created) <- bagIds.zip(times)) {
      inside(getBagRelation(bagId)) {
        case Success(BagRelation(bag, base, time)) =>
          bag shouldBe bagId
          base shouldBe baseId
          time shouldBe created
      }
    }
  }

  it should "return a BagIdNotFoundException when the given bagId does not exist in the database" in {
    // Note: the database is empty at this point!
    val someBagId = UUID.randomUUID()
    inside(getBagRelation(someBagId)) {
      case Failure(BagIdNotFoundException(id)) => id shouldBe someBagId
    }
  }

  "addBagRelation" should "insert a new bag relation into the database" in {
    val bagIds@(baseId :: _) = List.fill(3)(UUID.randomUUID())
    val times = List(
      DateTime.parse("1992-07-30T16:00:00"),
      DateTime.parse("2004-01-01"),
      DateTime.now()
    )

    bagIds.zip(times)
      .map { case (bagId, time) => addBagRelation(bagId, baseId, time) }
      .collectResults shouldBe a[Success[_]]

    val rel1 :: rel2 :: rels = bagIds.zip(times).map { case (bagId, time) => BagRelation(bagId, baseId, time) }

    inside(getAllBagRelations) {
      case Success(relations) => relations should contain allOf(rel1, rel2, rels: _*)
    }
  }

  it should "fail if inserting a bag relation twice" in {
    val bagId = UUID.randomUUID()
    val baseId = UUID.randomUUID()
    val time = DateTime.now()

    val result1 = addBagRelation(bagId, baseId, time)
    val result2 = addBagRelation(bagId, baseId, time)

    result1 shouldBe a[Success[_]]
    inside(result2) {
      case Failure(e) => e.getMessage should include ("UNIQUE constraint failed")
    }

    inside(getAllBagRelations) {
      case Success(relations) => relations should contain (BagRelation(bagId, baseId, time))
    }
  }

  def setupBagStoreIndexTestCase(): Map[Char, BagId] = {
    // sequence with base F
    val bagIdA = UUID.randomUUID()
    val bagIdB = UUID.randomUUID()
    val bagIdC = UUID.randomUUID()
    val bagIdD = UUID.randomUUID()
    val bagIdE = UUID.randomUUID()
    val bagIdF = UUID.randomUUID()

    // sequence with base Z
    val bagIdX = UUID.randomUUID()
    val bagIdY = UUID.randomUUID()
    val bagIdZ = UUID.randomUUID()

    def time = DateTime.now()

    val relations = BagRelation(bagIdA, bagIdE, time) ::
      BagRelation(bagIdB, bagIdA, time) ::
      BagRelation(bagIdC, bagIdA, time) ::
      BagRelation(bagIdD, bagIdB, time) ::
      BagRelation(bagIdE, bagIdF, time) ::
      BagRelation(bagIdF, bagIdF, time) ::
      BagRelation(bagIdX, bagIdY, time) ::
      BagRelation(bagIdY, bagIdZ, time) ::
      BagRelation(bagIdZ, bagIdZ, time) :: Nil

    bulkAddBagRelation(relations) shouldBe a[Success[_]]

    Map(
      'a' -> bagIdA,
      'b' -> bagIdB,
      'c' -> bagIdC,
      'd' -> bagIdD,
      'e' -> bagIdE,
      'f' -> bagIdF,
      'x' -> bagIdX,
      'y' -> bagIdY,
      'z' -> bagIdZ
    )
  }

  "getAllBaseBagIds" should "return a sequence of bagIds refering to bags that are the base of their sequence" in {
    val bags = setupBagStoreIndexTestCase()

    inside(getAllBaseBagIds) {
      case Success(bases) => bases should contain allOf(bags('f'), bags('z'))
    }
  }

  "bulkAddBagRelation" should "insert multiple bag relations into the database in one commit" in {
    // the call to bulkAddBagRelation is done during the setup process
    val bag1 :: bag2 :: tail = setupBagStoreIndexTestCase().values.toList

    inside(getAllBagRelations) {
      case Success(rels) => rels.map(_.bagId) should contain allOf(bag1, bag2, tail:_*)
    }
  }

  // TODO not sure how to test failure and rollback for bulkAddBagRelation

  "updateBaseIdRecursively" should "set all baseIds in the index to the base bagId of the sequence" in {
    val bags = setupBagStoreIndexTestCase()

    updateBaseIdRecursively(bags('f')) shouldBe a[Success[_]]

    inside(getAllBagRelations) {
      case Success(rels) => rels.map(rel => (rel.bagId, rel.baseId)) should contain allOf(
        (bags('a'), bags('f')),
        (bags('b'), bags('f')),
        (bags('c'), bags('f')),
        (bags('d'), bags('f')),
        (bags('e'), bags('f')),
        (bags('f'), bags('f')),
        // x, y and z should be untouched
        (bags('x'), bags('y')),
        (bags('y'), bags('z')),
        (bags('z'), bags('z'))
      )
    }
  }

  "bulkUpdateBaseIdRecursively" should "set all baseIds in the index to the base bagId of the sequence, for each element in the collection of bagIds" in {
    val bags = setupBagStoreIndexTestCase()

    bulkUpdateBaseIdRecursively(bags('f') :: bags('z') :: Nil) shouldBe a[Success[_]]

    inside(getAllBagRelations) {
      case Success(rels) => rels.map(rel => (rel.bagId, rel.baseId)) should contain allOf(
        (bags('a'), bags('f')),
        (bags('b'), bags('f')),
        (bags('c'), bags('f')),
        (bags('d'), bags('f')),
        (bags('e'), bags('f')),
        (bags('f'), bags('f')),
        (bags('x'), bags('z')),
        (bags('y'), bags('z')),
        (bags('z'), bags('z'))
      )
    }
  }

  // TODO not sure how to test failure and rollback for bulkUpdateBaseIdRecursively
}
