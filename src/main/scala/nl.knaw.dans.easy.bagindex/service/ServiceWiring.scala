/*
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.bagindex.service

import java.nio.file.Paths

import nl.knaw.dans.easy.bagindex.ConfigurationComponent
import nl.knaw.dans.easy.bagindex.access.AccessWiring
import nl.knaw.dans.easy.bagindex.components.IndexWiring
import nl.knaw.dans.easy.bagindex.server.ServerWiring

trait ServiceWiring extends ServerWiring
  with AccessWiring
  with IndexWiring
  with ConfigurationComponent {
  lazy val configuration: Configuration = Configuration(Paths.get(System.getProperty("app.home")))
}
