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

import nl.knaw.dans.easy.bagstoreindex.{ BagId, ParentNotFoundException }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.joda.time.DateTime

import scala.util.{ Failure, Try }

trait AddBagToIndex {
  this: Database with DebugEnhancedLogging =>

  /**
   * Inserts a base bagId into the index; that is, the bagId specifies itself as its parent.
   *
   * @param bagId the bagId to be added to the index
   * @param timestamp the timestamp corresponding to the bagId
   * @return `Success` if the bagId was added to the index; `Failure` otherwise
   */
  def addBase(bagId: BagId, timestamp: Option[DateTime] = None): Try[Unit] = {
    trace((bagId, timestamp))
    addBagRelation(bagId, bagId, timestamp.getOrElse(DateTime.now()))
  }

  /**
   * Insert a bagId into the index, given another bagId as its parent.
   * If the parent is already in the index with another base bagId, this super-parent is used
   * as the base bagId of the currently added bagId instead.
   * If the parent does not exist in the index a `ParentNotFoundException` is returned.
   *
   * @param bagId the bagId to be added to the index
   * @param baseId the base of this bagId
   * @param timestamp the timestamp corresponding to the bagId
   * @return `Success` if the bagId was added to the index; `Failure` otherwise
   */
  def add(bagId: BagId, baseId: BagId, timestamp: Option[DateTime] = None): Try[Unit] = {
    trace((bagId, baseId, timestamp))
    for {
      maybeSuperBase <- getBaseBagId(baseId)
      _ <- maybeSuperBase.map(addBagRelation(bagId, _, timestamp.getOrElse(DateTime.now()))).getOrElse(Failure(ParentNotFoundException(baseId)))
    } yield ()
  }
}
