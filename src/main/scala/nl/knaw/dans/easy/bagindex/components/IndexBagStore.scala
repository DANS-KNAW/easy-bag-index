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

import java.nio.file.Path

import nl.knaw.dans.easy.bagindex.{ BagId, BagInfo }

import scala.collection.immutable.Seq
import scala.util.Try

trait IndexBagStore {
  this: BagStoreAccess with BagFacadeComponent with Database =>

  /**
   * Traverse the bagstore and create an index of bag relations based on the bags inside.
   *
   * @return `Success` if the indexing was successful; `Failure` otherwise
   */
  def indexBagStore(): Try[Unit] = {
    // lock db table
    // walk over bagstore and insert each bag into the bag_info table 'as-is'
    //     INSERT INTO bag_info VALUES (<bagId>, <baseId>, <created>);
    // get all base bagIds
    //     SELECT bagId FROM bag_info WHERE bagId = base;
    // perform update query for each base bagId
    //     UPDATE bag_info SET base = '<base bagId>' WHERE bagId IN (WITH RECURSIVE
    //       bags_in_sequence(bag) AS (
    //         VALUES('<base bagId>')
    //           UNION
    //           SELECT bagId FROM bag_info, bags_in_sequence
    //         WHERE bag_info.base=bags_in_sequence.bag
    //       )
    //     SELECT * FROM bags_in_sequence);
    // unlock db table

    for {
      bags: Seq[(BagId, Path)] <- traverse
      infos: Seq[BagInfo] = bags.map { case (bagId, path) =>
        bagFacade.getIndexRelevantBagInfo(path).get match { // TODO is there a better way to fail fast?
          case (Some(baseDir), Some(created)) => BagInfo(bagId, baseDir, created)
          case (None, Some(created)) => BagInfo(bagId, bagId, created)
          case _ => throw new Exception(s"could not index bag $bagId")
        }
      }
      _ <- bulkAddBagInfo(infos)
      bases: Seq[BagId] <- getAllBaseBagIds
      _ <- bulkUpdateBaseIdRecursively(bases)
    } yield ()
  }
}
