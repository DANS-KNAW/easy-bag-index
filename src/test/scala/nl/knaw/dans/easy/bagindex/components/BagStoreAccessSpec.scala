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

  "traverse" should "list all bags in the bagstore" in {
    val uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val uuid2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
    val uuid3 = UUID.fromString("00000000-0000-0000-0000-000000000003")

    val path1 = bagStoreBaseDir.resolve("00/000000000000000000000000000001/bag-revision-1")
    val path2 = bagStoreBaseDir.resolve("00/000000000000000000000000000002/bag-revision-2")
    val path3 = bagStoreBaseDir.resolve("00/000000000000000000000000000003/bag-revision-3")

    inside(traverse) {
      case Success(stream) => stream.toList should (have size 3 and
        contain only((uuid1, path1), (uuid2, path2), (uuid3, path3)))
    }
  }
}
