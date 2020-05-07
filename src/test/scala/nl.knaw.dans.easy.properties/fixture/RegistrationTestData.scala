/**
 * Copyright (C) 2019 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
package nl.knaw.dans.easy.properties.fixture

import java.util.UUID

import cats.syntax.option._
import nl.knaw.dans.easy.properties.app.model.contentType.{ ContentTypeValue, InputContentType }
import nl.knaw.dans.easy.properties.app.model.curator.InputCurator
import nl.knaw.dans.easy.properties.app.model.identifier.{ IdentifierType, InputIdentifier }
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ IngestStepLabel, InputIngestStep }
import nl.knaw.dans.easy.properties.app.model.iscurationperformed.InputIsCurationPerformed
import nl.knaw.dans.easy.properties.app.model.iscurationrequired.InputIsCurationRequired
import nl.knaw.dans.easy.properties.app.model.isnewversion.InputIsNewVersion
import nl.knaw.dans.easy.properties.app.model.springfield.{ InputSpringfield, SpringfieldPlayMode }
import nl.knaw.dans.easy.properties.app.model.state.{ InputState, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, DoiAction, DoiActionEvent, DoiRegisteredEvent, Origin }
import nl.knaw.dans.easy.properties.app.register.DepositProperties
import org.joda.time.DateTime

trait RegistrationTestData {

  val depositId: DepositId = UUID.fromString("9d507261-3b79-22e7-86d0-6fb9417d930d")
  val validDepositPropertiesBody: String =
    """bag-store.bag-name = bag
      |creation.timestamp = 2019-01-01T00:00:00.000Z
      |depositor.userId = user001
      |deposit.origin = SWORD2
      |
      |state.label = SUBMITTED
      |state.description = my description
      |
      |deposit.ingest.current-step = BAGSTORE
      |
      |identifier.doi = my-doi-value
      |identifier.urn = my-urn-value
      |identifier.fedora = my-fedora-value
      |bag-store.bag-id = my-bag-store-value
      |
      |identifier.dans-doi.registered = yes
      |identifier.dans-doi.action = update
      |
      |curation.datamanager.userId = archie001
      |curation.datamanager.email = does.not.exists@dans.knaw.nl
      |
      |curation.is-new-version = yes
      |curation.required = no
      |curation.performed = no
      |
      |springfield.domain = domain
      |springfield.user = user
      |springfield.collection = collection
      |springfield.playmode = continuous
      |
      |easy-sword2.client-message.content-type = application/zip""".stripMargin

  val minimalDepositPropertiesBody: String =
    """creation.timestamp = 2019-01-01T00:00:00.000Z
      |depositor.userId = user001
      |deposit.origin = SWORD2""".stripMargin

  val timestamp: DateTime = DateTime.parse("2019-01-01T00:00:00.000Z")
  val validDepositProperties: DepositProperties = DepositProperties(
    deposit = Deposit(UUID.fromString("9d507261-3b79-22e7-86d0-6fb9417d930d"), "bag".some, timestamp, "user001", Origin.SWORD2),
    state = InputState(StateLabel.SUBMITTED, "my description", timestamp).some,
    ingestStep = InputIngestStep(IngestStepLabel.BAGSTORE, timestamp).some,
    identifiers = Seq(
      InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", timestamp),
      InputIdentifier(IdentifierType.DOI, "my-doi-value", timestamp),
      InputIdentifier(IdentifierType.URN, "my-urn-value", timestamp),
      InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", timestamp),
    ),
    doiAction = DoiActionEvent(DoiAction.UPDATE, timestamp).some,
    doiRegistered = DoiRegisteredEvent(value = true, timestamp).some,
    curator = InputCurator("archie001", "does.not.exists@dans.knaw.nl", timestamp).some,
    isNewVersion = InputIsNewVersion(value = true, timestamp).some,
    isCurationRequired = InputIsCurationRequired(value = false, timestamp).some,
    isCurationPerformed = InputIsCurationPerformed(value = false, timestamp).some,
    springfield = InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, timestamp).some,
    contentType = InputContentType(ContentTypeValue.ZIP, timestamp).some,
  )
  val minimalDepositProperties: DepositProperties = DepositProperties(
    deposit = Deposit(UUID.fromString("9d507261-3b79-22e7-86d0-6fb9417d930d"), none, timestamp, "user001", Origin.SWORD2),
    state = none,
    ingestStep = none,
    identifiers = Seq.empty,
    doiAction = none,
    doiRegistered = none,
    curator = none,
    isNewVersion = none,
    isCurationRequired = none,
    isCurationPerformed = none,
    springfield = none,
    contentType = none,
  )
}
