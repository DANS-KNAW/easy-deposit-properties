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
package nl.knaw.dans.easy.properties.app.repository

import nl.knaw.dans.easy.properties.app.model.ingestStep.{ IngestStep, InputIngestStep }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }

trait IngestStepDao {

  def getById(ids: Seq[String]): QueryErrorOr[Seq[IngestStep]]

  def getCurrent(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, IngestStep)]]

  def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[IngestStep])]]

  def store(id: DepositId, step: InputIngestStep): MutationErrorOr[IngestStep]

  def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Deposit)]]
}
