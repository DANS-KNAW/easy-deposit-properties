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
package nl.knaw.dans.easy.properties.app.model.curation

import nl.knaw.dans.easy.properties.app.model.curator.Curator
import nl.knaw.dans.easy.properties.app.model.{ CurationPerformedEvent, CurationRequiredEvent, IsNewVersionEvent, Timestamp, Timestamped }
import sangria.relay.Node

@deprecated
case class Curation(id: String,
                    isNewVersion: Option[Boolean],
                    isRequired: Boolean,
                    isPerformed: Boolean,
                    datamanagerUserId: String,
                    datamanagerEmail: String,
                    timestamp: Timestamp,
                   ) extends Node with Timestamped {

  @deprecated
  def getCurator: Curator = {
    Curator(id, datamanagerUserId, datamanagerEmail, timestamp)
  }

  @deprecated
  def getIsNewVersionEvent: IsNewVersionEvent = {
    IsNewVersionEvent(isNewVersion, timestamp)
  }

  @deprecated
  def getCurationRequiredEvent: CurationRequiredEvent = {
    CurationRequiredEvent(isRequired, timestamp)
  }

  @deprecated
  def getCurationPerformedEvent: CurationPerformedEvent = {
    CurationPerformedEvent(isPerformed, timestamp)
  }
}
