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
package nl.knaw.dans.easy.properties.app.graphql.model

import nl.knaw.dans.easy.properties.app.graphql._
import nl.knaw.dans.easy.properties.app.graphql.middleware.Authentication.RequiresAuthentication
import nl.knaw.dans.easy.properties.app.graphql.resolvers._
import nl.knaw.dans.easy.properties.app.model.DoiAction.DoiAction
import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import nl.knaw.dans.easy.properties.app.model.contentType.ContentTypeValue.ContentTypeValue
import nl.knaw.dans.easy.properties.app.model.contentType.InputContentType
import nl.knaw.dans.easy.properties.app.model.curation.InputCuration
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import nl.knaw.dans.easy.properties.app.model.identifier.InputIdentifier
import nl.knaw.dans.easy.properties.app.model.ingestStep.IngestStepLabel.IngestStepLabel
import nl.knaw.dans.easy.properties.app.model.ingestStep.InputIngestStep
import nl.knaw.dans.easy.properties.app.model.springfield.InputSpringfield
import nl.knaw.dans.easy.properties.app.model.springfield.SpringfieldPlayMode.SpringfieldPlayMode
import nl.knaw.dans.easy.properties.app.model.state.InputState
import nl.knaw.dans.easy.properties.app.model.state.StateLabel.StateLabel
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, DoiActionEvent, DoiRegisteredEvent }
import org.joda.time.DateTime
import sangria.macros.derive.{ GraphQLDescription, GraphQLField, GraphQLFieldTags, GraphQLName }
import sangria.schema.{ Action, Context, DeferredValue }

case class AddDepositInput(clientMutationId: Option[String],
                           @GraphQLDescription("The deposit's identifier.") depositId: DepositId,
                           @GraphQLDescription("The name of the deposited bag.") bagName: Option[String],
                           @GraphQLDescription("The timestamp at which this deposit was created.") creationTimestamp: DateTime,
                           @GraphQLDescription("The depositor that submits this deposit.") depositorId: String,
                           @GraphQLDescription("The origin of the deposited bag.") origin: Origin,
                          )
case class AddBagNameInput(clientMutationId: Option[String],
                           @GraphQLDescription("The deposit's identifier.") depositId: DepositId,
                           @GraphQLDescription("The name of the deposited bag.") bagName: String,
                          )
case class UpdateStateInput(clientMutationId: Option[String],
                            @GraphQLDescription("The deposit's identifier.") depositId: DepositId,
                            @GraphQLDescription("The state label of the deposit.") label: StateLabel,
                            @GraphQLDescription("Additional information about the state.") description: String,
                            @GraphQLDescription("The timestamp at which the deposit got into this state.") timestamp: DateTime,
                           )
case class UpdateIngestStepInput(clientMutationId: Option[String],
                                 @GraphQLDescription("The deposit's identifier.") depositId: DepositId,
                                 @GraphQLDescription("The label of the ingest step.") step: IngestStepLabel,
                                 @GraphQLDescription("The timestamp at which the deposit got into this ingest step.") timestamp: DateTime,
                                )
case class AddIdentifierInput(clientMutationId: Option[String],
                              @GraphQLDescription("The deposit's identifier.") depositId: DepositId,
                              @GraphQLName("type") @GraphQLDescription("The type of identifier.") idType: IdentifierType,
                              @GraphQLName("value") @GraphQLDescription("The value of the identifier.") idValue: String,
                              @GraphQLDescription("The timestamp at which the identifier got added to this deposit.") timestamp: DateTime,
                             )
case class SetDoiRegisteredInput(clientMutationId: Option[String],
                                 @GraphQLDescription("The deposit's identifier.") depositId: DepositId,
                                 @GraphQLDescription("Whether the DOI is registered in DataCite.") value: Boolean,
                                 @GraphQLDescription("The timestamp at which the DOI was registered in DataCite.") timestamp: DateTime,
                                )
case class SetDoiActionInput(clientMutationId: Option[String],
                             @GraphQLDescription("The deposit's identifier.") depositId: DepositId,
                             @GraphQLDescription("Whether the DOI must be 'created' or 'updated' when registering in DataCite.") value: DoiAction,
                             @GraphQLDescription("The timestamp at which this value was added.") timestamp: DateTime,
                            )
case class SetCurationInput(clientMutationId: Option[String],
                            @GraphQLDescription("The deposit's identifier.") depositId: DepositId,
                            @GraphQLDescription("The data manager's username in EASY.") datamanagerUserId: String,
                            @GraphQLDescription("The data manager's email address.") datamanagerEmail: String,
                            @GraphQLDescription("True if the deposit is a new version.") isNewVersion: Option[Boolean],
                            @GraphQLDescription("True if curation by a data manager is required.") isCurationRequired: Boolean,
                            @GraphQLDescription("True if curation by the data manager has been performed.") isCurationPerformed: Boolean,
                            @GraphQLDescription("The timestamp at which the curation event was assigned to this deposit.") timestamp: DateTime,
                           )
case class SetSpringfieldInput(clientMutationId: Option[String],
                               @GraphQLDescription("The deposit's identifier.") depositId: DepositId,
                               @GraphQLDescription("The domain of Springfield.") domain: String,
                               @GraphQLDescription("The user of Springfield.") user: String,
                               @GraphQLDescription("The collection of Springfield.") collection: String,
                               @GraphQLDescription("The playmode used in Springfield.") playmode: SpringfieldPlayMode,
                               @GraphQLDescription("The timestamp at which this springfield configuration was associated with the deposit.") timestamp: DateTime,
                              )
case class SetContentTypeInput(clientMutationId: Option[String],
                               @GraphQLDescription("The deposit's identifier.") depositId: DepositId,
                               @GraphQLDescription("The content type associated with this deposit.") value: ContentTypeValue,
                               @GraphQLDescription("The timestamp at which this springfield configuration was associated with the deposit.") timestamp: DateTime,
                              )
case class RegisterDepositInput(clientMutationId: Option[String],
                                @GraphQLDescription("The deposit's identifier.") depositId: DepositId,
                                @GraphQLDescription("The 'deposit.properties' describing the deposit to be registered.") depositProperties: String,
                               )
case class DeleteDepositsInput(clientMutationId: Option[String],
                               @GraphQLDescription("A list of deposit identifiers.") depositIds: Seq[DepositId],
                              )

class AddDepositPayload(cmi: Option[String], depositId: DepositId) {
  @GraphQLField
  val clientMutationId: Option[String] = cmi

  @GraphQLField
  def deposit(implicit ctx: Context[DataContext, AddDepositPayload]): DeferredValue[DataContext, Option[GraphQLDeposit]] = {
    DepositResolver.depositById(depositId)
      .map(_.map(new GraphQLDeposit(_)))
  }
}
class AddBagNamePayload(cmi: Option[String], depositId: DepositId) {
  @GraphQLField
  val clientMutationId: Option[String] = cmi

  @GraphQLField
  def deposit(implicit ctx: Context[DataContext, AddBagNamePayload]): DeferredValue[DataContext, Option[GraphQLDeposit]] = {
    DepositResolver.depositById(depositId)
      .map(_.map(new GraphQLDeposit(_)))
  }
}
class UpdateStatePayload(cmi: Option[String], objectId: String) {
  @GraphQLField
  val clientMutationId: Option[String] = cmi

  @GraphQLField
  def state(implicit ctx: Context[DataContext, UpdateStatePayload]): DeferredValue[DataContext, Option[GraphQLState]] = {
    StateResolver.stateById(objectId)
      .map(_.map(new GraphQLState(_)))
  }
}
class UpdateIngestStepPayload(cmi: Option[String], objectId: String) {
  @GraphQLField
  val clientMutationId: Option[String] = cmi

  @GraphQLField
  def ingestStep(implicit ctx: Context[DataContext, UpdateIngestStepPayload]): DeferredValue[DataContext, Option[GraphQLIngestStep]] = {
    IngestStepResolver.ingestStepById(objectId)
      .map(_.map(new GraphQLIngestStep(_)))
  }
}
class AddIdentifierPayload(cmi: Option[String], objectId: String) {
  @GraphQLField
  val clientMutationId: Option[String] = cmi

  @GraphQLField
  def identifier(implicit ctx: Context[DataContext, AddIdentifierPayload]): DeferredValue[DataContext, Option[GraphQLIdentifier]] = {
    IdentifierResolver.identifierById(objectId)
      .map(_.map(new GraphQLIdentifier(_)))
  }
}
class SetDoiRegisteredPayload(cmi: Option[String], obj: DoiRegisteredEvent) {
  @GraphQLField
  val clientMutationId: Option[String] = cmi

  @GraphQLField
  def doiRegistered(implicit ctx: Context[DataContext, SetDoiRegisteredPayload]): GraphQLDoiRegistered = {
    new GraphQLDoiRegistered(obj)
  }
}
class SetDoiActionPayload(cmi: Option[String], obj: DoiActionEvent) {
  @GraphQLField
  val clientMutationId: Option[String] = cmi

  @GraphQLField
  def doiAction(implicit ctx: Context[DataContext, SetDoiActionPayload]): GraphQLDoiAction = {
    new GraphQLDoiAction(obj)
  }
}
class SetCurationPayload(cmi: Option[String], objectId: String) {
  @GraphQLField
  val clientMutationId: Option[String] = cmi

  @GraphQLField
  def curation(implicit ctx: Context[DataContext, SetCurationPayload]): DeferredValue[DataContext, Option[GraphQLCuration]] = {
    CurationResolver.curationById(objectId)
      .map(_.map(new GraphQLCuration(_)))
  }
}
class SetSpringfieldPayload(cmi: Option[String], objectId: String) {
  @GraphQLField
  val clientMutationId: Option[String] = cmi

  @GraphQLField
  def springfield(implicit ctx: Context[DataContext, SetSpringfieldPayload]): DeferredValue[DataContext, Option[GraphQLSpringfield]] = {
    SpringfieldResolver.springfieldById(objectId)
      .map(_.map(new GraphQLSpringfield(_)))
  }
}
class SetContentTypePayload(cmi: Option[String], objectId: String) {
  @GraphQLField
  val clientMutationId: Option[String] = cmi

  @GraphQLField
  def contentType(implicit ctx: Context[DataContext, SetContentTypePayload]): DeferredValue[DataContext, Option[GraphQLContentType]] = {
    ContentTypeResolver.contentTypeById(objectId)
      .map(_.map(new GraphQLContentType(_)))
  }
}
class RegisterDepositPayload(cmi: Option[String], depositId: DepositId) {
  @GraphQLField
  val clientMutationId: Option[String] = cmi

  @GraphQLField
  def deposit(implicit ctx: Context[DataContext, RegisterDepositPayload]): DeferredValue[DataContext, Option[GraphQLDeposit]] = {
    DepositResolver.depositById(depositId)
      .map(_.map(new GraphQLDeposit(_)))
  }
}
case class DeleteDepositsPayload(@GraphQLField clientMutationId: Option[String], @GraphQLField depositIds: Seq[DepositId])

@GraphQLDescription("The root query for implementing GraphQL mutations.")
class Mutation {

  @GraphQLField
  @GraphQLDescription("Register a new deposit with 'id', 'creationTimestamp', 'depositId' and 'origin'.")
  @GraphQLFieldTags(RequiresAuthentication)
  def addDeposit(input: AddDepositInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, AddDepositPayload] = {
    ctx.ctx.repo.deposits
      .store(Deposit(
        id = input.depositId,
        bagName = input.bagName,
        creationTimestamp = input.creationTimestamp,
        depositorId = input.depositorId,
        origin = input.origin,
      ))
      .map(deposit => new AddDepositPayload(input.clientMutationId, deposit.id))
      .toTry
  }

  @GraphQLField
  @GraphQLDescription("Register the bag's name for the deposit identified by 'id'.")
  @GraphQLFieldTags(RequiresAuthentication)
  def addBagName(input: AddBagNameInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, AddBagNamePayload] = {
    ctx.ctx.repo.deposits
      .storeBagName(
        depositId = input.depositId,
        bagName = input.bagName,
      )
      .map(depositId => new AddBagNamePayload(
        cmi = input.clientMutationId,
        depositId = depositId,
      ))
      .toTry
  }

  @GraphQLField
  @GraphQLDescription("Update the state of the deposit identified by 'id'.")
  @GraphQLFieldTags(RequiresAuthentication)
  def updateState(input: UpdateStateInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, UpdateStatePayload] = {
    ctx.ctx.repo.states
      .store(
        id = input.depositId,
        state = InputState(
          label = input.label,
          description = input.description,
          timestamp = input.timestamp,
        ),
      )
      .map(state => new UpdateStatePayload(
        cmi = input.clientMutationId,
        objectId = state.id,
      ))
      .toTry
  }

  @GraphQLField
  @GraphQLDescription("Update the ingest step of the deposit identified by 'id'.")
  @GraphQLFieldTags(RequiresAuthentication)
  def updateIngestStep(input: UpdateIngestStepInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, UpdateIngestStepPayload] = {
    ctx.ctx.repo.ingestSteps
      .store(
        id = input.depositId,
        step = InputIngestStep(
          step = input.step,
          timestamp = input.timestamp,
        ),
      )
      .map(ingestStep => new UpdateIngestStepPayload(
        cmi = input.clientMutationId,
        objectId = ingestStep.id,
      ))
      .toTry
  }

  @GraphQLField
  @GraphQLDescription("Add an identifier to the deposit identified by 'id'.")
  @GraphQLFieldTags(RequiresAuthentication)
  def addIdentifier(input: AddIdentifierInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, AddIdentifierPayload] = {
    ctx.ctx.repo.identifiers
      .store(
        id = input.depositId,
        identifier = InputIdentifier(
          idType = input.idType,
          idValue = input.idValue,
          timestamp = input.timestamp,
        ),
      )
      .map(identifier => new AddIdentifierPayload(
        cmi = input.clientMutationId,
        objectId = identifier.id,
      ))
      .toTry
  }

  @GraphQLField
  @GraphQLDescription("Set whether the DOI has been registered in DataCite.")
  @GraphQLFieldTags(RequiresAuthentication)
  def setDoiRegistered(input: SetDoiRegisteredInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, SetDoiRegisteredPayload] = {
    ctx.ctx.repo.doiRegistered
      .store(
        id = input.depositId,
        registered = DoiRegisteredEvent(
          value = input.value,
          timestamp = input.timestamp,
        ),
      )
      .map(doiRegisteredEvent => new SetDoiRegisteredPayload(
        cmi = input.clientMutationId,
        obj = doiRegisteredEvent,
      ))
      .toTry
  }

  @GraphQLField
  @GraphQLDescription("Set whether the DOI should be 'created' or 'updated' on registration in DataCite.")
  @GraphQLFieldTags(RequiresAuthentication)
  def setDoiAction(input: SetDoiActionInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, SetDoiActionPayload] = {
    ctx.ctx.repo.doiAction
      .store(
        id = input.depositId,
        action = DoiActionEvent(
          value = input.value,
          timestamp = input.timestamp,
        ),
      )
      .map(doiActionEvent => new SetDoiActionPayload(
        cmi = input.clientMutationId,
        obj = doiActionEvent,
      ))
      .toTry
  }

  @GraphQLField
  @GraphQLDescription("Assign a curation event to the deposit identified by 'id'.")
  @GraphQLFieldTags(RequiresAuthentication)
  def setCuration(input: SetCurationInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, SetCurationPayload] = {
    ctx.ctx.repo.curation
      .store(
        id = input.depositId,
        curation = InputCuration(
          isNewVersion = input.isNewVersion,
          isRequired = input.isCurationRequired,
          isPerformed = input.isCurationPerformed,
          datamanagerUserId = input.datamanagerUserId,
          datamanagerEmail = input.datamanagerEmail,
          timestamp = input.timestamp,
        ),
      )
      .map(curation => new SetCurationPayload(
        cmi = input.clientMutationId,
        objectId = curation.id,
      ))
      .toTry
  }

  @GraphQLField
  @GraphQLDescription("Set the springfield configuration for this deposit.")
  @GraphQLFieldTags(RequiresAuthentication)
  def setSpringfield(input: SetSpringfieldInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, SetSpringfieldPayload] = {
    ctx.ctx.repo.springfield
      .store(
        id = input.depositId,
        springfield = InputSpringfield(
          domain = input.domain,
          user = input.user,
          collection = input.collection,
          playmode = input.playmode,
          timestamp = input.timestamp,
        ),
      )
      .map(springfield => new SetSpringfieldPayload(
        cmi = input.clientMutationId,
        objectId = springfield.id,
      ))
      .toTry
  }

  @GraphQLField
  @GraphQLDescription("Set the content type for this deposit.")
  @GraphQLFieldTags(RequiresAuthentication)
  def setContentType(input: SetContentTypeInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, SetContentTypePayload] = {
    ctx.ctx.repo.contentType
      .store(
        id = input.depositId,
        contentType = InputContentType(
          value = input.value,
          timestamp = input.timestamp,
        ),
      )
      .map(contentType => new SetContentTypePayload(
        cmi = input.clientMutationId,
        objectId = contentType.id,
      ))
      .toTry
  }

  @GraphQLField
  @GraphQLDescription("Register a new deposit with initial properties from a 'deposit.properties' string.")
  @GraphQLFieldTags(RequiresAuthentication)
  def registerDeposit(input: RegisterDepositInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, RegisterDepositPayload] = {
    ctx.ctx.registration
      .register(
        depositId = input.depositId,
        props = input.depositProperties,
      )
      .map(depositId => new RegisterDepositPayload(
        cmi = input.clientMutationId,
        depositId = depositId,
      ))
      .toTry
  }

  @GraphQLField
  @GraphQLDescription("Delete deposits.")
  @GraphQLFieldTags(RequiresAuthentication)
  def deleteDeposits(input: DeleteDepositsInput)(implicit ctx: Context[DataContext, Unit]): Action[DataContext, DeleteDepositsPayload] = {
    ctx.ctx.deleter
      .deleteDepositsBy(
        ids = input.depositIds,
      )
      .map(depositIds => DeleteDepositsPayload(
        input.clientMutationId,
        depositIds,
      ))
      .toTry
  }
}
