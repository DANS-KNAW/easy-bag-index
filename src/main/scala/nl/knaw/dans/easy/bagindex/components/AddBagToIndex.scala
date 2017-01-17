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

import nl.knaw.dans.easy.bagindex.{ BagId, BaseId }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.joda.time.DateTime

import scala.util.Try

trait AddBagToIndex {
  this: Database with DebugEnhancedLogging =>

  /**
   * Inserts a baseId into the index; that is, the bagId specifies itself as its base.
   *
   * @param bagId the bagId to be added to the index
   * @param timestamp the timestamp corresponding to the bagId
   * @return `Success` if the bagId was added to the index; `Failure` otherwise
   */
  def addBase(bagId: BagId, timestamp: Option[DateTime] = None): Try[BaseId] = {
    trace((bagId, timestamp))
    addBagRelation(bagId, bagId, timestamp.getOrElse(DateTime.now()))
      .map(_ => bagId)
  }

  /**
   * Insert a bagId into the index, given another bagId as its base.
   * If the baseId is already in the index with another baseId, this ''super-baseId'' is used
   * as the baseId of the currently added bagId instead.
   * If the baseId does not exist in the index a `BagIdNotFoundException` is returned.
   *
   * @param bagId the bagId to be added to the index
   * @param baseId the base of this bagId
   * @param timestamp the timestamp corresponding to the bagId
   * @return the baseId of the super-base if the bagId was added to the index; `Failure` otherwise
   */
  def add(bagId: BagId, baseId: BaseId, timestamp: Option[DateTime] = None): Try[BaseId] = {
    trace((bagId, baseId, timestamp))
    for {
      superBase <- getBaseBagId(baseId)
      _ <- addBagRelation(bagId, superBase, timestamp.getOrElse(DateTime.now()))
    } yield superBase
  }
}
