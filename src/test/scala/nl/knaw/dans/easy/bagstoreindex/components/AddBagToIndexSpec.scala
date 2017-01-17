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

import nl.knaw.dans.easy.bagstoreindex.{ BagIdNotFoundException, BagStoreIndexDatabaseFixture, Relation }

import scala.util.{ Failure, Success }

class AddBagToIndexSpec extends BagStoreIndexDatabaseFixture with AddBagToIndex {

  def addBaseTest(): UUID = {
    val bagId = UUID.randomUUID()

    inside(addBase(bagId)) {
      case Success(superBase) => superBase shouldBe bagId
    }

    inside(getAllBagRelations) {
      case Success(relations) =>
        relations should have size 1
        relations.map { case Relation(id, base, _) => (id, base) } should contain (bagId, bagId)
    }

    bagId
  }

  def addChildBagIdTest(): (UUID, UUID) = {
    val baseId = addBaseTest()
    val bagId = UUID.randomUUID()

    inside(add(bagId, baseId)) {
      case Success(superBase) => superBase shouldBe baseId
    }

    inside(getAllBagRelations) {
      case Success(relations) =>
        relations should have size 2
        relations.map { case Relation(id, base, _) => (id, base) } should contain (bagId, baseId)
    }

    (bagId, baseId)
  }

  def addChildBagIdWithSuperBaseTest(): (UUID, UUID, UUID) = {
    val (baseId, superBaseId) = addChildBagIdTest()
    val bagId = UUID.randomUUID()

    inside(add(bagId, baseId)) {
      case Success(superBase) => superBase shouldBe superBaseId
    }

    inside(getAllBagRelations) {
      case Success(relations) =>
        relations should have size 3
        relations.map { case Relation(id, base, _) => (id, base) } should contain (bagId, superBaseId)
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
    inside(getAllBagRelations) {
      case Success(relations) => relations.map(_.bagId) should not contain baseId
    }

    inside(add(bagId, baseId)) {
      case Failure(BagIdNotFoundException(id)) => id shouldBe baseId
    }
  }
}
