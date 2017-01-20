package nl.knaw.dans.easy.bagindex

import nl.knaw.dans.easy.bagindex.components.Bagit4FacadeComponent

trait Bagit4Fixture extends TestSupportFixture with Bagit4FacadeComponent {

  override val bagFacade = new Bagit4Facade()
}
