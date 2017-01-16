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

import java.nio.file.{ Files, Paths }

import nl.knaw.dans.easy.bagstoreindex.components.Configuration
import org.scalatest.BeforeAndAfter

trait BagStoreIndexFixture extends TestSupportFixture with BeforeAndAfter with Configuration {

  private val dbLocation = testDir.resolve("bag-store-index.db")
  Files.copy(Paths.get(getClass.getClassLoader.getResource("database/bag-store-index.db").toURI), dbLocation)

  val dbDriverClass: String = "org.sqlite.JDBC"
  val dbUrl: String = s"jdbc:sqlite:${dbLocation.toString}"
  val dbUsername = Option.empty[String]
  val dbPassword = Option.empty[String]
}
