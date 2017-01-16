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

import java.io.File

import nl.knaw.dans.easy.bagstoreindex.components.{ AddBagToIndex, Configuration, Database }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration

trait BagStoreIndexApp extends AddBagToIndex
  with Database
  with Configuration
  with DebugEnhancedLogging {

  val properties = new PropertiesConfiguration(new File(System.getProperty("app.home"), "cfg/application.properties"))

  val dbDriverClass: String = properties.getString("bag-store-index.database.driver-class")
  val dbUrl: String = properties.getString("bag-store-index.database.url")
  val dbUsername: Option[String] = Option(properties.getString("bag-store-index.database.username"))
  val dbPassword: Option[String] = Option(properties.getString("bag-store-index.database.password"))

  def validateSettings(): Unit = {
    // TODO some asserts to validate basic settings
  }
}
