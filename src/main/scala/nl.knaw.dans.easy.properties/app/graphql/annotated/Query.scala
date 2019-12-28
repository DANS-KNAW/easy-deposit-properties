package nl.knaw.dans.easy.properties.app.graphql.annotated

import nl.knaw.dans.easy.properties.app.graphql.DataContext
import nl.knaw.dans.easy.properties.app.graphql.ordering.DepositOrder
import nl.knaw.dans.easy.properties.app.graphql.relay.ExtendedConnection
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ DepositResolver, IdentifierResolver }
import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import nl.knaw.dans.easy.properties.app.model.contentType.DepositContentTypeFilter
import nl.knaw.dans.easy.properties.app.model.curator.DepositCuratorFilter
import nl.knaw.dans.easy.properties.app.model.{ DepositCurationPerformedFilter, DepositCurationRequiredFilter, DepositDoiActionFilter, DepositDoiRegisteredFilter, DepositId, DepositIsNewVersionFilter, DepositorId }
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType
import nl.knaw.dans.easy.properties.app.model.ingestStep.DepositIngestStepFilter
import nl.knaw.dans.easy.properties.app.model.state.DepositStateFilter
import nl.knaw.dans.easy.properties.app.repository.DepositFilters
import org.joda.time.DateTime
import sangria.macros.derive.{ GraphQLDescription, GraphQLField, GraphQLName }
import sangria.relay.ConnectionArgs
import sangria.schema.{ Context, DeferredValue }

@GraphQLDescription("The query root of easy-deposit-properties' GraphQL interface.")
class Query {

  @GraphQLField
  @GraphQLDescription("Get the technical metadata of the deposit identified by 'id'.")
  def deposit(@GraphQLDescription("The id for which to find the deposit") id: DepositId)
             (implicit ctx: Context[DataContext, Unit]): DeferredValue[DataContext, Option[GraphQLDeposit]] = {
    DepositResolver.depositById(id)
      .map(_.map(new GraphQLDeposit(_)))
  }

  @GraphQLField
  @GraphQLDescription("List all registered deposits.")
  def deposits(@GraphQLDescription("Find only those deposits that have this specified bag name.") bagName: Option[String] = None,
               @GraphQLDescription("Find only those deposits that have this specified origin.") origin: Option[Origin] = None,
               @GraphQLDescription("List only those deposits that have this specified state label.") state: Option[DepositStateFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified ingest step label.") ingestStep: Option[DepositIngestStepFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified value for DOI registered.") doiRegistered: Option[DepositDoiRegisteredFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified value for DOI action.") doiAction: Option[DepositDoiActionFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified data manager.") curator: Option[DepositCuratorFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified value for 'is-new-version'.") isNewVersion: Option[DepositIsNewVersionFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified value for 'curation required'.") curationRequired: Option[DepositCurationRequiredFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified value for 'curation performed'.") curationPerformed: Option[DepositCurationPerformedFilter] = None,
               @GraphQLDescription("List only those deposits that have this specified content type.") contentType: Option[DepositContentTypeFilter] = None,
               @GraphQLDescription("Ordering options for the returned deposits.") orderBy: Option[DepositOrder] = None,
               @GraphQLDescription("List only those elements that have a timestamp earlier than this given timestamp.") earlierThan: Option[DateTime] = None,
               @GraphQLDescription("List only those elements that have a timestamp later than this given timestamp.") laterThan: Option[DateTime] = None,
               @GraphQLDescription("List only those elements that have a timestamp equal to the given timestamp.") atTimestamp: Option[DateTime] = None,
               before: Option[String] = None,
               after: Option[String] = None,
               first: Option[Int] = None,
               last: Option[Int] = None,
              )(implicit ctx: Context[DataContext, Unit]): DeferredValue[DataContext, ExtendedConnection[GraphQLDeposit]] = {
    DepositResolver.findDeposit(DepositFilters(
      bagName = bagName,
      originFilter = origin,
      stateFilter = state,
      ingestStepFilter = ingestStep,
      doiRegisteredFilter = doiRegistered,
      doiActionFilter = doiAction,
      curatorFilter = curator,
      isNewVersionFilter = isNewVersion,
      curationRequiredFilter = curationRequired,
      curationPerformedFilter = curationPerformed,
      contentTypeFilter = contentType,
    ))
      .map(TimebasedSearchArguments(earlierThan, laterThan, atTimestamp, orderBy))
      .map(deposits => ExtendedConnection.connectionFromSeq(
        deposits.map(new GraphQLDeposit(_)),
        ConnectionArgs(before, after, first, last),
      ))
  }

  @GraphQLField
  @GraphQLDescription("Get the technical metadata related to this depositor.")
  def depositor(@GraphQLDescription("Show deposits from this depositor.") id: DepositorId)
               (implicit ctx: Context[DataContext, Unit]): GraphQLDepositor = {
    new GraphQLDepositor(id)
  }

//  @GraphQLField
//  @GraphQLDescription("List all depositors.")
//  def depositors()(implicit ctx: Context[DataContext, Unit])

  @GraphQLField
  @GraphQLDescription("Find an identifier with the given type and value.")
  def identifier(@GraphQLName("type") @GraphQLDescription("The type of identifier to be found.") idType: IdentifierType.Value,
                 @GraphQLName("value") @GraphQLDescription("The value of the identifier to be found.") idValue: String,
                )(implicit ctx: Context[DataContext, Unit]): DeferredValue[DataContext, Option[GraphQLIdentifier]] = {
    IdentifierResolver.identifierByTypeAndValue(idType, idValue)
      .map(_.map(new GraphQLIdentifier(_)))
  }
}
