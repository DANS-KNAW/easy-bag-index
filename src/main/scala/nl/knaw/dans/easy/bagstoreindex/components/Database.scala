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

import java.sql.{ Connection, DriverManager }
import java.util.UUID

import nl.knaw.dans.easy.bagstoreindex.BagId
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.util.Try

trait Database {
  this: Configuration with DebugEnhancedLogging =>
  import logger._

  var connection: Connection = _

  def initConnection(): Unit = {
    info("Creating database connection ...")

    Class.forName(dbDriverClass)

    val optConn = for {
      username <- dbUsername
      password <- dbPassword
    } yield DriverManager.getConnection(dbUrl, username, password)

    connection = optConn.getOrElse(DriverManager.getConnection(dbUrl))

    info(s"Database connected with $dbUrl.")
  }

  /**
   * Close the database's connection.
   *
   * @return `Success` if the closing went well, `Failure` otherwise
   */
  def closeConnection(): Try[Unit] = Try {
    info("Closing database connection ...")
    connection.close()
    info("Database connection closed")
  }

  /**
   * Return the base bagId of the given bagId if the latter exists (wrapped in an `Option`).
   * Otherwise a `None` is returned.
   *
   * @param bagId the bagId for which the base bagId needs to be returned
   * @return the base bagId of the given bagId
   */
  def getBaseBagId(bagId: BagId) = Try {
    trace(bagId)
    val prepStatement = connection.prepareStatement("SELECT base FROM BagRelation WHERE bagId=?;")
    prepStatement.setString(1, bagId.toString)
    prepStatement.closeOnCompletion()
    val resultSet = prepStatement.executeQuery()
    val base = Option(resultSet.next())
      .filter(b => b)
      .map(_ => UUID.fromString(resultSet.getString("base")))
    resultSet.close()
    base
  }

  /**
   * Add a bag relation to the database. A bag relation consists of a unique bagId (that is not yet
   * included in the database), a base bagId and a timestamp.
   *
   * @param bagId the unique bag identifier
   * @param base the base bagId of the bagId
   * @param timestamp the date/time at which the bag was created
   * @return `Success` if the bag relation was added successfully; `Failure` otherwise
   */
  def addBagRelation(bagId: BagId, base: BagId, timestamp: DateTime): Try[Unit] = Try {
    trace((bagId, base, timestamp))
    val prepStatement = connection.prepareStatement("INSERT INTO BagRelation VALUES (?, ?, ?);")
    prepStatement.setString(1, bagId.toString)
    prepStatement.setString(2, base.toString)
    prepStatement.setString(3, timestamp.toString(ISODateTimeFormat.dateTime()))
    prepStatement.closeOnCompletion()
    prepStatement.executeUpdate()
  }
}
