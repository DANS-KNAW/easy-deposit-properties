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
package nl.knaw.dans.easy.properties.app.model

import nl.knaw.dans.easy.properties.app.model.SeriesFilter.SeriesFilter

@deprecated sealed abstract class CurationEvent[T](value: T, timestamp: Timestamp) extends Timestamped

@deprecated case class IsNewVersionEvent(isNewVersion: Option[Boolean], timestamp: Timestamp) extends CurationEvent(isNewVersion, timestamp)
@deprecated case class CurationRequiredEvent(curationRequired: Boolean, timestamp: Timestamp) extends CurationEvent(curationRequired, timestamp)
@deprecated case class CurationPerformedEvent(curationPerformed: Boolean, timestamp: Timestamp) extends CurationEvent(curationPerformed, timestamp)

@deprecated case class DepositIsNewVersionFilter(isNewVersion: Boolean, filter: SeriesFilter = SeriesFilter.LATEST) extends DepositFilter
@deprecated case class DepositCurationRequiredFilter(curationRequired: Boolean, filter: SeriesFilter = SeriesFilter.LATEST) extends DepositFilter
@deprecated case class DepositCurationPerformedFilter(curationPerformed: Boolean, filter: SeriesFilter = SeriesFilter.LATEST) extends DepositFilter
