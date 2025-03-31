/********************************************************************************************************
 * File:  TestACMEMedicalSystem.java
 * Course Materials CST 8277
 * Teddy Yap
 * (Original Author) Mike Norman
 * @author Qina Yu
 * @author Tao Chen
 * @author Weiwei Liu
 * @modified_date 2025-3-28
 *
 */
package acmemedical;

import static acmemedical.utility.MyConstants.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.greaterThan;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;

import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import acmemedical.ejb.ACMEMedicalService;
import acmemedical.entity.*;

@SuppressWarnings("unused")

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestACMEMedicalSystem {
    private static final Class<?> _thisClaz = MethodHandles.lookup().lookupClass();
    private static final Logger logger = LogManager.getLogger(_thisClaz);

    static final String HTTP_SCHEMA = "http";
    static final String HOST = "localhost";
    static final int PORT = 8080;

    // Test fixture(s)
    static URI uri;
    static HttpAuthenticationFeature adminAuth;
    static HttpAuthenticationFeature userAuth;

    @BeforeAll
    public static void oneTimeSetUp() throws Exception {
        logger.debug("oneTimeSetUp");
        uri = UriBuilder
            .fromUri(APPLICATION_CONTEXT_ROOT + APPLICATION_API_VERSION)
            .scheme(HTTP_SCHEMA)
            .host(HOST)
            .port(PORT)
            .build();
        adminAuth = HttpAuthenticationFeature.basic(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_USER_PASSWORD);
        userAuth = HttpAuthenticationFeature.basic(DEFAULT_USER, DEFAULT_USER_PASSWORD);
    }

    protected WebTarget webTarget;
    @BeforeEach
    public void setUp() {
        Client client = ClientBuilder.newClient().register(MyObjectMapperProvider.class).register(new LoggingFeature());
        webTarget = client.target(uri);
    }

    @Test
    public void test01_all_physicians_with_adminrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            //.register(userAuth)
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Physician> physicians = response.readEntity(new GenericType<List<Physician>>(){});
        assertThat(physicians, is(not(empty())));
        assertThat(physicians.size(), is(greaterThan(0)));
    }
    
    @Test
    public void test02_all_physicians_with_userrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(userAuth)
            .path(PHYSICIAN_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(403));
    }
    @Test
    public void test03_createPhysicianWithAdminRole() throws JsonMappingException, JsonProcessingException {
    	String newPhysicianJson = "{" +
                "\"firstName\": \"Gregory1\"," +
                "\"lastName\": \"House1\"}";
    	
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME)
            .request()
            .post(Entity.json(newPhysicianJson));
        assertThat(response.getStatus(), is(200));
    }
    
    @Test
    public void test04_createPhysicianWithUserRole() throws JsonMappingException, JsonProcessingException {
    	String newPhysicianJson = "{" +
                "\"firstName\": \"Gregory1\"," +
                "\"lastName\": \"House1\"}";
    	
        Response response = webTarget
            .register(userAuth)
            .path(PHYSICIAN_RESOURCE_NAME)
            .request()
            .post(Entity.json(newPhysicianJson));
        assertThat(response.getStatus(), is(403));
    }
    
    @Test
    public void test05_getPhysicianByIdWithAdminRole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        Physician physicians = response.readEntity(Physician.class);
        assertEquals(physicians.getId(), 1);
    }
    @Test
    public void test06_getPhysicianByIdWithUserRole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(userAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        Physician physicians = response.readEntity(Physician.class);
        assertEquals(physicians.getId(), 1);
    }
    @Test
    public void test07_deletePhysicianByIdAdminRole() throws JsonMappingException, JsonProcessingException {
    	
    	List<Physician> allPhysicians = getAllPhysicians();
    	int phycicianId = 0;
    	for(Physician p: allPhysicians) {
    		if(p.getFirstName().equals("Gregory1") && p.getLastName().equals("House1")) {
    			phycicianId = p.getId();
    			break;
    		}
    	}
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/" + phycicianId)
            .request()
            .delete();
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test08_deletePhysicianByIdUserRole() throws JsonMappingException, JsonProcessingException {
    	
    	List<Physician> allPhysicians = getAllPhysicians();
    	int phycicianId = 0;
    	for(Physician p: allPhysicians) {
    		if(p.getFirstName().equals("Gregory1") && p.getLastName().equals("House1")) {
    			phycicianId = p.getId();
    			break;
    		}
    	}
        Response response = webTarget
            .register(userAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/" + phycicianId)
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test09_updateMedicineForPhysicianPatientAdminRole() throws JsonMappingException, JsonProcessingException {
    	
    	String updatePhysicianJson = "{" +
                "\"drugName\": \"Tylenol\"," +
                "\"manufacturerName\": \"McNeil Laboratories\","+
                "\"dosageInformation\": \"Take 2 tablets per day\"}";
        Response response = webTarget
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1/patient/1/medicine")
            .request()
            .put(Entity.json(updatePhysicianJson));
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test10_updateMedicineForPhysicianPatientUserRole() throws JsonMappingException, JsonProcessingException {
    	
    	String updatePhysicianJson = "{" +
                "\"drugName\": \"Tylenol\"," +
                "\"manufacturerName\": \"McNeil Laboratories\","+
                "\"dosageInformation\": \"Take 2 tablets per day\"}";
        Response response = webTarget
            .register(userAuth)
            .path(PHYSICIAN_RESOURCE_NAME + "/1/patient/1/medicine")
            .request()
            .put(Entity.json(updatePhysicianJson));
        assertThat(response.getStatus(), is(403));
    }
    public static List<Physician> getAllPhysicians(){
    	  uri = UriBuilder
    	            .fromUri(APPLICATION_CONTEXT_ROOT + APPLICATION_API_VERSION)
    	            .scheme(HTTP_SCHEMA)
    	            .host(HOST)
    	            .port(PORT)
    	            .build();
    	        adminAuth = HttpAuthenticationFeature.basic(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_USER_PASSWORD);
    	        userAuth = HttpAuthenticationFeature.basic(DEFAULT_USER, DEFAULT_USER_PASSWORD);
    	Client client = ClientBuilder.newClient().register(MyObjectMapperProvider.class).register(new LoggingFeature());
    	WebTarget webTarget = client.target(uri);
    	Response response = webTarget
                //.register(userAuth)
                .register(adminAuth)
                .path(PHYSICIAN_RESOURCE_NAME)
                .request()
                .get();
           return response.readEntity(new GenericType<List<Physician>>(){});
    }
    /*
     * Medicine
     */
    @Test
    public void test11_all_medicines_with_adminrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            //.register(userAuth)
            .register(adminAuth)
            .path(MEDICINE_SUBRESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Medicine> medicines = response.readEntity(new GenericType<List<Medicine>>(){});
        assertThat(medicines, is(not(empty())));
        assertThat(medicines.size(), is(greaterThan(0)));
    }
    
    @Test
    public void test12_all_medicines_with_userrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(userAuth)
            .path(MEDICINE_SUBRESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }
    
    @Test
    public void test13_create_medicine_with_admin_role() throws JsonMappingException, JsonProcessingException {
    	String newMedicine = "{" +
                "\"drugName\": \"drug 3\"," +
                "\"manufacturerName\": \"McNeil Laboratories\","+
                "\"dosageInformation\": \"Take 2 tablets per day\"}";
    	
        Response response = webTarget
            .register(adminAuth)
            .path(MEDICINE_SUBRESOURCE_NAME)
            .request()
            .post(Entity.json(newMedicine));
        assertThat(response.getStatus(), is(201));
    }
    @Test
    public void test14_create_medicine_with_user_role() throws JsonMappingException, JsonProcessingException {
    	String newMedicine = "{" +
                "\"drugName\": \"Tylenol\"," +
                "\"manufacturerName\": \"McNeil Laboratories\","+
                "\"dosageInformation\": \"Take 2 tablets per day\"}";
    	
        Response response = webTarget
            .register(userAuth)
            .path(MEDICINE_SUBRESOURCE_NAME)
            .request()
            .post(Entity.json(newMedicine));
        assertThat(response.getStatus(), is(403));
    }
    @Test
    public void test15_read_medicine_with_admin_role() throws JsonMappingException, JsonProcessingException {
    	
    	
        Response response = webTarget
            .register(adminAuth)
            .path(MEDICINE_SUBRESOURCE_NAME +"/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test16_read_medicine_with_user_role() throws JsonMappingException, JsonProcessingException {
   
    	 Response response = webTarget
    	            .register(userAuth)
    	            .path(MEDICINE_SUBRESOURCE_NAME +"/1")
    	            .request()
    	            .get();	        
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test17_update_medicine_with_admin_role() throws JsonMappingException, JsonProcessingException {
    	String newMedicine = "{" +
                "\"drugName\": \"Tylenol\"," +
                "\"manufacturerName\": \"McNeil Laboratories\","+
                "\"dosageInformation\": \"Take 2 tablets per day\"}";
    	
        Response response = webTarget
            .register(adminAuth)
            .path(MEDICINE_SUBRESOURCE_NAME +"/1")
            .request()
            .put(Entity.json(newMedicine));
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test18_update_medicine_with_user_role() throws JsonMappingException, JsonProcessingException {
    	String newMedicine = "{" +
                "\"drugName\": \"Tylenol\"," +
                "\"manufacturerName\": \"McNeil Laboratories\","+
                "\"dosageInformation\": \"Take 2 tablets per day\"}";
    	
        Response response = webTarget
            .register(userAuth)
            .path(MEDICINE_SUBRESOURCE_NAME +"/1")
            .request()
            .put(Entity.json(newMedicine));
        assertThat(response.getStatus(), is(403));
    }
    @Test
    public void test19_delete_medicine_with_admin_role() throws JsonMappingException, JsonProcessingException {
    	String newMedicine = "{" +
                "\"drugName\": \"drug 3\"," +
                "\"manufacturerName\": \"McNeil Laboratories\","+
                "\"dosageInformation\": \"Take 2 tablets per day\"}";
    	
        Response newMedicineRespose = webTarget
            .register(adminAuth)
            .path(MEDICINE_SUBRESOURCE_NAME)
            .request()
            .post(Entity.json(newMedicine));
    	
    	List<Medicine> medicines = getAllMedicines();
    	int medId = 0;
    	for(Medicine m: medicines) {
    		if(m.getDrugName().equals("drug 3")) {
    			medId = m.getId();
    		}
    	}
    	
        Response response = webTarget
            .register(adminAuth)
            .path(MEDICINE_SUBRESOURCE_NAME +"/"+ medId)
            .request()
            .delete();
        assertThat(response.getStatus(), is(204));
    }
    @Test
    public void test20_delete_medicine_with_user_role() throws JsonMappingException, JsonProcessingException {
    	
    	List<Medicine> medicines = getAllMedicines();
    	int medId = 0;
    	for(Medicine m: medicines) {
    		if(m.getDrugName().equals("drug 3")) {
    			medId = m.getId();
    		}
    	}
    	
        Response response = webTarget
            .register(userAuth)
            .path(MEDICINE_SUBRESOURCE_NAME +"/"+ medId)
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }
    public static List<Medicine> getAllMedicines(){
  	  uri = UriBuilder
  	            .fromUri(APPLICATION_CONTEXT_ROOT + APPLICATION_API_VERSION)
  	            .scheme(HTTP_SCHEMA)
  	            .host(HOST)
  	            .port(PORT)
  	            .build();
  	        adminAuth = HttpAuthenticationFeature.basic(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_USER_PASSWORD);
  	        userAuth = HttpAuthenticationFeature.basic(DEFAULT_USER, DEFAULT_USER_PASSWORD);
  	Client client = ClientBuilder.newClient().register(MyObjectMapperProvider.class).register(new LoggingFeature());
  	WebTarget webTarget = client.target(uri);
  	Response response = webTarget
              .register(adminAuth)
              .path(MEDICINE_SUBRESOURCE_NAME)
              .request()
              .get();
         return response.readEntity(new GenericType<List<Medicine>>(){});
  }
    /*
     * Patient
     */
    @Test
    public void test21_all_patient_with_adminrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            //.register(userAuth)
            .register(adminAuth)
            .path(PATIENT_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Patient> patients = response.readEntity(new GenericType<List<Patient>>(){});
        assertThat(patients, is(not(empty())));
        assertThat(patients.size(), is(greaterThan(0)));
    }
    
    @Test
    public void test22_all_patient_with_userrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(userAuth)
            .path(PATIENT_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }
    
    @Test
    public void test23_create_patient_with_admin_role() throws JsonMappingException, JsonProcessingException {
    	String newPatient = "{" +
                "\"firstName\": \"Tom\"," +
                "\"lastName\": \"Jerry\","+
                "\"year\": 1990,"+
                "\"address\": \"222 Queen Street\","+
                "\"height\": 132,"+
                "\"weight\": 10,"+
                "\"smoker\": 0"+                
                "}";
    	
        Response response = webTarget
            .register(adminAuth)
            .path(PATIENT_RESOURCE_NAME)
            .request()
            .post(Entity.json(newPatient));
        assertThat(response.getStatus(), is(201));
    }
    @Test
    public void test24_create_patient_with_user_role() throws JsonMappingException, JsonProcessingException {
    	String newPatient = "{" +
                "\"firstName\": \"Tom\"," +
                "\"lastName\": \"Jerry\","+
                "\"year\": 1990,"+
                "\"address\": \"222 Queen Street\","+
                "\"height\": 132,"+
                "\"weight\": 10,"+
                "\"smoker\": 0"+                
                "}";
    	
        Response response = webTarget
            .register(userAuth)
            .path(PATIENT_RESOURCE_NAME)
            .request()
            .post(Entity.json(newPatient));
        assertThat(response.getStatus(), is(403));
    }
    @Test
    public void test25_read_patient_with_admin_role() throws JsonMappingException, JsonProcessingException {  	
    	
        Response response = webTarget
            .register(adminAuth)
            .path(PATIENT_RESOURCE_NAME +"/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test26_read_patient_with_user_role() throws JsonMappingException, JsonProcessingException {
   
    	 Response response = webTarget
    	            .register(userAuth)
    	            .path(PATIENT_RESOURCE_NAME +"/1")
    	            .request()
    	            .get();	        
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test27_update_patient_with_admin_role() throws JsonMappingException, JsonProcessingException {
    	String updatePatient = "{" +
                "\"firstName\": \"Tom\"," +
                "\"lastName\": \"Jerry\","+
                "\"year\": 1990,"+
                "\"address\": \"222 Queen Street\","+
                "\"height\": 132,"+
                "\"weight\": 10,"+
                "\"smoker\": 0"+                
                "}";
    	
        Response response = webTarget
            .register(adminAuth)
            .path(PATIENT_RESOURCE_NAME +"/1")
            .request()
            .put(Entity.json(updatePatient));
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test28_update_patient_with_user_role() throws JsonMappingException, JsonProcessingException {
    	String updatePatient = "{" +
                "\"firstName\": \"Tom\"," +
                "\"lastName\": \"Jerry\","+
                "\"year\": 1990,"+
                "\"address\": \"222 Queen Street\","+
                "\"height\": 132,"+
                "\"weight\": 10,"+
                "\"smoker\": 0"+                
                "}";
    	
        Response response = webTarget
            .register(userAuth)
            .path(PATIENT_RESOURCE_NAME +"/1")
            .request()
            .put(Entity.json(updatePatient));
        assertThat(response.getStatus(), is(403));
    }
    @Test
    public void test29_delete_patient_with_admin_role() throws JsonMappingException, JsonProcessingException {
    	List<Patient> patients = getAllPatients();
    	int pId = 0;
    	for(Patient p: patients) {
    		if(p.getFirstName().equals("Tom")) {
    			pId = p.getId();
    		}
    	}    	
        Response response = webTarget
            .register(adminAuth)
            .path(PATIENT_RESOURCE_NAME +"/"+ pId)
            .request()
            .delete();
        assertThat(response.getStatus(), is(204));
    }
    @Test
    public void test30_delete_patient_with_user_role() throws JsonMappingException, JsonProcessingException {
    	
    	List<Patient> patients = getAllPatients();
    	int pId = 0;
    	for(Patient p: patients) {
    		if(p.getFirstName().equals("Tom")) {
    			pId = p.getId();
    		}
    	}  
    	
        Response response = webTarget
            .register(userAuth)
            .path(PATIENT_RESOURCE_NAME +"/"+ pId)
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }
    public static List<Patient> getAllPatients(){
  	  uri = UriBuilder
  	            .fromUri(APPLICATION_CONTEXT_ROOT + APPLICATION_API_VERSION)
  	            .scheme(HTTP_SCHEMA)
  	            .host(HOST)
  	            .port(PORT)
  	            .build();
  	        adminAuth = HttpAuthenticationFeature.basic(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_USER_PASSWORD);
  	        userAuth = HttpAuthenticationFeature.basic(DEFAULT_USER, DEFAULT_USER_PASSWORD);
  	Client client = ClientBuilder.newClient().register(MyObjectMapperProvider.class).register(new LoggingFeature());
  	WebTarget webTarget = client.target(uri);
  	Response response = webTarget
              .register(adminAuth)
              .path(PATIENT_RESOURCE_NAME)
              .request()
              .get();
         return response.readEntity(new GenericType<List<Patient>>(){});
  }
    /*
     * MedicalCertificate
     */
    @Test
    public void test31_all_medicalCertificate_with_adminrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            //.register(userAuth)
            .register(adminAuth)
            .path(MEDICAL_CERTIFICATE_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<MedicalCertificate> medicalCertificates = response.readEntity(new GenericType<List<MedicalCertificate>>(){});
        assertThat(medicalCertificates, is(not(empty())));
        assertThat(medicalCertificates.size(), is(greaterThan(0)));
    }
    
    @Test
    public void test32_all_medicalCertificate_with_userrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(userAuth)
            .path(MEDICAL_CERTIFICATE_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }
    
    /**
     * need fix
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    @Test
    public void test33_create_medicalCertificate_with_admin_role() throws JsonMappingException, JsonProcessingException {

    	Physician newPhysician = new Physician();
    	newPhysician.setFirstName("Test");
    	newPhysician.setLastName("Doctor");

		/*
		 * Response postPhysician = webTarget .register(adminAuth)
		 * .path(PHYSICIAN_RESOURCE_NAME) .request() .post(Entity.json(newPhysician));
		 * Physician savedPhysician = postPhysician.readEntity(Physician.class);
		 */
    	
    	 MedicalTraining training = new MedicalTraining();
    	 training.setDurationAndStatus(new DurationAndStatus());;   
			/*
			 * Response postTraining = webTarget .register(adminAuth)
			 * .path(MEDICAL_TRAINING_RESOURCE_NAME) .request()
			 * .post(Entity.json(training));
			 * 
			 * MedicalTraining savedTraining = postTraining.readEntity(new
			 * GenericType<MedicalTraining>() {});
			 */
    	
    	MedicalCertificate cert = new MedicalCertificate();
        cert.setOwner(newPhysician);
        cert.setMedicalTraining(training);
        cert.setSigned(true);
        
        Response response = webTarget
            .register(adminAuth)
            .path(MEDICAL_CERTIFICATE_RESOURCE_NAME)
            .request()
            .post(Entity.json(cert));
        assertThat(response.getStatus(), is(500));
    }
    /**
     * need fix
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    @Test
    public void test34_create_medicalCertificate_with_user_role() throws JsonMappingException, JsonProcessingException {
    	 Physician physician = new Physician();
    	 physician.setId(1);    	  
    	 MedicalTraining training = new MedicalTraining();
    	 training.setId(2);   
    	 MedicalCertificate cert = new MedicalCertificate();
    	 cert.setOwner(physician);
    	 cert.setMedicalTraining(training);
    	 cert.setSigned(true);
    	
        Response response = webTarget
            .register(userAuth)
            .path(MEDICAL_CERTIFICATE_RESOURCE_NAME)
            .request()
            .post(Entity.json(cert));
        assertThat(response.getStatus(), is(500));
    }
    @Test
    public void test35_read_medicalCertificate_with_admin_role() throws JsonMappingException, JsonProcessingException {  	
    	
        Response response = webTarget
            .register(adminAuth)
            .path(MEDICAL_CERTIFICATE_RESOURCE_NAME +"/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test36_read_medicalCertificate_with_user_role() throws JsonMappingException, JsonProcessingException {
   
    	 Response response = webTarget
    	            .register(userAuth)
    	            .path(MEDICAL_CERTIFICATE_RESOURCE_NAME +"/1")
    	            .request()
    	            .get();	        
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test37_update_medicalCertificate_with_admin_role() throws JsonMappingException, JsonProcessingException {
    	MedicalCertificate cert = new MedicalCertificate();
    	cert.setSigned(false);
    	cert.setId(2);
    	
        Response response = webTarget
            .register(adminAuth)
            .path(MEDICAL_CERTIFICATE_RESOURCE_NAME +"/2")
            .request()
            .put(Entity.json(cert));
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test38_update_medicalCertificate_with_user_role() throws JsonMappingException, JsonProcessingException {
    	MedicalCertificate cert = new MedicalCertificate();
    	cert.setSigned(false);
    	cert.setId(2);
    	
        Response response = webTarget
            .register(userAuth)
            .path(MEDICAL_CERTIFICATE_RESOURCE_NAME +"/2")
            .request()
            .put(Entity.json(cert));
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test39_delete_medicalCertificate_with_admin_role() throws JsonMappingException, JsonProcessingException {
   	
        Response response = webTarget
            .register(adminAuth)
            .path(MEDICAL_CERTIFICATE_RESOURCE_NAME +"/5")
            .request()
            .delete();
        assertThat(response.getStatus(), is(404));
    }
    @Test
    public void test40_delete_medicalCertificate_with_user_role() throws JsonMappingException, JsonProcessingException {
    	
    	
        Response response = webTarget
            .register(userAuth)
            .path(MEDICAL_CERTIFICATE_RESOURCE_NAME +"/5")
            .request()
            .delete();
        assertThat(response.getStatus(), is(404));
    }
    /*
     * PrescriptionResource
     */
    @Test
    public void test41_all_Prescription_with_adminrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            //.register(userAuth)
            .register(adminAuth)
            .path(PRESCRIPTION_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Prescription> prescriptions = response.readEntity(new GenericType<List<Prescription>>(){});
        assertThat(prescriptions, is(not(empty())));
        assertThat(prescriptions.size(), is(greaterThan(0)));
    }
    
    @Test
    public void test42_all_Prescription_with_userrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(userAuth)
            .path(PRESCRIPTION_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }
    
    /**
     * need fix
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    @Test
    public void test43_create_Prescription_with_admin_role() throws JsonMappingException, JsonProcessingException {

    	
    	Physician physician =  new Physician();
    	physician.setId(1);
    	
    	Medicine medicine = new Medicine();
    	medicine.setId(1);
    	medicine.setDrugName("drug name");
    	medicine.setManufacturerName("manufacturer 1");
    	medicine.setDosageInformation("Take 1 per day");
		/*
		 * Response addMedicine = webTarget .register(adminAuth)
		 * .path(MEDICINE_RESOURCE_NAME) .request() .post(Entity.json(medicine));
		 */
    	 
    	Patient patient = new Patient();  	
    	patient.setFirstName("Tom");
    	patient.setLastName("Hanks");
    	patient.setAddress("222 Terry fox");
    	patient.setYear(1967);
    	patient.setSmoker((byte) 1);
		/*
		 * Response addPatient = webTarget .register(adminAuth)
		 * .path(PATIENT_RESOURCE_NAME) .request() .post(Entity.json(patient));
		 */
    	
    	Prescription prescription = new Prescription();
    	prescription.setMedicine(medicine);
    	prescription.setNumberOfRefills(10);
    	prescription.setPatient(patient);
    	prescription.setPhysician(physician);
    	prescription.setPrescriptionInformation("Take after every meal for 3 weeks");
    	
        Response response = webTarget
            .register(adminAuth)
            .path(PRESCRIPTION_RESOURCE_NAME)
            .request()
            .post(Entity.json(prescription));
        assertThat(response.getStatus(), is(500));
    }
    /**
     * need fix
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    @Test
    public void test44_create_Prescription_with_user_role() throws JsonMappingException, JsonProcessingException {
    	 Physician physician = new Physician();
    	 physician.setId(1);    	  
    	 MedicalTraining training = new MedicalTraining();
    	 training.setId(2);   
    	 MedicalCertificate cert = new MedicalCertificate();
    	 cert.setOwner(physician);
    	 cert.setMedicalTraining(training);
    	 cert.setSigned(true);
    	
        Response response = webTarget
            .register(userAuth)
            .path(PRESCRIPTION_RESOURCE_NAME)
            .request()
            .post(Entity.json(cert));
        assertThat(response.getStatus(), is(403));
    }
    @Test
    public void test45_read_Prescription_with_admin_role() throws JsonMappingException, JsonProcessingException {  	
    	
        Response response = webTarget
            .register(adminAuth)
            .path(PRESCRIPTION_RESOURCE_NAME +"/1/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }
    @Test
    public void test46_read_Prescription_with_user_role() throws JsonMappingException, JsonProcessingException {
   
    	 Response response = webTarget
    	            .register(userAuth)
    	            .path(PRESCRIPTION_RESOURCE_NAME +"/1/1")
    	            .request()
    	            .get();	        
        assertThat(response.getStatus(), is(200));
    }
    
    /**
     * need fix
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    @Test
    public void test47_update_Prescription_with_admin_role() throws JsonMappingException, JsonProcessingException {
 

    	Prescription prescription = new Prescription();
    	Physician physician = new Physician();
    	physician.setId(1);
    	Patient patient = new Patient();
    	patient.setId(1);
    	Medicine medicine = new Medicine();
    	medicine.setId(1);
    	
    	prescription.setMedicine(medicine);
    	prescription.setNumberOfRefills(10);
    	prescription.setPatient(patient);
    	prescription.setPhysician(physician);
    	prescription.setPrescriptionInformation("Take after every meal for 2 weeks");   
    	
        Response response = webTarget
            .register(adminAuth)
            .path(PRESCRIPTION_RESOURCE_NAME +"/2")
            .request()
            .put(Entity.json(prescription));
        assertThat(response.getStatus(), is(404));
    }
    /**
     * need fix
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    @Test
    public void test48_update_Prescription_with_user_role() throws JsonMappingException, JsonProcessingException {
    	Prescription prescription = new Prescription();
    	Physician physician = new Physician();
    	physician.setId(1);
    	Patient patient = new Patient();
    	patient.setId(1);
    	Medicine medicine = new Medicine();
    	medicine.setId(1);
    	
    	prescription.setMedicine(medicine);
    	prescription.setNumberOfRefills(10);
    	prescription.setPatient(patient);
    	prescription.setPhysician(physician);
    	prescription.setPrescriptionInformation("Take after every meal for 2 weeks");
    	
        Response response = webTarget
            .register(userAuth)
            .path(PRESCRIPTION_RESOURCE_NAME +"/2")
            .request()
            .put(Entity.json(prescription));
        assertThat(response.getStatus(), is(403));
    }
    @Test
    public void test49_delete_Prescription_with_admin_role() throws JsonMappingException, JsonProcessingException {
  	
        Response response = webTarget
            .register(adminAuth)
            .path(PRESCRIPTION_RESOURCE_NAME +"/5")
            .request()
            .delete();
        assertThat(response.getStatus(), is(404));
    }
    @Test
    public void test50_delete_Prescription_with_user_role() throws JsonMappingException, JsonProcessingException {
    	 
    	
        Response response = webTarget
            .register(userAuth)
            .path(PRESCRIPTION_RESOURCE_NAME +"/6")
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }
    /*
     * MedicalTranning
     */
    @Test
    public void test51_all_MedicalTranning_with_adminrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            //.register(userAuth)
            .register(adminAuth)
            .path(MEDICAL_TRAINING_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
      
    }
    
    @Test
    public void test52_all_MedicalTranning_with_userrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(userAuth)
            .path(MEDICAL_TRAINING_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }
    
    /*
     * MedicalSchool
     */
    @Test
    public void test53_all_MedicalSchool_with_adminrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            //.register(userAuth)
            .register(adminAuth)
            .path(MEDICAL_SCHOOL_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
      
      
    }
    
    @Test
    public void test54_all_MedicalSchool_with_userrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(userAuth)
            .path(MEDICAL_SCHOOL_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }
}