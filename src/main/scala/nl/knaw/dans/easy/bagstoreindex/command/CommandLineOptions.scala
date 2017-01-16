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
package nl.knaw.dans.easy.bagstoreindex.command

import java.util.UUID

import nl.knaw.dans.easy.bagstoreindex.Version
import org.apache.commons.configuration.PropertiesConfiguration
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.rogach.scallop.{ ScallopConf, ScallopOption, Subcommand, singleArgConverter }

class CommandLineOptions(args: Array[String], properties: PropertiesConfiguration) extends ScallopConf(args) {
  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  printedName = "easy-bag-store-index"
  private val _________ = " " * printedName.length
  private val SUBCOMMAND_SEPARATOR = "---\n"
  version(s"$printedName v${Version()}")
  banner(
    s"""
      |Index a BagStore
      |
      |Usage:
      |
      |$printedName \\
      |${_________}  | add --parent|-p --timestamp|-t <bagId>
      |
      |Options:
    """.stripMargin)

  private implicit val uuidConverter = singleArgConverter[UUID](UUID.fromString)
  private implicit val dateTimeConverter = singleArgConverter[DateTime](DateTime.parse)

  val add = new Subcommand("add") {
    descr("Adds a bag identifier to the index")
    val bagId: ScallopOption[UUID] = trailArg[UUID](name = "bagId",
      descr = "the bag identifier to be added")
    val parentId: ScallopOption[UUID] = opt[UUID](name = "parent",
      descr = "the bag identifier of the parent bag",
      short = 'p')
    val timestamp: ScallopOption[DateTime] = opt[DateTime](name = "timestamp",
      descr = "the timestamp of the creation of the bag",
      short = 't')
  }

  addSubcommand(add)

  footer("")
}
object CommandLineOptions {
  def apply(args: Array[String], properties: PropertiesConfiguration): CommandLineOptions = new CommandLineOptions(args, properties)
}
