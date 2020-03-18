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
package nl.knaw.dans.easy.properties.app.graphql.typedefinitions

import nl.knaw.dans.easy.properties.app.graphql.DataContext
import nl.knaw.dans.easy.properties.app.graphql.model._
import nl.knaw.dans.easy.properties.app.model.DoiAction.DoiAction
import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import nl.knaw.dans.easy.properties.app.model.ingestStep.IngestStepLabel.IngestStepLabel
import nl.knaw.dans.easy.properties.app.model.springfield.SpringfieldPlayMode.SpringfieldPlayMode
import nl.knaw.dans.easy.properties.app.model.state.StateLabel.StateLabel
import nl.knaw.dans.easy.properties.app.model.{ DepositId, DepositorId }
import org.joda.time.DateTime
import sangria.macros.derive.{ deriveInputObjectType, deriveObjectType }
import sangria.marshalling.FromInput
import sangria.schema.{ InputObjectType, ObjectType }

trait GraphQLMutationType {
  this: Scalars
    with GraphQLContentTypeType
    with GraphQLDepositType
    with GraphQLDoiRegisteredType
    with GraphQLDoiActionType
    with GraphQLIdentifierType
    with GraphQLIngestStepType
    with GraphQLQueryType
    with GraphQLSpringfieldType
    with GraphQLStateType =>

  implicit val AddDepositInputType: InputObjectType[AddDepositInput] = deriveInputObjectType[AddDepositInput]()
  implicit val AddDepositInputFromInput: FromInput[AddDepositInput] = fromInput(ad => AddDepositInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositId = ad("depositId").asInstanceOf[DepositId],
    bagName = ad.get("bagName").flatMap(_.asInstanceOf[Option[String]]),
    creationTimestamp = ad.get("creationTimestamp").flatMap(_.asInstanceOf[Option[DateTime]]),
    depositorId = ad("depositorId").asInstanceOf[DepositorId],
    origin = ad("origin").asInstanceOf[Origin],
  ))
  implicit val AddDepositPayloadType: ObjectType[DataContext, AddDepositPayload] = deriveObjectType[DataContext, AddDepositPayload]()

  implicit val AddBagNameInputType: InputObjectType[AddBagNameInput] = deriveInputObjectType[AddBagNameInput]()
  implicit val AddBagNameInputFromInput: FromInput[AddBagNameInput] = fromInput(ad => AddBagNameInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositId = ad("depositId").asInstanceOf[DepositId],
    bagName = ad("bagName").asInstanceOf[String],
  ))
  implicit val AddBagNamePayloadType: ObjectType[DataContext, AddBagNamePayload] = deriveObjectType[DataContext, AddBagNamePayload]()

  implicit val UpdateStateInputType: InputObjectType[UpdateStateInput] = deriveInputObjectType[UpdateStateInput]()
  implicit val UpdateStateInputFromInput: FromInput[UpdateStateInput] = fromInput(ad => UpdateStateInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositId = ad("depositId").asInstanceOf[DepositId],
    label = ad("label").asInstanceOf[StateLabel],
    description = ad("description").asInstanceOf[String],
    timestamp = ad.get("timestamp").flatMap(_.asInstanceOf[Option[DateTime]]),
  ))
  implicit val UpdateStatePayloadType: ObjectType[DataContext, UpdateStatePayload] = deriveObjectType[DataContext, UpdateStatePayload]()

  implicit val UpdateIngestStepInputType: InputObjectType[UpdateIngestStepInput] = deriveInputObjectType[UpdateIngestStepInput]()
  implicit val UpdateIngestStepInputFromInput: FromInput[UpdateIngestStepInput] = fromInput(ad => UpdateIngestStepInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositId = ad("depositId").asInstanceOf[DepositId],
    step = ad("step").asInstanceOf[IngestStepLabel],
    timestamp = ad.get("timestamp").flatMap(_.asInstanceOf[Option[DateTime]]),
  ))
  implicit val UpdateIngestStepPayloadType: ObjectType[DataContext, UpdateIngestStepPayload] = deriveObjectType[DataContext, UpdateIngestStepPayload]()

  implicit val AddIdentifierInputType: InputObjectType[AddIdentifierInput] = deriveInputObjectType[AddIdentifierInput]()
  implicit val AddIdentifierInputFromInput: FromInput[AddIdentifierInput] = fromInput(ad => AddIdentifierInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositId = ad("depositId").asInstanceOf[DepositId],
    idType = ad("type").asInstanceOf[IdentifierType],
    idValue = ad("value").asInstanceOf[String],
    timestamp = ad.get("timestamp").flatMap(_.asInstanceOf[Option[DateTime]]),
  ))
  implicit val AddIdentifierPayloadType: ObjectType[DataContext, AddIdentifierPayload] = deriveObjectType[DataContext, AddIdentifierPayload]()

  implicit val SetDoiRegisteredInputType: InputObjectType[SetDoiRegisteredInput] = deriveInputObjectType[SetDoiRegisteredInput]()
  implicit val SetDoiRegisteredInputFromInput: FromInput[SetDoiRegisteredInput] = fromInput(ad => SetDoiRegisteredInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositId = ad("depositId").asInstanceOf[DepositId],
    value = ad("value").asInstanceOf[Boolean],
    timestamp = ad.get("timestamp").flatMap(_.asInstanceOf[Option[DateTime]]),
  ))
  implicit val SetDoiRegisteredPayloadType: ObjectType[DataContext, SetDoiRegisteredPayload] = deriveObjectType[DataContext, SetDoiRegisteredPayload]()

  implicit val SetDoiActionInputType: InputObjectType[SetDoiActionInput] = deriveInputObjectType[SetDoiActionInput]()
  implicit val SetDoiActionInputFromInput: FromInput[SetDoiActionInput] = fromInput(ad => SetDoiActionInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositId = ad("depositId").asInstanceOf[DepositId],
    value = ad("value").asInstanceOf[DoiAction],
    timestamp = ad.get("timestamp").flatMap(_.asInstanceOf[Option[DateTime]]),
  ))
  implicit val SetDoiActionPayloadType: ObjectType[DataContext, SetDoiActionPayload] = deriveObjectType[DataContext, SetDoiActionPayload]()

  implicit val SetCurationInputType: InputObjectType[SetCurationInput] = deriveInputObjectType[SetCurationInput]()
  implicit val SetCurationInputFromInput: FromInput[SetCurationInput] = fromInput(ad => SetCurationInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositId = ad("depositId").asInstanceOf[DepositId],
    datamanagerUserId = ad("datamanagerUserId").asInstanceOf[String],
    datamanagerEmail = ad("datamanagerEmail").asInstanceOf[String],
    isNewVersion = ad.get("isNewVersion").flatMap(_.asInstanceOf[Option[Boolean]]),
    isCurationRequired = ad("isCurationRequired").asInstanceOf[Boolean],
    isCurationPerformed = ad("isCurationPerformed").asInstanceOf[Boolean],
    timestamp = ad.get("timestamp").flatMap(_.asInstanceOf[Option[DateTime]]),
  ))
  implicit val SetCurationPayloadType: ObjectType[DataContext, SetCurationPayload] = deriveObjectType[DataContext, SetCurationPayload]()

  implicit val SetSpringfieldInputType: InputObjectType[SetSpringfieldInput] = deriveInputObjectType[SetSpringfieldInput]()
  implicit val SetSpringfieldInputFromInput: FromInput[SetSpringfieldInput] = fromInput(ad => SetSpringfieldInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositId = ad("depositId").asInstanceOf[DepositId],
    domain = ad("domain").asInstanceOf[String],
    user = ad("user").asInstanceOf[String],
    collection = ad("collection").asInstanceOf[String],
    playmode = ad("playmode").asInstanceOf[SpringfieldPlayMode],
    timestamp = ad.get("timestamp").flatMap(_.asInstanceOf[Option[DateTime]]),
  ))
  implicit val SetSpringfieldPayloadType: ObjectType[DataContext, SetSpringfieldPayload] = deriveObjectType[DataContext, SetSpringfieldPayload]()

  implicit val SetContentTypeInputType: InputObjectType[SetContentTypeInput] = deriveInputObjectType[SetContentTypeInput]()
  implicit val SetContentTypeInputFromInput: FromInput[SetContentTypeInput] = fromInput(ad => SetContentTypeInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositId = ad("depositId").asInstanceOf[DepositId],
    value = ad("value").asInstanceOf[String],
    timestamp = ad.get("timestamp").flatMap(_.asInstanceOf[Option[DateTime]]),
  ))
  implicit val SetContentTypePayloadType: ObjectType[DataContext, SetContentTypePayload] = deriveObjectType[DataContext, SetContentTypePayload]()

  implicit val RegisterDepositInputType: InputObjectType[RegisterDepositInput] = deriveInputObjectType[RegisterDepositInput]()
  implicit val RegisterDepositInputFromInput: FromInput[RegisterDepositInput] = fromInput(ad => RegisterDepositInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositId = ad("depositId").asInstanceOf[DepositId],
    depositProperties = ad("depositProperties").asInstanceOf[String],
  ))
  implicit val RegisterDepositPayloadType: ObjectType[DataContext, RegisterDepositPayload] = deriveObjectType[DataContext, RegisterDepositPayload]()

  implicit val DeleteDepositsInputType: InputObjectType[DeleteDepositsInput] = deriveInputObjectType[DeleteDepositsInput]()
  implicit val DeleteDepositsInputFromInput: FromInput[DeleteDepositsInput] = fromInput(ad => DeleteDepositsInput(
    clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
    depositIds = ad("depositIds").asInstanceOf[Seq[DepositId]],
  ))
  implicit val DeleteDepositsPayloadType: ObjectType[DataContext, DeleteDepositsPayload] = deriveObjectType[DataContext, DeleteDepositsPayload]()
}
