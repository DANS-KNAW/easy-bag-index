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
import java.util.UUID

import gov.loc.repository.bagit.{ Bag, BagFactory }
import nl.knaw.dans.easy.bagindex.{ BagNotFoundException, BaseId, NoBagInfoFoundException, dateTimeFormatter }
import org.joda.time.DateTime

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.util.{ Failure, Success, Try }

trait BagFacadeComponent {

  val bagFacade: BagFacade

  trait BagFacade {
    def getIndexRelevantBagInfo(bagDir: Path): Try[(Option[BaseId], Option[DateTime])]

    def getBagInfo(bagDir: Path): Try[Map[String, String]]
  }
}

trait Bagit4FacadeComponent extends BagFacadeComponent {
  class Bagit4Facade(bagFactory: BagFactory = new BagFactory) extends BagFacade {

    val IS_VERSION_OF = "Is-Version-Of"
    val CREATED = "Created"

    def getIndexRelevantBagInfo(bagDir: Path): Try[(Option[BaseId], Option[DateTime])] = {
      for {
        info <- getBagInfo(bagDir)
        baseId = info.get(IS_VERSION_OF).map(UUID.fromString)
        created = info.get(CREATED).map(DateTime.parse(_, dateTimeFormatter))
      } yield (baseId, created)
    }

    def getBagInfo(bagDir: Path): Try[Map[String, String]] = {
      for {
        bag <- getBag(bagDir)
        info <- Option(bag.getBagInfoTxt) // this call returns null if there is not bag-info.txt
          .map(map => Success(map.asScala.toMap))
          .getOrElse(Failure(NoBagInfoFoundException(bagDir)))
      } yield info
    }

    private def getBag(bagDir: Path): Try[Bag] = Try {
      bagFactory.createBag(bagDir.toFile, BagFactory.Version.V0_97, BagFactory.LoadOption.BY_MANIFESTS)
    }.recoverWith { case cause => Failure(BagNotFoundException(bagDir, cause)) }
  }
}
