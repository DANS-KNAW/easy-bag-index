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
package nl.knaw.dans.easy.bagindex

import org.scalatest.matchers.{ MatchResult, Matcher }

import scala.xml.{ Node, PrettyPrinter, XML }

trait CustomMatchers {

  // copied from easy-split-multi-deposit
  class EqualTrimmedMatcher(right: Iterable[Node]) extends Matcher[Iterable[Node]] {
    private val pp = new PrettyPrinter(160, 2)

    private def prepForTest(n: Node): Node = XML.loadString(pp.format(n))

    override def apply(left: Iterable[Node]): MatchResult = {
      val prettyLeft = left.map(prepForTest)
      val prettyRight = right.map(prepForTest)

      lazy val prettyLeftString = prettyLeft.mkString("\n")
      lazy val prettyRightString = prettyRight.mkString("\n")

      MatchResult(
        prettyLeft == prettyRight,
        s"$prettyLeftString was not equal to $prettyRightString",
        s"$prettyLeftString was equal to $prettyRightString"
      )
    }
  }
  def equalTrimmed(right: Iterable[Node]) = new EqualTrimmedMatcher(right)
}
