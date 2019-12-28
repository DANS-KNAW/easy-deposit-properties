package nl.knaw.dans.easy.properties.app.graphql.annotated

import nl.knaw.dans.easy.properties.app.graphql.DataContext
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ DepositResolver, IdentifierResolver }
import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, Timestamp }
import sangria.macros.derive.{ GraphQLDescription, GraphQLField, GraphQLName }
import sangria.relay.Node
import sangria.schema.{ Context, DeferredValue }

@GraphQLName("Deposit")
@GraphQLDescription("Contains all technical metadata about this deposit.")
class GraphQLDeposit(deposit: Deposit) extends Node {

  @GraphQLField
  @GraphQLDescription("The identifier of the deposit.")
  val depositId: DepositId = deposit.id

  @GraphQLField
  @GraphQLDescription("The name of the deposited bag.")
  val bagName: Option[String] = deposit.bagName

  @GraphQLField
  @GraphQLDescription("The moment this deposit was created.")
  val creationTimestamp: Timestamp = deposit.creationTimestamp

  @GraphQLField
  @GraphQLDescription("The origin of the deposit.")
  val origin: Origin = deposit.origin

  override val id: String = depositId.toString

  @GraphQLField
  @GraphQLDescription("Get the timestamp at which this deposit was last modified. If the dataset was only created, the creation timestamp is returned.")
  def lastModified(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[Timestamp]] = {
    DepositResolver.lastModified(deposit.id)
  }

//  @GraphQLField
//  @GraphQLDescription("The current state of the deposit.")
//  def state(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    StateResolver.currentById(deposit.id)
//  }

//  @GraphQLField
//  @GraphQLDescription("List all states of the deposit.")
//  def states()
//            (implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    StateResolver.allById(deposit.id)
//      .map(timebasedFilterAndSort(optStateOrderArgument))
//      .map(ExtendedConnection.connectionFromSeq(_, ConnectionArgs(ctx)))
//  }

//  @GraphQLField
//  @GraphQLDescription("The current ingest step of the deposit.")
//  def ingestStep(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    IngestStepResolver.currentById(deposit.id)
//  }

//  @GraphQLField
//  @GraphQLDescription("List all ingest steps of the deposit.")
//  def ingestSteps()
//                 (implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    IngestStepResolver.allById(deposit.id)
//      .map(timebasedFilterAndSort(optIngestStepOrderArgument))
//      .map(ExtendedConnection.connectionFromSeq(_, ConnectionArgs(ctx)))
//  }

//  @GraphQLField
//  @GraphQLDescription("Information about the depositor that submitted this deposit.")
//  def depositor(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    new GraphQLDepositor(deposit.depositorId)
//  }

  @GraphQLField
  @GraphQLDescription("Return the identifier of the given type related to this deposit.")
  def identifier(@GraphQLName("type") @GraphQLDescription("Find the identifier with this specific type.") idType: IdentifierType.Value)
                (implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Option[GraphQLIdentifier]] = {
    IdentifierResolver.identifierByType(deposit.id, idType)
      .map(_.map(new GraphQLIdentifier(_)))
  }

  @GraphQLField
  @GraphQLDescription("List the identifiers related to this deposit.")
  def identifiers(implicit ctx: Context[DataContext, GraphQLDeposit]): DeferredValue[DataContext, Seq[GraphQLIdentifier]] = {
    IdentifierResolver.allById(deposit.id)
      .map(_.map(new GraphQLIdentifier(_)))
  }

//  @GraphQLField
//  @GraphQLDescription("Returns whether the DOI is registered in DataCite.")
//  def doiRegistered(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    DoiEventResolver.isDoiRegistered(deposit.id)
//  }

//  @GraphQLField
//  @GraphQLDescription("Lists all state changes related to the registration of the DOI in DataCite.")
//  def doiRegisteredEvents(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    DoiEventResolver.allDoiRegisteredById(deposit.id)
//      .map(_.sortBy(_.timestamp))
//  }

//  @GraphQLField
//  @GraphQLDescription("Returns whether the DOI should be 'created' or 'updated' on registration in DataCite.")
//  def doiAction(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    DoiEventResolver.currentDoiActionById(deposit.id)
//  }

//  @GraphQLField
//  @GraphQLDescription("Lists all state changes related to whether the DOI should be 'created' or 'updated' on registration in DataCite.")
//  def doiActionEvents(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    DoiEventResolver.allDoiActionsById(deposit.id)
//      .map(_.sortBy(_.timestamp))
//  }

//  @GraphQLField
//  @GraphQLDescription("The data manager currently assigned to this deposit.")
//  def curator(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    CurationResolver.currentCuratorsById(deposit.id)
//  }

//  @GraphQLField
//  @GraphQLDescription("List all data manager that were ever assigned to this deposit.")
//  def curators()
//              (implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    CurationResolver.allCuratorsById(deposit.id)
//      .map(timebasedFilterAndSort(optCuratorOrderArgument))
//      .map(ExtendedConnection.connectionFromSeq(_, ConnectionArgs(ctx)))
//  }

//  @GraphQLField
//  @GraphQLDescription("Whether this deposit is a new version.")
//  def isNewVersion(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    CurationResolver.isNewVersion(deposit.id)
//  }

//  @GraphQLField
//  @GraphQLDescription("List the present and past values for 'is-new-version'.")
//  def isNewVersionEvents(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    CurationResolver.allIsNewVersionEvents(deposit.id)
//      .map(_.sortBy(_.timestamp))
//  }

//  @GraphQLField
//  @GraphQLDescription("Whether this deposit requires curation.")
//  def curationRequired(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    CurationResolver.isCurationRequired(deposit.id)
//  }

//  @GraphQLField
//  @GraphQLDescription("List the present and past values for 'curation-required'.")
//  def curationRequiredEvents(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    CurationResolver.allIsCurationRequiredEvents(deposit.id)
//      .map(_.sortBy(_.timestamp))
//  }

//  @GraphQLField
//  @GraphQLDescription("Whether curation on this deposit has been performed.")
//  def curationPerformed(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    CurationResolver.isCurationPerformed(deposit.id)
//  }

//  @GraphQLField
//  @GraphQLDescription("List the present and past values for 'curation-performed'.")
//  def curationPerformedEvents(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    CurationResolver.allIsCurationPerformedEvents(deposit.id)
//      .map(_.sortBy(_.timestamp))
//  }

//  @GraphQLField
//  @GraphQLDescription("The springfield configuration currently associated with this deposit.")
//  def springfield(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    SpringfieldResolver.currentById(deposit.id)
//  }

//  @GraphQLField
//  @GraphQLDescription("List the present and past values for springfield configuration.")
//  def springfields()
//                  (implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    SpringfieldResolver.allById(deposit.id)
//      .map(timebasedFilterAndSort(optSpringfieldOrderArgument))
//  }

//  @GraphQLField
//  @GraphQLDescription("The content type currently associated with this deposit.")
//  def contentType(implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    ContentTypeResolver.currentById(deposit.id)
//  }

//  @GraphQLField
//  @GraphQLDescription("List the present and past values of content types.")
//  def contentTypes()
//                  (implicit ctx: Context[DataContext, GraphQLDeposit]) = {
//    ContentTypeResolver.allById(deposit.id)
//      .map(timebasedFilterAndSort(optContentTypeOrderArgument))
//  }
}
