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

import nl.knaw.dans.easy.bagstoreindex.{ BagId, BagIdNotFoundException, BaseId, Relation }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.collection.immutable.Seq
import scala.util.Try

trait Database {
  this: DebugEnhancedLogging =>
  import logger._

  private var connection: Connection = _

  val dbDriverClass: String
  val dbUrl: String
  val dbUsername: Option[String]
  val dbPassword: Option[String]

  /**
   * Establishes the connection with the database
   */
  def initConnection(): Try[Unit] = Try {
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
   * Return the baseId of the given bagId if the latter exists.
   * If the bagId does not exist, a `BagIdNotFoundException` is returned.
   *
   * @param bagId the bagId for which the base bagId needs to be returned
   * @return the baseId of the given bagId if it exists; failure otherwise
   */
  def getBaseBagId(bagId: BagId): Try[BaseId] = Try {
    trace(bagId)
    val prepStatement = connection.prepareStatement("SELECT base FROM BagRelation WHERE bagId=?;")
    prepStatement.setString(1, bagId.toString)
    prepStatement.closeOnCompletion()
    val resultSet = prepStatement.executeQuery()
    val base = if (resultSet.next())
                 UUID.fromString(resultSet.getString("base"))
               else
                 throw BagIdNotFoundException(bagId)
    resultSet.close()
    base
  }

  // TODO test, document and use
  def getBagsWithBase(baseId: BaseId): Try[Seq[BagId]] = Try {
    trace(baseId)
    val prepStatement = connection.prepareStatement("SELECT bagId FROM BagRelation WHERE baseId=? ORDER BY timestamp;")
    prepStatement.setString(1, baseId.toString)
    prepStatement.closeOnCompletion()
    val resultSet = prepStatement.executeQuery()
    val result: Seq[BagId] = Stream.continually(resultSet.next())
      .takeWhile(b => b)
      .map(_ => UUID.fromString(resultSet.getString("bagId")))
    resultSet.close()
    result
  }

  /**
   * Returns a list of all bag relations that are present in the database.
   * '''Warning:''' this may load large amounts of data into memory.
   *
   * @return a list of all bag relations
   */
  def getAllBagRelations: Try[List[Relation]] = Try {
    val statement = connection.createStatement
    statement.closeOnCompletion()
    val resultSet = statement.executeQuery("SELECT * FROM BagRelation;")

    val result = Stream.continually(resultSet.next())
      .takeWhile(b => b)
      .map(_ => Relation(
        bagId = UUID.fromString(resultSet.getString("bagId")),
        baseId = UUID.fromString(resultSet.getString("base")),
        timestamp = DateTime.parse(resultSet.getString("timestamp"), ISODateTimeFormat.dateTime())))
      .toList

    resultSet.close()

    result
  }

  /**
   * Add a bag relation to the database. A bag relation consists of a unique bagId (that is not yet
   * included in the database), a base bagId and a timestamp.
   *
   * @param bagId the unique bag identifier
   * @param baseId the base bagId of the bagId
   * @param timestamp the date/time at which the bag was created
   * @return `Success` if the bag relation was added successfully; `Failure` otherwise
   */
  def addBagRelation(bagId: BagId, baseId: BaseId, timestamp: DateTime): Try[Unit] = Try {
    trace((bagId, baseId, timestamp))
    val prepStatement = connection.prepareStatement("INSERT INTO BagRelation VALUES (?, ?, ?);")
    prepStatement.setString(1, bagId.toString)
    prepStatement.setString(2, baseId.toString)
    prepStatement.setString(3, timestamp.toString(ISODateTimeFormat.dateTime()))
    prepStatement.closeOnCompletion()
    prepStatement.executeUpdate()
  }
}
