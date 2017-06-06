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

import nl.knaw.dans.easy.bagindex.{ BagIdNotFoundException, BagIndexDatabaseFixture, BagInfo }

import scala.util.{ Failure, Success }

class AddBagToIndexSpec extends BagIndexDatabaseFixture with AddBagToIndex {

  def addBaseTest(): UUID = {
    val bagId = UUID.randomUUID()

    inside(addBase(bagId, doi = testDoi)) {
      case Success(superBase) => superBase shouldBe bagId
    }

    inside(getAllBagInfos) {
      case Success(relations) => relations.map { case BagInfo(id, base, _, _) => (id, base) } should contain ((bagId, bagId))
    }

    bagId
  }

  def addChildBagIdTest(): (UUID, UUID) = {
    val baseId = addBaseTest()
    val bagId = UUID.randomUUID()
    val doi = testDoi.replaceAll("6", "7")

    add(bagId, baseId, doi = doi) should matchPattern { case Success(`baseId`) => }

    inside(getAllBagInfos) {
      case Success(relations) => relations.map { case BagInfo(id, base, _, _) => (id, base) } should contain ((bagId, baseId))
    }

    (bagId, baseId)
  }

  def addChildBagIdWithSuperBaseTest(): (UUID, UUID, UUID) = {
    val (baseId, superBaseId) = addChildBagIdTest()
    val bagId = UUID.randomUUID()
    val doi = testDoi.replaceAll("6", "8")

    add(bagId, baseId, doi = doi) should matchPattern { case Success(`superBaseId`) => }

    inside(getAllBagInfos) {
      case Success(relations) => relations.map { case BagInfo(id, base, _, _) => (id, base) } should contain ((bagId, superBaseId))
    }

    (bagId, baseId, superBaseId)
  }

  "add" should "put a relation from the bagId to itself in the database when no base is specified" in {
    addBaseTest()
  }

  it should "put a relation from the bagId to its base in the database when the baseId exists in the database" in {
    addChildBagIdTest()
  }

  it should "put a relation from the bagId to a super-baseId in the database when the given baseId hase another baseId" in {
    addChildBagIdWithSuperBaseTest()
  }

  it should "fail with a BagIdNotFoundException when the specified baseId does not exist in the database" in {
    val bagId = UUID.randomUUID()
    val baseId = UUID.randomUUID()

    // assert that the base is not yet present in the database
    inside(getAllBagInfos) {
      case Success(relations) => relations.map(_.bagId) should not contain baseId
    }

    add(bagId, baseId, doi = testDoi) should matchPattern { case Failure(BagIdNotFoundException(`baseId`)) => }
  }
}
