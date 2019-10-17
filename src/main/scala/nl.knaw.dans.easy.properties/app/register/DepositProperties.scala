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
package nl.knaw.dans.easy.properties.app.register

import nl.knaw.dans.easy.properties.app.model.{ Deposit, DoiActionEvent, DoiRegisteredEvent }
import nl.knaw.dans.easy.properties.app.model.contentType.InputContentType
import nl.knaw.dans.easy.properties.app.model.curation.InputCuration
import nl.knaw.dans.easy.properties.app.model.identifier.InputIdentifier
import nl.knaw.dans.easy.properties.app.model.ingestStep.InputIngestStep
import nl.knaw.dans.easy.properties.app.model.springfield.InputSpringfield
import nl.knaw.dans.easy.properties.app.model.state.InputState

case class DepositProperties(deposit: Deposit,
                             state: Option[InputState],
                             ingestStep: Option[InputIngestStep],
                             identifiers: Seq[InputIdentifier],
                             doiAction: Option[DoiActionEvent],
                             doiRegistered: Option[DoiRegisteredEvent],
                             curation: Option[InputCuration],
                             springfield: Option[InputSpringfield],
                             contentType: Option[InputContentType],
                            )
