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

import java.sql.{ Connection, DriverManager }
import java.util.UUID

import nl.knaw.dans.easy.bagindex._
import nl.knaw.dans.lib.error.TraversableTryExtensions
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.joda.time.DateTime
import resource._

import scala.collection.immutable.Seq
import scala.util.{ Failure, Try }

trait Database {
  this: DebugEnhancedLogging =>
  import logger._

  private var connection: Connection = _

  val dbDriverClass: String
  val dbUrl: String
  val dbUsername: Option[String]
  val dbPassword: Option[String]

  /**
   * Hook for creating a connection. If a username and password is provided, these will be taken
   * into account when creating the connection; otherwise the connection is created without
   * username and password.
   *
   * @return the connection
   */
  protected def createConnection: Connection = {
    val optConn = for {
      username <- dbUsername
      password <- dbPassword
    } yield DriverManager.getConnection(dbUrl, username, password)

    optConn.getOrElse(DriverManager.getConnection(dbUrl))
  }

  /**
   * Establishes the connection with the database
   */
  def initConnection(): Try[Unit] = Try {
    info("Creating database connection ...")

    Class.forName(dbDriverClass)
    connection = createConnection

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
  def getBaseBagId(bagId: BagId): Try[BaseId] = {
    trace(bagId)

    val resultSet = for {
      prepStatement <- managed(connection.prepareStatement("SELECT base FROM BagRelation WHERE bagId=?;"))
      _ = prepStatement.setString(1, bagId.toString)
      resultSet <- managed(prepStatement.executeQuery())
    } yield resultSet

    resultSet
      .map(result =>
        if (result.next())
          UUID.fromString(result.getString("base"))
        else
          throw BagIdNotFoundException(bagId))
      .tried
  }

  /**
   * Returns a sequence of all bagIds that have the given baseId as their base, ordered by the 'created' timestamp.
   *
   * @param baseId the baseId used during this search
   * @return a sequence of all bagIds with a given baseId
   */
  def getAllBagsWithBase(baseId: BaseId): Try[Seq[BagId]] = {
    trace(baseId)

    val resultSet = for {
      prepStatement <- managed(connection.prepareStatement("SELECT bagId FROM BagRelation WHERE base=? ORDER BY created;"))
      _ = prepStatement.setString(1, baseId.toString)
      resultSet <- managed(prepStatement.executeQuery())
    } yield resultSet

    resultSet
      .map(result => Stream.continually(result.next())
        .takeWhile(b => b)
        .map(_ => UUID.fromString(result.getString("bagId")))
        .toList)
      .tried
  }

  /**
   * Returns the `Relation` object for the given bagId if it is present in the database.
   * If the bagId does not exist, a `BagIdNotFoundException` is returned.
   *
   * @param bagId the bagId corresponding to the relation
   * @return the relation data of the given bagId
   */
  def getBagRelation(bagId: BagId): Try[BagRelation] = {
    trace(bagId)

    val resultSet = for {
      prepStatement <- managed(connection.prepareStatement("SELECT * FROM BagRelation WHERE bagId=?;"))
      _ = prepStatement.setString(1, bagId.toString)
      resultSet <- managed(prepStatement.executeQuery())
    } yield resultSet

    resultSet
      .map(result =>
        if (result.next())
          BagRelation(
            bagId = UUID.fromString(result.getString("bagId")),
            baseId = UUID.fromString(result.getString("base")),
            created = DateTime.parse(result.getString("created"), dateTimeFormatter))
        else
          throw BagIdNotFoundException(bagId))
      .tried
  }

  /**
   * Returns a sequence of all bag relations that are present in the database.
   * '''Warning:''' this may load large amounts of data into memory.
   *
   * @return a list of all bag relations
   */
  def getAllBagRelations: Try[Seq[BagRelation]] = {
    val resultSet = for {
      statement <- managed(connection.createStatement)
      resultSet <- managed(statement.executeQuery("SELECT * FROM BagRelation;"))
    } yield resultSet

    resultSet
      .map(result => Stream.continually(result.next())
        .takeWhile(b => b)
        .map(_ => BagRelation(
          bagId = UUID.fromString(result.getString("bagId")),
          baseId = UUID.fromString(result.getString("base")),
          created = DateTime.parse(result.getString("created"), dateTimeFormatter)))
        .toList)
      .tried
  }

  /**
   * Return a sequence of bagIds refering to bags that are the base of their sequence.
   *
   * @return the bagId of the base of every sequence
   */
  def getAllBaseBagIds: Try[Seq[BagId]] = {
    val resultSet = for {
      statement <- managed(connection.createStatement)
      resultSet <- managed(statement.executeQuery("SELECT bagId FROM BagRelation WHERE bagId = base;"))
    } yield resultSet

    resultSet
      .map(result => Stream.continually(result.next())
        .takeWhile(b => b)
        .map(_ => UUID.fromString(result.getString("bagId")))
        .toList)
      .tried
  }

  /**
   * Add a bag relation to the database. A bag relation consists of a unique bagId (that is not yet
   * included in the database), a base bagId and a 'created' timestamp.
   *
   * @param bagId the unique bag identifier
   * @param baseId the base bagId of the bagId
   * @param created the date/time at which the bag was created
   * @return `Success` if the bag relation was added successfully; `Failure` otherwise
   */
  def addBagRelation(bagId: BagId, baseId: BaseId, created: DateTime): Try[Unit] = {
    trace(bagId, baseId, created)

    managed(connection.prepareStatement("INSERT INTO BagRelation VALUES (?, ?, ?);"))
      .map(prepStatement => {
        prepStatement.setString(1, bagId.toString)
        prepStatement.setString(2, baseId.toString)
        prepStatement.setString(3, created.toString(dateTimeFormatter))
        prepStatement.executeUpdate()
      })
      .tried
      .map(_ => ())
  }

  /**
   * Adds all bag relations in the collection to the index. If any addition fails,
   * the whole addition is rolled back.
   *
   * @param iterable the bag relations to be inserted
   * @return `Success` if the bag relations were added successfully; `Failure` otherwise
   */
  def bulkAddBagRelation(iterable: Iterable[BagRelation]): Try[Unit] = Try {
    connection.setAutoCommit(false)

    val res = iterable
      .map(relation => addBagRelation(relation.bagId, relation.baseId, relation.created))
      .collectResults
      .map(_ => connection.commit())
      .recoverWith {
        case e =>
          connection.rollback()
          Failure(e)
      }

    connection.setAutoCommit(true)

    res
  }.flatten

  /**
   * Set all baseIds in the sequence to the base bagId of the sequence
   *
   * @param base the base bagId to which all baseIds in the sequence should be updated to.
   * @return `Success` if the update was successful; `Failure` otherwise
   */
  def updateBaseIdRecursively(base: BagId): Try[Unit] = {
    trace(base)

    val query =
      """
        |UPDATE BagRelation SET base = ? WHERE bagId IN (WITH RECURSIVE
        |  bags_in_sequence(bag) AS (
        |    VALUES(?)
        |      UNION
        |      SELECT bagId FROM BagRelation, bags_in_sequence
        |    WHERE BagRelation.base=bags_in_sequence.bag
        |  )
        |SELECT * FROM bags_in_sequence);
      """.stripMargin

    managed(connection.prepareStatement(query))
      .map(prepStatement => {
        prepStatement.setString(1, base.toString)
        prepStatement.setString(2, base.toString)
        prepStatement.executeUpdate()
      })
      .tried
      .map(_ => ())
  }

  /**
   * Set all baseIds in the sequence to the base bagId of the sequence, for every baseId in the
   * given collection. If any of the updates fails, the whole update is rolled back.
   *
   * @param iterable the base bagIds to which all baseIds in the sequences should be updated to.
   * @return `Success` if the update was successful; `Failure` otherwise
   */
  def bulkUpdateBaseIdRecursively(iterable: Iterable[BagId]): Try[Unit] = Try {
    connection.setAutoCommit(false)

    val res = iterable
      .map(updateBaseIdRecursively)
      .collectResults
      .map(_ => connection.commit())
      .recoverWith {
        case e =>
          connection.rollback()
          Failure(e)
      }

    connection.setAutoCommit(true)

    res
  }.flatten
}
