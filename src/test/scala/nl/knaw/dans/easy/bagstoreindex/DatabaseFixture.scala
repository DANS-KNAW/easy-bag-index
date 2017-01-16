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
package nl.knaw.dans.easy.bagstoreindex

import java.util.UUID

import nl.knaw.dans.easy.bagstoreindex.components.{ Configuration, Database }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.util.Try

trait DatabaseFixture extends Database with Configuration with DebugEnhancedLogging {

  case class Record(bagId: BagId, parentId: BagId, timestamp: DateTime)

  def getAllBagRelations: Try[List[Record]] = Try {
    val statement = connection.createStatement
    statement.closeOnCompletion()
    val resultSet = statement.executeQuery("SELECT * FROM BagRelation;")

    val result = Stream.continually(resultSet.next())
      .takeWhile(b => b)
      .map(_ => Record(
        bagId = UUID.fromString(resultSet.getString("bagId")),
        parentId = UUID.fromString(resultSet.getString("base")),
        timestamp = DateTime.parse(resultSet.getString("timestamp"), ISODateTimeFormat.dateTime())))
      .toList

    resultSet.close()

    result
  }
}
