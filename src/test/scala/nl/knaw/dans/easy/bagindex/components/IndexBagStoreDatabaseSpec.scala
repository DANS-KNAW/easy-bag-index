package nl.knaw.dans.easy.bagindex.components

import java.util.UUID

import nl.knaw.dans.easy.bagindex.{ BagId, BagIndexDatabaseFixture, BagInfo }
import org.joda.time.DateTime

import scala.collection.immutable.Seq
import scala.util.Success

class IndexBagStoreDatabaseSpec extends BagIndexDatabaseFixture with IndexBagStoreDatabase with Database {

  def setupBagStoreIndexTestCase(): Map[Char, (BagId, DateTime)] = {
    // sequence with first bag F
    val bagIdA = UUID.randomUUID()
    val bagIdB = UUID.randomUUID()
    val bagIdC = UUID.randomUUID()
    val bagIdD = UUID.randomUUID()
    val bagIdE = UUID.randomUUID()
    val bagIdF = UUID.randomUUID()
    val bagIdG = UUID.randomUUID()

    // sequence with first bag Z
    val bagIdX = UUID.randomUUID()
    val bagIdY = UUID.randomUUID()
    val bagIdZ = UUID.randomUUID()

    // dates
    val dateA = DateTime.parse("1992")
    val dateB = DateTime.parse("1995")
    val dateC = DateTime.parse("1998")
    val dateD = DateTime.parse("2001")
    val dateE = DateTime.parse("1989")
    val dateF = DateTime.parse("1986")
    val dateG = DateTime.parse("1983")
    val dateX = DateTime.parse("2016")
    val dateY = DateTime.parse("2015")
    val dateZ = DateTime.parse("2014")

    val relations = BagInfo(bagIdA, bagIdE, dateA) ::
      BagInfo(bagIdB, bagIdA, dateB) ::
      BagInfo(bagIdC, bagIdA, dateC) ::
      BagInfo(bagIdD, bagIdB, dateD) ::
      BagInfo(bagIdE, bagIdF, dateE) ::
      BagInfo(bagIdF, bagIdF, dateF) ::
      BagInfo(bagIdG, bagIdC, dateG) ::
      BagInfo(bagIdX, bagIdY, dateX) ::
      BagInfo(bagIdY, bagIdZ, dateY) ::
      BagInfo(bagIdZ, bagIdZ, dateZ) :: Nil

    bulkAddBagInfo(relations) shouldBe a[Success[_]]

    Map(
      'a' -> (bagIdA, dateA),
      'b' -> (bagIdB, dateB),
      'c' -> (bagIdC, dateC),
      'd' -> (bagIdD, dateD),
      'e' -> (bagIdE, dateE),
      'f' -> (bagIdF, dateF),
      'g' -> (bagIdG, dateG),
      'x' -> (bagIdX, dateX),
      'y' -> (bagIdY, dateY),
      'z' -> (bagIdZ, dateZ)
    )
  }

  "getAllBaseBagIds" should "return a sequence of bagIds refering to bags that are the base of their sequence" in {
    val bags = setupBagStoreIndexTestCase()

    inside(getAllBaseBagIds) {
      case Success(bases) => bases should contain allOf(bags('f')._1, bags('z')._1)
    }
  }

  "getAllBagsInSequence" should "return a sequence of bagIds and date/times of all bags that are in the same sequence as the given bagId" in {
    val bags = setupBagStoreIndexTestCase()
    val (zBags, fBags) = bags.partition { case (c, _) => List('x', 'y', 'z').contains(c) }
    val fBag1 :: fBag2 :: fTail = fBags.values.toList
    val zBag1 :: zBag2 :: zTail = zBags.values.toList

    inside(getAllBagsInSequence(bags('f')._1)) {
      case Success(sequence) => sequence should (have size 7 and contain allOf(fBag1, fBag2, fTail:_*))
    }
    inside(getAllBagsInSequence(bags('z')._1)) {
      case Success(sequence) => sequence should (have size 3 and contain allOf(zBag1, zBag2, zTail:_*))
    }
  }

  "clearIndex" should "delete all data from the bag-index" in {
    inside(getAllBagInfos) {
      case Success(data) => data should not be empty
    }

    clearIndex() shouldBe a[Success[_]]

    inside(getAllBagInfos) {
      case Success(data) => data shouldBe empty
    }
  }

  it should "succeed if clearing an empty bag-index" in {
    inside(getAllBagInfos) {
      case Success(data) => data should not be empty
    }

    clearIndex() shouldBe a[Success[_]]
    clearIndex() shouldBe a[Success[_]]

    inside(getAllBagInfos) {
      case Success(data) => data shouldBe empty
    }
  }

  "updateBagsInSequence" should "update all bags in the sequence to have the newBaseId as their base in the database" in {
    val bags = setupBagStoreIndexTestCase()

    val fBags = getAllBagsInSequence(bags('f')._1).get.map(_._1)
    updateBagsInSequence(bags('g')._1, fBags) shouldBe a[Success[_]]

    inside(getAllBagInfos) {
      case Success(rels) => rels.map(rel => (rel.bagId, rel.baseId)) should contain allOf(
        (bags('a')._1, bags('g')._1),
        (bags('b')._1, bags('g')._1),
        (bags('c')._1, bags('g')._1),
        (bags('d')._1, bags('g')._1),
        (bags('e')._1, bags('g')._1),
        (bags('f')._1, bags('g')._1),
        (bags('g')._1, bags('g')._1),
        // x, y and z should be untouched
        (bags('x')._1, bags('y')._1),
        (bags('y')._1, bags('z')._1),
        (bags('z')._1, bags('z')._1)
      )
    }
  }

  "bulkUpdateBagsInSequence" should "update all bags in all given sequences to have the newBaseId as their base in the database" in {
    val bags = setupBagStoreIndexTestCase()

    val changes: Seq[(BagId, Seq[BagId])] = List(
      bags('g')._1 -> getAllBagsInSequence(bags('f')._1).get.map(_._1),
      bags('z')._1 -> getAllBagsInSequence(bags('z')._1).get.map(_._1)
    )
    bulkUpdateBagsInSequence(changes) shouldBe a[Success[_]]

    inside(getAllBagInfos) {
      case Success(rels) => rels.map(rel => (rel.bagId, rel.baseId)) should contain allOf(
        (bags('a')._1, bags('g')._1),
        (bags('b')._1, bags('g')._1),
        (bags('c')._1, bags('g')._1),
        (bags('d')._1, bags('g')._1),
        (bags('e')._1, bags('g')._1),
        (bags('f')._1, bags('g')._1),
        (bags('g')._1, bags('g')._1),
        (bags('x')._1, bags('z')._1),
        (bags('y')._1, bags('z')._1),
        (bags('z')._1, bags('z')._1)
      )
    }
  }

  // TODO not sure how to test failure and rollback for bulkUpdateBagsInSequence

  "bulkAddBagInfo" should "insert multiple bag relations into the database in one commit" in {
    // the call to bulkAddBagInfo is done during the setup process
    val bag1 :: bag2 :: tail = setupBagStoreIndexTestCase().values.toList

    inside(getAllBagInfos) {
      case Success(rels) => rels.map(_.bagId) should contain allOf(bag1._1, bag2._1, tail.map(_._1):_*)
    }
  }

  // TODO not sure how to test failure and rollback for bulkAddbagInfo
}
