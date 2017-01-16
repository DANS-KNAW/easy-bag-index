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

import nl.knaw.dans.easy.bagstoreindex.{ BagStoreIndexApp, TryExtensions }

import scala.language.reflectiveCalls
import scala.util.Try

object Command extends App with BagStoreIndexApp {

  initConnection()

  val opts = CommandLineOptions(args, properties)
  opts.verify()

  // TODO continue with the rest (command parsing etc.)

  val result: Try[String] = opts.subcommand match {
    case Some(cmd @ opts.add) =>
      val bagId = cmd.bagId()
      val parentBagId = cmd.parentId.toOption
      val timestamp = cmd.timestamp.toOption

      // TODO better info teruggeven wanneer sprake is van een super-base-bagId
      parentBagId.map(base => add(bagId, base, timestamp).map(_ => s"Added bagId $bagId with base $base"))
        .getOrElse(addBase(bagId, timestamp).map(_ => s"Added bagId $bagId as base"))
    case _ => throw new IllegalArgumentException(s"Unknown command: ${opts.subcommand}")
      Try { "Unknown command" }
  }

  result.map(msg => println(s"OK: $msg"))
    .onError(e => println(s"FAILED: ${e.getMessage}"))

  closeConnection()
}
