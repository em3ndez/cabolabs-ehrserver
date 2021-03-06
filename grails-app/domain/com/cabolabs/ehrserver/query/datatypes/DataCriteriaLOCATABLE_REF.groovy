/*
 * Copyright 2011-2020 CaboLabs Health Informatics
 *
 * The EHRServer was designed and developed by Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> at CaboLabs Health Informatics (www.cabolabs.com).
 *
 * You can't remove this notice from the source code, you can't remove the "Powered by CaboLabs" from the UI, you can't remove this notice from the window that appears then the "Powered by CaboLabs" link is clicked.
 *
 * Any modifications to the provided source code can be stated below this notice.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cabolabs.ehrserver.query.datatypes

import com.cabolabs.ehrserver.query.DataCriteria

class DataCriteriaLOCATABLE_REF extends DataCriteria {

   static String indexType = 'LocatableRefIndex'

   String locatable_ref_pathValue // path to the instruction inside a version, from compo root
   String locatable_ref_pathOperand
   boolean locatable_ref_pathNegation = false

   // For LOCATABLE_REF the querying will be customized because LREF.value depends on instances, so is a param, not part of the query definition.

   DataCriteriaLOCATABLE_REF()
   {
      rmTypeName = 'LOCATABLE_REF'
      alias = 'dlor'
   }

   static constraints = {
   }

   static List criteriaSpec(String archetypeId, String path, boolean returnCodes = true)
   {
      return [
        [
          locatable_ref_path: [
            contains:  'value', // ilike %value%
            eq:  'value'
          ]
        ]
      ]
   }

   static List attributes()
   {
      return ['value']
   }

   static List functions()
   {
      return []
   }

   boolean containsFunction()
   {
      return false
   }
}
