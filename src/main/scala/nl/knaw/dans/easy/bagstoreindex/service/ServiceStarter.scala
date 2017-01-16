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
package nl.knaw.dans.easy.bagstoreindex.service

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.daemon.{ Daemon, DaemonContext }

class ServiceStarter extends Daemon with DebugEnhancedLogging {
  var bagStoreIndexService: BagStoreIndexService = _

  def init(context: DaemonContext): Unit = {
    logger.info("Initializing service...")
    bagStoreIndexService = BagStoreIndexService()
    logger.info("Service initialized.")
  }

  def start(): Unit = {
    logger.info("Starting service...")
    bagStoreIndexService.start()
    logger.info("Service started.")
  }

  def stop(): Unit = {
    logger.info("Stopping service...")
    bagStoreIndexService.stop()
  }

  def destroy(): Unit = {
    bagStoreIndexService.destroy
    logger.info("Service stopped.")
  }
}
