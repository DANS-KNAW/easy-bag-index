package nl.knaw.dans.easy.bagindex.components

import nl.knaw.dans.easy.bagindex.BagId
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try

trait GetBagSequence {
  this: Database with DebugEnhancedLogging =>

  /**
   * Returns a sequence of all bagIds that are in the same bag sequence as the given bagId.
   * The resulting sequence is sorted by the timestamp of the bag creation.
   * If the given bagId is not in the database, a `BagIdNotFoundException` will be returned.
   *
   * @param bagId the bagId of which the whole sequence is requested
   * @return a sequence of all bagIds that are in the same bag sequence as the given bagId.
   */
  // TODO use
  def getBagSequence(bagId: BagId): Try[Seq[BagId]] = {
    trace(bagId)
    for {
      baseId <- getBaseBagId(bagId)
      seq <- getAllBagsWithBase(baseId)
    } yield seq
  }
}
