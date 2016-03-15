package com.cabolabs.ehrserver.api

import static org.junit.Assert.*
import com.cabolabs.ehrserver.openehr.demographic.Person
import grails.test.mixin.*
import grails.test.mixin.support.*
import com.cabolabs.ehrserver.ehr.clinical_documents.*
import com.cabolabs.ehrserver.openehr.common.change_control.*
import com.cabolabs.ehrserver.openehr.common.generic.*

import org.junit.*
import org.springframework.mock.web.MockMultipartFile

import com.cabolabs.ehrserver.ehr.clinical_documents.data.*
import grails.util.Holders
import com.cabolabs.ehrserver.query.*

import com.cabolabs.ehrserver.api.RestController
import com.cabolabs.ehrserver.openehr.ehr.Ehr
import com.cabolabs.security.Organization

import com.cabolabs.ehrserver.parsers.XmlService
import com.cabolabs.ehrserver.parsers.XmlValidationService


/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(RestController)
@Mock([ Ehr,Person,Organization,
        PatientProxy, DoctorProxy,
        OperationalTemplateIndex, OperationalTemplateIndexItem, ArchetypeIndexItem, Contribution, VersionedComposition, Version, CompositionIndex, AuditDetails,
        DataValueIndex, DvQuantityIndex, DvCountIndex, DvProportionIndex, DvTextIndex, DvCodedTextIndex, DvDateTimeIndex, DvBooleanIndex,
        Query, DataGet, DataCriteria
      ])
class RestController2Tests {

   private static String PS = System.getProperty("file.separator")
   private static String patientUid = 'a86ac702-980a-478c-8f16-927fd4a5e9ae'
   
   // grailsApplication is injected by the controller test mixin
   // http://stackoverflow.com/questions/18614893/how-to-access-grailsapplication-and-applicationcontext-in-functional-tests
   def config = grailsApplication.config.app //Holders.config.app
   
   void setUp()
	{
      println "setUp"
      
      controller.xmlService = new XmlService()
      controller.xmlService.xmlValidationService = new XmlValidationService()
      
      // Sample organizations
      def hospital = new Organization(name: 'Hospital de Clinicas', number: '1234')
      def clinic = new Organization(name: 'Clinica del Tratamiento del Dolor', number: '6666')
      def practice = new Organization(name: 'Cirugia Estetica', number: '5555')
      
      hospital.save(failOnError:true, flush:true)
      clinic.save(failOnError:true, flush:true)
      practice.save(failOnError:true, flush:true)
      
      
	   // Copiado de bootstrap porque no agarra las instancias en testing.
		def persons = [
         new Person(
            firstName: 'Pablo',
            lastName: 'Pazos',
            dob: new Date(81, 9, 24),
            sex: 'M',
            idCode: '4116238-0',
            idType: 'CI',
            role: 'pat',
            uid: patientUid,
            organizationUid: hospital.uid
         ),
         new Person(
            firstName: 'Barbara',
            lastName: 'Cardozo',
            dob: new Date(87, 2, 19),
            sex: 'F',
            idCode: '1234567-0',
            idType: 'CI',
            role: 'pat',
            uid: '22222222-1111-1111-1111-111111111111',
            organizationUid: hospital.uid
         ),
         new Person(
            firstName: 'Carlos',
            lastName: 'Cardozo',
            dob: new Date(80, 2, 20),
            sex: 'M',
            idCode: '3453455-0',
            idType: 'CI',
            role: 'pat',
            uid: '33333333-1111-1111-1111-111111111111',
            organizationUid: hospital.uid
         )
         ,
         new Person(
            firstName: 'Mario',
            lastName: 'Gomez',
            dob: new Date(64, 8, 19),
            sex: 'M',
            idCode: '5677565-0',
            idType: 'CI',
            role: 'pat',
            uid: '44444444-1111-1111-1111-111111111111',
            organizationUid: hospital.uid
         )
         ,
         new Person(
            firstName: 'Carla',
            lastName: 'Martinez',
            dob: new Date(92, 1, 5),
            sex: 'F',
            idCode: '84848884-0',
            idType: 'CI',
            role: 'pat',
            uid: '55555555-1111-1111-1111-111111111111',
            organizationUid: hospital.uid
         )
      ]
      
      persons.each { p ->
         
         if (!p.save())
         {
            println p.errors
         }
      }
	  
	  
	  // Crea EHRs para los pacientes de prueba
	  // Idem EhrController.createEhr
	  def ehr
	  persons.eachWithIndex { p, i ->
	  
	    if (p.role == 'pat')
		 {
			ehr = new Ehr(
			   subject: new PatientProxy(
			      value: p.uid
			   ),
            organizationUid: p.organizationUid
		    )
         
          if (!ehr.save()) println ehr.errors
		 }
	  }
     
     
     // Setup queries for testing
     
     def compositionQuery = new Query(
        name: "test query composition",
        type: "composition",
        where: [])
     if (!compositionQuery.save()) println compositionQuery.errors
   }

   void tearDown()
	{
      // Tear down logic here
      def version_repo = new File(config.version_repo)
      
      version_repo.eachFile {
         it.delete()
      }
   }

	void testPatientList()
	{
	    // Personas creados en el setUp
	    assert Person.count() == 5
		
		// Formato XML por defecto
		controller.patientList()
		println groovy.xml.XmlUtil.serialize( controller.response.text )
		assert controller.response.xml.patients.patient.size() == 5
		response.reset()
		
		
		// Formato incorrecto
		params.format = 'text'
		controller.patientList()
		assert controller.response.xml.code.text() == "error"
		response.reset()
	}
	

   void testEhrList()
	{
	    // EHRs creados en el setUp
	    assert Ehr.count() == 5
	
	
	    // sin format devuelve XML por defecto
		controller.ehrList()
		
		assert controller.response.contentType == "text/xml;charset=UTF-8"
		
		// pretty print del XML
		// Se puede cambiar la opcion en Config: 
		// test {
	    //   grails.converters.default.pretty.print = true
	    // }
		println groovy.xml.XmlUtil.serialize( controller.response.text )
		//println controller.response.text
		
		// ehrs debe tener 5 tags ehr
		// con .text es solo el texto, con .xml es el xml :)
		assert controller.response.xml.ehrs.ehr.size() == 5
		response.reset()
		
		
		// para que withFormat considere el param format> controller.request.format = 'json' 
		// http://grails.1312388.n4.nabble.com/withFormat-tests-in-ControllerUnitTestCase-td3343763.html
		params.format = 'json'
		controller.ehrList()
		assert controller.response.contentType == 'application/json;charset=UTF-8'
		println controller.response.text
		
		// json es un array y debe tener 5 objetos
		assert controller.response.json.ehrs.size() == 5
		response.reset()
		
		
		// Debe tirar error en XML porque no es un formato recocnocido
		params.format = 'text'
		controller.ehrList()
		//println controller.response.text
		//println groovy.xml.XmlUtil.serialize( controller.response.text )
      /*
       * <?xml version="1.0" encoding="UTF-8" ?><result>
           <code>error</code>
           <message>formato 'text' no reconocido, debe ser exactamente 'xml' o 'json'</message>
         </result>
       */
		assert controller.response.xml.code.text() == "error"
		response.reset()
		
		
		// Prueba paginacion con max=3
		params.format = 'xml'
		params.max = 3
		controller.ehrList()
		assert controller.response.xml.ehrs.ehr.size() == 3
		response.reset()
		
		// Prueba paginacion con offset=3 debe devolver 2 ehrs porque hay 5
		params.format = 'xml'
		params.offset = 3
		controller.ehrList()
		assert controller.response.xml.ehrs.ehr.size() == 2
		response.reset()
    }
   
   
   void testCommitHugeCompo()
   {
      def oti = new com.cabolabs.archetype.OperationalTemplateIndexer()
      def opt = new File( "opts" + PS + "tests" + PS + "RIPPLE - Conformance Test template.opt" )
      oti.index(opt)
      
      request.method = 'POST'
      request.contentType = 'text/xml'
      request.xml = $/<?xml version="1.0" encoding="UTF-8" ?>
<versions xmlns="http://schemas.openehr.org/v1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<version xsi:type="ORIGINAL_VERSION">
  <contribution>
    <id xsi:type="HIER_OBJECT_ID">
      <value>ad6866e1-fb08-4e9b-a93b-5095a2563789</value>
    </id>
    <namespace>EHR::COMMON</namespace>
    <type>CONTRIBUTION</type>
  </contribution>
  <commit_audit>
    <system_id>CABOLABS_EHR</system_id>
    <committer xsi:type="PARTY_IDENTIFIED">
      <external_ref>
        <id xsi:type="HIER_OBJECT_ID">
          <value>cc193f71-f5fe-438a-87f9-81ecb302eede</value>
        </id>
        <namespace>DEMOGRAPHIC</namespace>
        <type>PERSON</type>
      </external_ref>
      <name>Dr. Pablo Pazos</name>
    </committer>
    <time_committed>
      <value>20140901T233114,065-0300</value>
    </time_committed>
    <change_type>
      <value>creation</value>
      <defining_code>
        <terminology_id>
          <value>openehr</value>
        </terminology_id>
        <code_string>249</code_string>
      </defining_code>
    </change_type>
  </commit_audit>
  <uid>
    <value>f16dd9db-b2cd-4e68-b08d-38bea43751b9::ripple_osi.ehrscape.c4h::1</value>
  </uid>
  <data xsi:type="COMPOSITION" archetype_node_id="openEHR-EHR-COMPOSITION.encounter.v1">
        <name>
            <value>Encounter</value>
        </name>
        <uid xsi:type="OBJECT_VERSION_ID">
            <value>f16dd9db-b2cd-4e68-b08d-38bea43751b9::ripple_osi.ehrscape.c4h::1</value>
        </uid>
        <archetype_details>
            <archetype_id>
                <value>openEHR-EHR-COMPOSITION.encounter.v1</value>
            </archetype_id>
            <template_id>
                <value>RIPPLE - Conformance Test template</value>
            </template_id>
            <rm_version>1.0.1</rm_version>
        </archetype_details>
        <language>
            <terminology_id>
                <value>ISO_639-1</value>
            </terminology_id>
            <code_string>en</code_string>
        </language>
        <territory>
            <terminology_id>
                <value>ISO_3166-1</value>
            </terminology_id>
            <code_string>GB</code_string>
        </territory>
        <category>
            <value>event</value>
            <defining_code>
                <terminology_id>
                    <value>openehr</value>
                </terminology_id>
                <code_string>433</code_string>
            </defining_code>
        </category>
        <composer xsi:type="PARTY_IDENTIFIED">
            <name>Silvia Blake</name>
        </composer>
        <context>
            <start_time>
                <value>2015-12-02T17:41:56.809Z</value>
            </start_time>
            <setting>
                <value>other care</value>
                <defining_code>
                    <terminology_id>
                        <value>openehr</value>
                    </terminology_id>
                    <code_string>238</code_string>
                </defining_code>
            </setting>
            <other_context xsi:type="ITEM_TREE" archetype_node_id="at0001">
                <name>
                    <value>Tree</value>
                </name>
                <items xsi:type="CLUSTER" archetype_node_id="openEHR-EHR-CLUSTER.composition_context_detail.v1">
                    <name>
                        <value>Context detail</value>
                    </name>
                    <archetype_details>
                        <archetype_id>
                            <value>openEHR-EHR-CLUSTER.composition_context_detail.v1</value>
                        </archetype_id>
                        <rm_version>1.0.1</rm_version>
                    </archetype_details>
                    <items xsi:type="ELEMENT" archetype_node_id="at0001">
                        <name>
                            <value>Period of care identifier</value>
                        </name>
                        <value xsi:type="DV_TEXT">
                            <value>Ident. 52</value>
                        </value>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at0008">
                        <name>
                            <value>Tags</value>
                        </name>
                        <value xsi:type="DV_TEXT">
                            <value>Tags 96</value>
                        </value>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at0008">
                        <name>
                            <value>Tags #2</value>
                        </name>
                        <value xsi:type="DV_TEXT">
                            <value>Tags 96</value>
                        </value>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at0009">
                        <name>
                            <value>Attachment</value>
                        </name>
                        <value xsi:type="DV_MULTIMEDIA">
                            <alternate_text>alternate text</alternate_text>
                            <uri>
                                <value>http://med.tube.com/sample</value>
                            </uri>
                            <media_type>
                                <terminology_id>
                                    <value>IANA_media-types</value>
                                </terminology_id>
                                <code_string>video/mp4</code_string>
                            </media_type>
                            <size>504903212</size>
                        </value>
                    </items>
                </items>
            </other_context>
            <health_care_facility>
                <external_ref>
                    <id xsi:type="GENERIC_ID">
                        <value>9091</value>
                        <scheme>HOSPITAL-NS</scheme>
                    </id>
                    <namespace>HOSPITAL-NS</namespace>
                    <type>PARTY</type>
                </external_ref>
                <name>Hospital</name>
            </health_care_facility>
        </context>
        <content xsi:type="SECTION" archetype_node_id="openEHR-EHR-SECTION.adhoc.v1">
            <name>
                <value>Ad hoc heading</value>
            </name>
            <archetype_details>
                <archetype_id>
                    <value>openEHR-EHR-SECTION.adhoc.v1</value>
                </archetype_id>
                <rm_version>1.0.1</rm_version>
            </archetype_details>
            <items xsi:type="OBSERVATION" archetype_node_id="openEHR-EHR-OBSERVATION.pulse.v1">
                <name>
                    <value>Pulse</value>
                </name>
                <archetype_details>
                    <archetype_id>
                        <value>openEHR-EHR-OBSERVATION.pulse.v1</value>
                    </archetype_id>
                    <rm_version>1.0.1</rm_version>
                </archetype_details>
                <language>
                    <terminology_id>
                        <value>ISO_639-1</value>
                    </terminology_id>
                    <code_string>en</code_string>
                </language>
                <encoding>
                    <terminology_id>
                        <value>IANA_character-sets</value>
                    </terminology_id>
                    <code_string>UTF-8</code_string>
                </encoding>
                <subject xsi:type="PARTY_SELF"/>
                <protocol xsi:type="ITEM_TREE" archetype_node_id="at0010">
                    <name>
                        <value>List</value>
                    </name>
                    <items xsi:type="ELEMENT" archetype_node_id="at1019">
                        <name>
                            <value>Method</value>
                        </name>
                        <value xsi:type="DV_CODED_TEXT">
                            <value>Device</value>
                            <defining_code>
                                <terminology_id>
                                    <value>local</value>
                                </terminology_id>
                                <code_string>at1034</code_string>
                            </defining_code>
                        </value>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at1037">
                        <name>
                            <value>Findings Location</value>
                        </name>
                        <value xsi:type="DV_CODED_TEXT">
                            <value>Femoral Artery - Left</value>
                            <defining_code>
                                <terminology_id>
                                    <value>local</value>
                                </terminology_id>
                                <code_string>at1043</code_string>
                            </defining_code>
                        </value>
                    </items>
                    <items xsi:type="CLUSTER" archetype_node_id="openEHR-EHR-CLUSTER.device.v1">
                        <name>
                            <value>Medical Device</value>
                        </name>
                        <archetype_details>
                            <archetype_id>
                                <value>openEHR-EHR-CLUSTER.device.v1</value>
                            </archetype_id>
                            <rm_version>1.0.1</rm_version>
                        </archetype_details>
                        <items xsi:type="ELEMENT" archetype_node_id="at0001">
                            <name>
                                <value>Device name</value>
                            </name>
                            <value xsi:type="DV_TEXT">
                                <value>Device name 33</value>
                            </value>
                        </items>
                        <items xsi:type="ELEMENT" archetype_node_id="at0021">
                            <name>
                                <value>Unique device identifier (UDI)</value>
                            </name>
                            <value xsi:type="DV_IDENTIFIER">
                                <issuer>Issuer</issuer>
                                <assigner>Assigner</assigner>
                                <id>59466e78-d15c-4765-87da-20e56f1413a6</id>
                                <type>Prescription</type>
                            </value>
                        </items>
                        <items xsi:type="ELEMENT" archetype_node_id="at0004">
                            <name>
                                <value>Manufacturer</value>
                            </name>
                            <value xsi:type="DV_TEXT">
                                <value>Manufacturer 6</value>
                            </value>
                        </items>
                        <items xsi:type="ELEMENT" archetype_node_id="at0005">
                            <name>
                                <value>Date of manufacture</value>
                            </name>
                            <value xsi:type="DV_DATE_TIME">
                                <value>2015-12-02T17:41:56.811Z</value>
                            </value>
                        </items>
                    </items>
                </protocol>
                <data archetype_node_id="at0002">
                    <name>
                        <value>history</value>
                    </name>
                    <origin>
                        <value>2015-12-02T17:41:56.809Z</value>
                    </origin>
                    <events xsi:type="POINT_EVENT" archetype_node_id="at0003">
                        <name>
                            <value>First event</value>
                        </name>
                        <time>
                            <value>2015-12-02T17:41:56.809Z</value>
                        </time>
                        <data xsi:type="ITEM_TREE" archetype_node_id="at0001">
                            <name>
                                <value>structure</value>
                            </name>
                            <items xsi:type="ELEMENT" archetype_node_id="at0004">
                                <name xsi:type="DV_CODED_TEXT">
                                    <value>Heart Rate</value>
                                    <defining_code>
                                        <terminology_id>
                                            <value>local</value>
                                        </terminology_id>
                                        <code_string>at1027</code_string>
                                    </defining_code>
                                </name>
                                <value xsi:type="DV_QUANTITY">
                                    <magnitude>53.0</magnitude>
                                    <units>/min</units>
                                    <precision>0</precision>
                                </value>
                            </items>
                            <items xsi:type="ELEMENT" archetype_node_id="at0005">
                                <name>
                                    <value>Regularity</value>
                                </name>
                                <value xsi:type="DV_CODED_TEXT">
                                    <value>Regularly Irregular</value>
                                    <defining_code>
                                        <terminology_id>
                                            <value>local</value>
                                        </terminology_id>
                                        <code_string>at0007</code_string>
                                    </defining_code>
                                </value>
                            </items>
                            <items xsi:type="ELEMENT" archetype_node_id="at1022">
                                <name>
                                    <value>Clinical Description</value>
                                </name>
                                <value xsi:type="DV_TEXT">
                                    <value>Clinical Description 14</value>
                                </value>
                            </items>
                            <items xsi:type="ELEMENT" archetype_node_id="at1023">
                                <name>
                                    <value>Clinical Interpretation</value>
                                </name>
                                <value xsi:type="DV_TEXT">
                                    <value>Clinical Interpretation 23</value>
                                </value>
                            </items>
                        </data>
                    </events>
                    <events xsi:type="POINT_EVENT" archetype_node_id="at0003">
                        <name>
                            <value>Second event</value>
                        </name>
                        <time>
                            <value>2015-12-02T17:41:56.809Z</value>
                        </time>
                        <data xsi:type="ITEM_TREE" archetype_node_id="at0001">
                            <name>
                                <value>structure</value>
                            </name>
                            <items xsi:type="ELEMENT" archetype_node_id="at1005">
                                <name>
                                    <value>Pulse Presence</value>
                                </name>
                                <value xsi:type="DV_CODED_TEXT">
                                    <value>Present</value>
                                    <defining_code>
                                        <terminology_id>
                                            <value>local</value>
                                        </terminology_id>
                                        <code_string>at1024</code_string>
                                    </defining_code>
                                </value>
                            </items>
                            <items xsi:type="ELEMENT" archetype_node_id="at0004">
                                <name xsi:type="DV_CODED_TEXT">
                                    <value>Pulse Rate</value>
                                    <defining_code>
                                        <terminology_id>
                                            <value>local</value>
                                        </terminology_id>
                                        <code_string>at1026</code_string>
                                    </defining_code>
                                </name>
                                <value xsi:type="DV_QUANTITY">
                                    <magnitude>3.0</magnitude>
                                    <units>/min</units>
                                    <precision>0</precision>
                                </value>
                            </items>
                            <items xsi:type="ELEMENT" archetype_node_id="at0005">
                                <name>
                                    <value>Regularity</value>
                                </name>
                                <value xsi:type="DV_CODED_TEXT">
                                    <value>Regularly Irregular</value>
                                    <defining_code>
                                        <terminology_id>
                                            <value>local</value>
                                        </terminology_id>
                                        <code_string>at0007</code_string>
                                    </defining_code>
                                </value>
                            </items>
                            <items xsi:type="ELEMENT" archetype_node_id="at1023">
                                <name>
                                    <value>Clinical Interpretation</value>
                                </name>
                                <value xsi:type="DV_TEXT">
                                    <value>Clinical Interpretation 34</value>
                                </value>
                            </items>
                        </data>
                    </events>
                    <events xsi:type="INTERVAL_EVENT" archetype_node_id="at1036">
                        <name>
                            <value>Maximum</value>
                        </name>
                        <time>
                            <value>2015-12-02T17:41:56.809Z</value>
                        </time>
                        <data xsi:type="ITEM_TREE" archetype_node_id="at0001">
                            <name>
                                <value>structure</value>
                            </name>
                            <items xsi:type="ELEMENT" archetype_node_id="at0004">
                                <name xsi:type="DV_CODED_TEXT">
                                    <value>Heart Rate</value>
                                    <defining_code>
                                        <terminology_id>
                                            <value>local</value>
                                        </terminology_id>
                                        <code_string>at1027</code_string>
                                    </defining_code>
                                </name>
                                <value xsi:type="DV_QUANTITY">
                                    <magnitude>16.0</magnitude>
                                    <units>/min</units>
                                    <precision>0</precision>
                                </value>
                            </items>
                        </data>
                        <state xsi:type="ITEM_TREE" archetype_node_id="at0012">
                            <name>
                                <value>List</value>
                            </name>
                            <items xsi:type="ELEMENT" archetype_node_id="at0013">
                                <name>
                                    <value>Position</value>
                                </name>
                                <value xsi:type="DV_CODED_TEXT">
                                    <value>Sitting</value>
                                    <defining_code>
                                        <terminology_id>
                                            <value>local</value>
                                        </terminology_id>
                                        <code_string>at1001</code_string>
                                    </defining_code>
                                </value>
                            </items>
                            <items xsi:type="ELEMENT" archetype_node_id="at1018">
                                <name>
                                    <value>Confounding Factors</value>
                                </name>
                                <value xsi:type="DV_TEXT">
                                    <value>Confounding Factors 16</value>
                                </value>
                            </items>
                        </state>
                        <width>
                            <value>P1DT11H11M</value>
                        </width>
                        <math_function>
                            <value>maximum</value>
                            <defining_code>
                                <terminology_id>
                                    <value>openehr</value>
                                </terminology_id>
                                <code_string>144</code_string>
                            </defining_code>
                        </math_function>
                    </events>
                </data>
            </items>
            <items xsi:type="EVALUATION" archetype_node_id="openEHR-EHR-EVALUATION.cpr_decision_uk.v1">
                <name>
                    <value>CPR decision</value>
                </name>
                <archetype_details>
                    <archetype_id>
                        <value>openEHR-EHR-EVALUATION.cpr_decision_uk.v1</value>
                    </archetype_id>
                    <rm_version>1.0.1</rm_version>
                </archetype_details>
                <language>
                    <terminology_id>
                        <value>ISO_639-1</value>
                    </terminology_id>
                    <code_string>en</code_string>
                </language>
                <encoding>
                    <terminology_id>
                        <value>IANA_character-sets</value>
                    </terminology_id>
                    <code_string>UTF-8</code_string>
                </encoding>
                <subject xsi:type="PARTY_SELF"/>
                <protocol xsi:type="ITEM_TREE" archetype_node_id="at0010">
                    <name>
                        <value>Tree</value>
                    </name>
                    <items xsi:type="ELEMENT" archetype_node_id="at0014">
                        <name>
                            <value>Discussion with informal carer</value>
                        </name>
                        <value xsi:type="DV_CODED_TEXT">
                            <value>Resuscitation not discussed with informal carer</value>
                            <defining_code>
                                <terminology_id>
                                    <value>local</value>
                                </terminology_id>
                                <code_string>at0024</code_string>
                            </defining_code>
                        </value>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at0009">
                        <name>
                            <value>Date for review of CPR decision</value>
                        </name>
                        <value xsi:type="DV_DATE_TIME">
                            <value>2015-12-02T17:41:56.811Z</value>
                        </value>
                    </items>
                </protocol>
                <data xsi:type="ITEM_TREE" archetype_node_id="at0001">
                    <name>
                        <value>Tree</value>
                    </name>
                    <items xsi:type="ELEMENT" archetype_node_id="at0003">
                        <name>
                            <value>CPR decision</value>
                        </name>
                        <value xsi:type="DV_CODED_TEXT">
                            <value>CPR decision status unknown</value>
                            <defining_code>
                                <terminology_id>
                                    <value>local</value>
                                </terminology_id>
                                <code_string>at0022</code_string>
                            </defining_code>
                        </value>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at0002">
                        <name>
                            <value>Date of CPR decision</value>
                        </name>
                        <value xsi:type="DV_DATE_TIME">
                            <value>2015-12-02T17:41:56.811Z</value>
                        </value>
                    </items>
                </data>
            </items>
            <items xsi:type="INSTRUCTION" archetype_node_id="openEHR-EHR-INSTRUCTION.request-procedure.v1">
                <name>
                    <value>Procedure request</value>
                </name>
                <uid xsi:type="HIER_OBJECT_ID">
                    <value>9a3871f8-8105-44f9-a06c-5626acaf40a3</value>
                </uid>
                <archetype_details>
                    <archetype_id>
                        <value>openEHR-EHR-INSTRUCTION.request-procedure.v1</value>
                    </archetype_id>
                    <rm_version>1.0.1</rm_version>
                </archetype_details>
                <language>
                    <terminology_id>
                        <value>ISO_639-1</value>
                    </terminology_id>
                    <code_string>en</code_string>
                </language>
                <encoding>
                    <terminology_id>
                        <value>IANA_character-sets</value>
                    </terminology_id>
                    <code_string>UTF-8</code_string>
                </encoding>
                <subject xsi:type="PARTY_SELF"/>
                <other_participations>
                    <function>
                        <value>performer</value>
                    </function>
                    <performer xsi:type="PARTY_IDENTIFIED">
                        <name>Nurse Bailey</name>
                    </performer>
                    <mode>
                        <value>not specified</value>
                        <defining_code>
                            <terminology_id>
                                <value>openehr</value>
                            </terminology_id>
                            <code_string>193</code_string>
                        </defining_code>
                    </mode>
                </other_participations>
                <protocol xsi:type="ITEM_TREE" archetype_node_id="at0008">
                    <name>
                        <value>Tree</value>
                    </name>
                    <items xsi:type="CLUSTER" archetype_node_id="openEHR-EHR-CLUSTER.distribution.v1">
                        <name>
                            <value>Distribution</value>
                        </name>
                        <archetype_details>
                            <archetype_id>
                                <value>openEHR-EHR-CLUSTER.distribution.v1</value>
                            </archetype_id>
                            <rm_version>1.0.1</rm_version>
                        </archetype_details>
                        <items xsi:type="ELEMENT" archetype_node_id="at0008">
                            <name>
                                <value>Group category</value>
                            </name>
                            <value xsi:type="DV_TEXT">
                                <value>Group category 26</value>
                            </value>
                        </items>
                        <items xsi:type="ELEMENT" archetype_node_id="at0012">
                            <name>
                                <value>Urgent</value>
                            </name>
                            <value xsi:type="DV_BOOLEAN">
                                <value>true</value>
                            </value>
                        </items>
                        <items xsi:type="ELEMENT" archetype_node_id="at0006">
                            <name>
                                <value>Date sent</value>
                            </name>
                            <value xsi:type="DV_DATE_TIME">
                                <value>2015-12-02T17:41:56.812Z</value>
                            </value>
                        </items>
                    </items>
                </protocol>
                <narrative>
                    <value>Human readable instruction narrative</value>
                </narrative>
                <activities archetype_node_id="at0001">
                    <name>
                        <value>Request</value>
                    </name>
                    <description xsi:type="ITEM_TREE" archetype_node_id="at0009">
                        <name>
                            <value>Tree</value>
                        </name>
                        <items xsi:type="ELEMENT" archetype_node_id="at0121">
                            <name>
                                <value>Procedure requested</value>
                            </name>
                            <value xsi:type="DV_TEXT">
                                <value>Procedure requested 79</value>
                            </value>
                        </items>
                        <items xsi:type="ELEMENT" archetype_node_id="at0144">
                            <name>
                                <value>Latest date service required</value>
                            </name>
                            <value xsi:type="DV_DATE_TIME">
                                <value>2015-12-02T17:41:56.812Z</value>
                            </value>
                        </items>
                        <items xsi:type="ELEMENT" archetype_node_id="at0076">
                            <name>
                                <value>Supplementary information to follow</value>
                            </name>
                            <value xsi:type="DV_BOOLEAN">
                                <value>true</value>
                            </value>
                        </items>
                    </description>
                    <timing>
                        <value>R2/2015-12-02T17:00:00Z/P3M</value>
                        <formalism>timing</formalism>
                    </timing>
                    <action_archetype_id>/.*/</action_archetype_id>
                </activities>
                <activities archetype_node_id="at0001">
                    <name>
                        <value>Request #3</value>
                    </name>
                    <description xsi:type="ITEM_TREE" archetype_node_id="at0009">
                        <name>
                            <value>Tree</value>
                        </name>
                        <items xsi:type="ELEMENT" archetype_node_id="at0121">
                            <name>
                                <value>Procedure requested</value>
                            </name>
                            <value xsi:type="DV_TEXT">
                                <value>Procedure requested 84</value>
                            </value>
                        </items>
                        <items xsi:type="ELEMENT" archetype_node_id="at0144">
                            <name>
                                <value>Latest date service required</value>
                            </name>
                            <value xsi:type="DV_DATE_TIME">
                                <value>2015-12-02T17:41:56.812Z</value>
                            </value>
                        </items>
                        <items xsi:type="ELEMENT" archetype_node_id="at0076">
                            <name>
                                <value>Supplementary information to follow</value>
                            </name>
                            <value xsi:type="DV_BOOLEAN">
                                <value>true</value>
                            </value>
                        </items>
                    </description>
                    <timing>
                        <value>R2/2015-12-02T17:00:00Z/P1M</value>
                        <formalism>timing</formalism>
                    </timing>
                    <action_archetype_id>/.*/</action_archetype_id>
                </activities>
            </items>
            <items xsi:type="ACTION" archetype_node_id="openEHR-EHR-ACTION.procedure.v1">
                <name>
                    <value>Procedure</value>
                </name>
                <archetype_details>
                    <archetype_id>
                        <value>openEHR-EHR-ACTION.procedure.v1</value>
                    </archetype_id>
                    <rm_version>1.0.1</rm_version>
                </archetype_details>
                <language>
                    <terminology_id>
                        <value>ISO_639-1</value>
                    </terminology_id>
                    <code_string>en</code_string>
                </language>
                <encoding>
                    <terminology_id>
                        <value>IANA_character-sets</value>
                    </terminology_id>
                    <code_string>UTF-8</code_string>
                </encoding>
                <subject xsi:type="PARTY_SELF"/>
                <protocol xsi:type="ITEM_TREE" archetype_node_id="at0053">
                    <name>
                        <value>Tree</value>
                    </name>
                    <items xsi:type="ELEMENT" archetype_node_id="at0054">
                        <name>
                            <value>Requestor order identifier</value>
                        </name>
                        <value xsi:type="DV_TEXT">
                            <value>Ident. 38</value>
                        </value>
                    </items>
                    <items xsi:type="CLUSTER" archetype_node_id="openEHR-EHR-CLUSTER.person_name.v1">
                        <name>
                            <value>Person name</value>
                        </name>
                        <archetype_details>
                            <archetype_id>
                                <value>openEHR-EHR-CLUSTER.person_name.v1</value>
                            </archetype_id>
                            <rm_version>1.0.1</rm_version>
                        </archetype_details>
                        <items xsi:type="ELEMENT" archetype_node_id="at0006">
                            <name>
                                <value>Name type</value>
                            </name>
                            <value xsi:type="DV_CODED_TEXT">
                                <value>AKA</value>
                                <defining_code>
                                    <terminology_id>
                                        <value>local</value>
                                    </terminology_id>
                                    <code_string>at0010</code_string>
                                </defining_code>
                            </value>
                        </items>
                        <items xsi:type="ELEMENT" archetype_node_id="at0022">
                            <name>
                                <value>Preferred name</value>
                            </name>
                            <value xsi:type="DV_BOOLEAN">
                                <value>true</value>
                            </value>
                        </items>
                        <items xsi:type="ELEMENT" archetype_node_id="at0001">
                            <name>
                                <value>Unstructured name</value>
                            </name>
                            <value xsi:type="DV_TEXT">
                                <value>Unstructured name 16</value>
                            </value>
                        </items>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at0056">
                        <name>
                            <value>Receiver order identifier</value>
                        </name>
                        <value xsi:type="DV_IDENTIFIER">
                            <issuer>Issuer</issuer>
                            <assigner>Assigner</assigner>
                            <id>8fb20ec3-b285-4ab8-acd4-25bcd58cbd24</id>
                            <type>Prescription</type>
                        </value>
                    </items>
                </protocol>
                <time>
                    <value>2015-12-02T17:41:56.813Z</value>
                </time>
                <description xsi:type="ITEM_TREE" archetype_node_id="at0001">
                    <name>
                        <value>Tree</value>
                    </name>
                    <items xsi:type="ELEMENT" archetype_node_id="at0002">
                        <name>
                            <value>Procedure name</value>
                        </name>
                        <value xsi:type="DV_TEXT">
                            <value>Procedure name 40</value>
                        </value>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at0065">
                        <name xsi:type="DV_CODED_TEXT">
                            <value>Run-time coded name</value>
                            <defining_code>
                                <terminology_id>
                                    <value>SNOMED-CT</value>
                                </terminology_id>
                                <code_string>70901006</code_string>
                            </defining_code>
                        </name>
                        <value xsi:type="DV_TEXT">
                            <value>Method 0</value>
                        </value>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at0065">
                        <name>
                            <value>Method #1</value>
                        </name>
                        <value xsi:type="DV_TEXT">
                            <value>Method #1 94</value>
                        </value>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at0066">
                        <name>
                            <value>Scheduled date/time</value>
                        </name>
                        <value xsi:type="DV_DATE_TIME">
                            <value>2015-12-02T17:41:56.812Z</value>
                        </value>
                    </items>
                </description>
                <ism_transition>
                    <current_state>
                        <value>initial</value>
                        <defining_code>
                            <terminology_id>
                                <value>openehr</value>
                            </terminology_id>
                            <code_string>524</code_string>
                        </defining_code>
                    </current_state>
                </ism_transition>
            </items>
            <items xsi:type="ADMIN_ENTRY" archetype_node_id="openEHR-EHR-ADMIN_ENTRY.inpatient_admission_uk.v1">
                <name>
                    <value>Inpatient admission</value>
                </name>
                <archetype_details>
                    <archetype_id>
                        <value>openEHR-EHR-ADMIN_ENTRY.inpatient_admission_uk.v1</value>
                    </archetype_id>
                    <rm_version>1.0.1</rm_version>
                </archetype_details>
                <language>
                    <terminology_id>
                        <value>ISO_639-1</value>
                    </terminology_id>
                    <code_string>en</code_string>
                </language>
                <encoding>
                    <terminology_id>
                        <value>IANA_character-sets</value>
                    </terminology_id>
                    <code_string>UTF-8</code_string>
                </encoding>
                <subject xsi:type="PARTY_SELF"/>
                <data xsi:type="ITEM_TREE" archetype_node_id="at0001">
                    <name>
                        <value>Tree</value>
                    </name>
                    <items xsi:type="ELEMENT" archetype_node_id="at0002">
                        <name>
                            <value>Date of admission</value>
                        </name>
                        <value xsi:type="DV_DATE_TIME">
                            <value>2015-12-02T17:41:56.813Z</value>
                        </value>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at0003">
                        <name>
                            <value>Admission method</value>
                        </name>
                        <value xsi:type="DV_CODED_TEXT">
                            <value>D.80 description</value>
                            <defining_code>
                                <terminology_id>
                                    <value>external_terminology</value>
                                </terminology_id>
                                <code_string>D.80</code_string>
                            </defining_code>
                        </value>
                    </items>
                    <items xsi:type="ELEMENT" archetype_node_id="at0009">
                        <name>
                            <value>Source of admission</value>
                        </name>
                        <value xsi:type="DV_TEXT">
                            <value>Source of admission 42</value>
                        </value>
                    </items>
                </data>
            </items>
            <items xsi:type="OBSERVATION" archetype_node_id="openEHR-EHR-OBSERVATION.demo.v1">
                <name>
                    <value>Demonstration</value>
                </name>
                <archetype_details>
                    <archetype_id>
                        <value>openEHR-EHR-OBSERVATION.demo.v1</value>
                    </archetype_id>
                    <rm_version>1.0.1</rm_version>
                </archetype_details>
                <language>
                    <terminology_id>
                        <value>ISO_639-1</value>
                    </terminology_id>
                    <code_string>en</code_string>
                </language>
                <encoding>
                    <terminology_id>
                        <value>IANA_character-sets</value>
                    </terminology_id>
                    <code_string>UTF-8</code_string>
                </encoding>
                <subject xsi:type="PARTY_SELF"/>
                <data archetype_node_id="at0001">
                    <name>
                        <value>Event Series</value>
                    </name>
                    <origin>
                        <value>2015-12-02T17:41:56.809Z</value>
                    </origin>
                    <events xsi:type="POINT_EVENT" archetype_node_id="at0002">
                        <name>
                            <value>Any event</value>
                        </name>
                        <time>
                            <value>2015-12-02T17:41:56.809Z</value>
                        </time>
                        <data xsi:type="ITEM_TREE" archetype_node_id="at0003">
                            <name>
                                <value>Tree</value>
                            </name>
                            <items xsi:type="CLUSTER" archetype_node_id="at0004">
                                <name>
                                    <value>Heading1</value>
                                </name>
                                <items xsi:type="ELEMENT" archetype_node_id="at0005">
                                    <name>
                                        <value>Free text or coded</value>
                                    </name>
                                    <value xsi:type="DV_TEXT">
                                        <value>Free text or coded 33</value>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0006">
                                    <name>
                                        <value>Text that uses Internal codes</value>
                                    </name>
                                    <value xsi:type="DV_CODED_TEXT">
                                        <value>Reclining</value>
                                        <defining_code>
                                            <terminology_id>
                                                <value>local</value>
                                            </terminology_id>
                                            <code_string>at0008</code_string>
                                        </defining_code>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0011">
                                    <name>
                                        <value>Text that is sourced from an external terminology</value>
                                    </name>
                                    <value xsi:type="DV_CODED_TEXT">
                                        <value>T.38 description</value>
                                        <defining_code>
                                            <terminology_id>
                                                <value>external_terminology</value>
                                            </terminology_id>
                                            <code_string>T.38</code_string>
                                        </defining_code>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0012">
                                    <name>
                                        <value>Quantity</value>
                                    </name>
                                    <value xsi:type="DV_QUANTITY">
                                        <magnitude>96.63</magnitude>
                                        <units>cm</units>
                                        <precision>1</precision>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0023">
                                    <name>
                                        <value>Interval of Quantity</value>
                                    </name>
                                    <value xsi:type="DV_INTERVAL">
                                        <lower xsi:type="DV_QUANTITY">
                                            <magnitude>5.66</magnitude>
                                            <units>cm</units>
                                        </lower>
                                        <upper xsi:type="DV_QUANTITY">
                                            <magnitude>62.91</magnitude>
                                            <units>cm</units>
                                        </upper>
                                        <lower_unbounded>false</lower_unbounded>
                                        <upper_unbounded>false</upper_unbounded>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0013">
                                    <name>
                                        <value>Count</value>
                                    </name>
                                    <value xsi:type="DV_COUNT">
                                        <magnitude>908</magnitude>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0022">
                                    <name>
                                        <value>Interval of Integer</value>
                                    </name>
                                    <value xsi:type="DV_INTERVAL">
                                        <lower xsi:type="DV_COUNT">
                                            <magnitude>10</magnitude>
                                        </lower>
                                        <upper xsi:type="DV_COUNT">
                                            <magnitude>4</magnitude>
                                        </upper>
                                        <lower_unbounded>false</lower_unbounded>
                                        <upper_unbounded>false</upper_unbounded>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0028">
                                    <name>
                                        <value>Proportion</value>
                                    </name>
                                    <value xsi:type="DV_PROPORTION">
                                        <numerator>58.0</numerator>
                                        <denominator>100.0</denominator>
                                        <type>2</type>
                                        <precision>0</precision>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0014">
                                    <name>
                                        <value>Date/Time</value>
                                    </name>
                                    <value xsi:type="DV_DATE_TIME">
                                        <value>2015-12-02T17:41:56.816Z</value>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0024">
                                    <name>
                                        <value>Interval of Date</value>
                                    </name>
                                    <value xsi:type="DV_INTERVAL">
                                        <lower xsi:type="DV_DATE_TIME">
                                            <value>2015-12-02T17:41:56.817Z</value>
                                        </lower>
                                        <upper xsi:type="DV_DATE_TIME">
                                            <value>2015-12-02T17:41:56.817Z</value>
                                        </upper>
                                        <lower_unbounded>false</lower_unbounded>
                                        <upper_unbounded>false</upper_unbounded>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0021">
                                    <name>
                                        <value>Duration</value>
                                    </name>
                                    <value xsi:type="DV_DURATION">
                                        <value>P1DT11H11M</value>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0015">
                                    <name>
                                        <value>Ordinal</value>
                                    </name>
                                    <value xsi:type="DV_ORDINAL">
                                        <value>1</value>
                                        <symbol>
                                            <value>Slight pain</value>
                                            <defining_code>
                                                <terminology_id>
                                                    <value>local</value>
                                                </terminology_id>
                                                <code_string>at0039</code_string>
                                            </defining_code>
                                        </symbol>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0026">
                                    <name>
                                        <value>Multimedia</value>
                                    </name>
                                    <value xsi:type="DV_MULTIMEDIA">
                                        <alternate_text>alternate text</alternate_text>
                                        <uri>
                                            <value>http://med.tube.com/sample</value>
                                        </uri>
                                        <media_type>
                                            <terminology_id>
                                                <value>IANA_media-types</value>
                                            </terminology_id>
                                            <code_string>text/xml</code_string>
                                        </media_type>
                                        <size>504903212</size>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0027">
                                    <name>
                                        <value>URI - resource identifier</value>
                                    </name>
                                    <value xsi:type="DV_URI">
                                        <value>http://example.com/path/resource</value>
                                    </value>
                                </items>
                                <items xsi:type="ELEMENT" archetype_node_id="at0044">
                                    <name>
                                        <value>Identifier</value>
                                    </name>
                                    <value xsi:type="DV_IDENTIFIER">
                                        <issuer>Issuer</issuer>
                                        <assigner>Assigner</assigner>
                                        <id>0ffc464a-d954-46f0-9f77-05ed6877ccf5</id>
                                        <type>Prescription</type>
                                    </value>
                                </items>
                            </items>
                        </data>
                    </events>
                </data>
            </items>
        </content>
  </data>
  <lifecycle_state>
    <value>completed</value>
    <defining_code>
      <terminology_id>
        <value>openehr</value>
      </terminology_id>
      <code_string>532</code_string>
    </defining_code>
  </lifecycle_state>
</version>
</versions>/$
      
      println "========= COMMIT ========="
      
      params.ehrUid = Ehr.get(1).uid
      params.auditSystemId = "TEST_SYSTEM_ID"
      params.auditCommitter = "Mr. Committer"
      controller.commit()
      
      println "========= FIN COMMIT ========="
      
      println controller.response.contentAsString
      
      def resp = new XmlSlurper().parseText( controller.response.contentAsString )
      
      assert resp.type.code.text() == "AA" // Application Reject
      
      println resp.message.text()
   }
}
