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
package nl.knaw.dans.easy.properties.app.graphql.types.typedefs

import nl.knaw.dans.easy.properties.app.graphql.ordering.{ IngestStepOrder, IngestStepOrderField, OrderDirection }
import nl.knaw.dans.easy.properties.app.model.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter
import nl.knaw.dans.easy.properties.app.model.ingestStep.IngestStepLabel.IngestStepLabel
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ DepositIngestStepFilter, IngestStepLabel }
import sangria.macros.derive._
import sangria.marshalling.FromInput
import sangria.schema.{ EnumType, InputObjectType }

trait GraphQLIngestStepType {
  this: GraphQLCommonTypes =>

  implicit val StepLabelType: EnumType[IngestStepLabel.Value] = deriveEnumType(
    EnumTypeDescription("The label identifying the ingest step."),
    DocumentValue("VALIDATE", "Started validating the deposit."),
    DocumentValue("PID_GENERATOR", "Persistent identifiers are being generated for this deposit."),
    DocumentValue("FEDORA", "A dissemination copy of the deposit is being made in Fedora."),
    DocumentValue("SPRINGFIELD", "Dissemination copies of the audio/video files in the deposit are being made in Springfield."),
    DocumentValue("BAGSTORE", "Creating an archival copy of the deposit for storage in the vault."),
    DocumentValue("SOLR4FILES", "The file content of the deposit's payload is being index."),
    DocumentValue("COMPLETED", "The ingest process of this deposit has completed."),
  )

  implicit val DepositIngestStepFilterType: InputObjectType[DepositIngestStepFilter] = deriveInputObjectType(
    InputObjectTypeDescription("The label and filter to be used in searching for deposits by ingest step"),
    DocumentInputField("label", "If provided, only show deposits with this state."),
    DocumentInputField("filter", "Determine whether to search in current states (`LATEST`, default) or all current and past states (`ALL`)."),
  )
  implicit val DepositIngestStepFilterFromInput: FromInput[DepositIngestStepFilter] = fromInput(ad => DepositIngestStepFilter(
    label = ad("label").asInstanceOf[IngestStepLabel],
    filter = ad("filter").asInstanceOf[Option[SeriesFilter]].getOrElse(SeriesFilter.LATEST),
  ))

  implicit val IngestStepOrderFieldType: EnumType[IngestStepOrderField.Value] = deriveEnumType()
  implicit val IngestStepOrderInputType: InputObjectType[IngestStepOrder] = deriveInputObjectType(
    InputObjectTypeDescription("Ordering options for ingest steps"),
    DocumentInputField("field", "The field to order ingest steps by"),
    DocumentInputField("direction", "The ordering direction"),
  )
  implicit val IngestStepOrderFromInput: FromInput[IngestStepOrder] = fromInput(ad => IngestStepOrder(
    field = ad("field").asInstanceOf[IngestStepOrderField.Value],
    direction = ad("direction").asInstanceOf[OrderDirection.Value],
  ))
}
