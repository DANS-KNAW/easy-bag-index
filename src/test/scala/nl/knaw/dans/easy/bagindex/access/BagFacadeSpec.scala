/**
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
package nl.knaw.dans.easy.bagindex.access

import java.nio.file.Paths

import nl.knaw.dans.easy.bagindex.{ BagStoreFixture, Bagit5Fixture, NoIdentifierFoundException, TestSupportFixture }

import scala.util.{ Failure, Success }
import scala.xml.transform.{ RewriteRule, RuleTransformer }
import scala.xml.{ Elem, Node, NodeSeq, XML }

class BagFacadeSpec extends TestSupportFixture with BagStoreFixture with Bagit5Fixture {

  private val bagStoreBaseDir = bagStore.baseDirs.headOption.getOrElse(throw new NoSuchElementException("no bagstore base directory found"))

  "getDoi" should "find the DOI identifier in a metadata/dataset.xml file" in {
    val doi = doiMap("00000000-0000-0000-0000-000000000001")
    bagFacade.getDoi(bagStoreBaseDir.resolve("00/000000000000000000000000000001/bag-revision-1/metadata/dataset.xml")) should matchPattern { case Success(`doi`) => }
  }

  it should "return empty string if the dataset.xml file did not contain a DOI identifier" in {
    val datasetXML = bagStoreBaseDir.resolve("00/000000000000000000000000000001/bag-revision-1/metadata/dataset.xml")
    val modifiedXML = Paths.get(datasetXML.toString + "_modified")

    object RemoveDOI extends RewriteRule {
      override def transform(node: Node): Seq[Node] = node match {
        case Elem(_, "identifier", _, _, _ @ _*) => NodeSeq.Empty
        case n => n
      }
    }

    new RuleTransformer(RemoveDOI)
      .transform(XML.loadFile(datasetXML.toFile))
      .foreach(XML.save(modifiedXML.toString, _))

    bagFacade.getDoi(modifiedXML) should matchPattern { case Success("") => }
  }

  "getUrn" should "find the URN identifier in a metadata/dataset.xml file" in {
    val urn = "urn:isan:0000-0000-2CEA-0000-1-0000-0000-Y"
    bagFacade.getUrn(bagStoreBaseDir.resolve("00/000000000000000000000000000001/bag-revision-1/metadata/dataset.xml")) should matchPattern { case Success(`urn`) => }
  }

  it should "fail if the dataset.xml file did not contain a URN identifier" in {
    val datasetXML = bagStoreBaseDir.resolve("00/000000000000000000000000000001/bag-revision-1/metadata/dataset.xml")
    val modifiedXML = Paths.get(datasetXML.toString + "_modified")

    object RemoveDOI extends RewriteRule {
      override def transform(node: Node): Seq[Node] = node match {
        case Elem(_, "identifier", _, _, _ @ _*) => NodeSeq.Empty
        case n => n
      }
    }

    new RuleTransformer(RemoveDOI)
      .transform(XML.loadFile(datasetXML.toFile))
      .foreach(XML.save(modifiedXML.toString, _))

    bagFacade.getUrn(modifiedXML) should matchPattern { case Failure(NoIdentifierFoundException("urn", `modifiedXML`)) => }
  }
}
