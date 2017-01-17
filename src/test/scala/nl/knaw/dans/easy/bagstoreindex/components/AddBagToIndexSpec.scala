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

import nl.knaw.dans.easy.bagstoreindex.{ BagStoreIndexDatabaseFixture, BagIdNotFoundException }

import scala.util.{ Failure, Success }

class AddBagToIndexSpec extends BagStoreIndexDatabaseFixture with AddBagToIndex {

  def addParent(): UUID = {
    val bagId = UUID.randomUUID()

    inside(addBase(bagId)) {
      case Success(superBase) => superBase shouldBe bagId
    }

    inside(getAllBagRelations) {
      case Success(relations) =>
        relations should have size 1
        relations.map { case Record(id, parent, _) => (id, parent) } should contain (bagId, bagId)
    }

    bagId
  }

  def addChildBagId(): (UUID, UUID) = {
    val parentId = addParent()
    val bagId = UUID.randomUUID()

    inside(add(bagId, parentId)) {
      case Success(superBase) => superBase shouldBe parentId
    }

    inside(getAllBagRelations) {
      case Success(relations) =>
        relations should have size 2
        relations.map { case Record(id, parent, _) => (id, parent) } should contain (bagId, parentId)
    }

    (bagId, parentId)
  }

  def addChildBagIdWithSuperParent(): (UUID, UUID, UUID) = {
    val (parentId, superParentId) = addChildBagId()
    val bagId = UUID.randomUUID()

    inside(add(bagId, parentId)) {
      case Success(superBase) => superBase shouldBe superParentId
    }

    inside(getAllBagRelations) {
      case Success(relations) =>
        relations should have size 3
        relations.map { case Record(id, parent, _) => (id, parent) } should contain (bagId, superParentId)
    }

    (bagId, parentId, superParentId)
  }

  "add" should "put a relation from the bagId to itself in the database when no parent is specified" in {
    addParent()
  }

  it should "put a relation from the bagId to its parent in the database when the parent exists in the database" in {
    addChildBagId()
  }

  it should "put a relation from the bagId to a superparent in the database when the given parent hase another parent" in {
    addChildBagIdWithSuperParent()
  }

  it should "fail with a ParentNotFoundException when the specified parent bagId does not exist in the database" in {
    val bagId = UUID.randomUUID()
    val parentId = UUID.randomUUID()

    // assert that the parent is not yet present in the database
    inside(getAllBagRelations) {
      case Success(relations) => relations.map(_.bagId) should not contain parentId
    }

    inside(add(bagId, parentId)) {
      case Failure(BagIdNotFoundException(id)) => id shouldBe parentId
    }
  }
}
