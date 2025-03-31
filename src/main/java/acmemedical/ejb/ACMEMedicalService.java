/********************************************************************************************************
 * File:  ACMEMedicalService.java Course Materials CST 8277
 *
 * @author Teddy Yap
 * @author Shariar (Shawn) Emami
 * @author Qina Yu
 * @author Tao Chen
 * @author Weiwei Liu
 * @modified_date 2025-3-28
 */
package acmemedical.ejb;

import static acmemedical.utility.MyConstants.DEFAULT_KEY_SIZE;
import static acmemedical.utility.MyConstants.DEFAULT_PROPERTY_ALGORITHM;
import static acmemedical.utility.MyConstants.DEFAULT_PROPERTY_ITERATIONS;
import static acmemedical.utility.MyConstants.DEFAULT_SALT_SIZE;
import static acmemedical.utility.MyConstants.DEFAULT_USER_PASSWORD;
import static acmemedical.utility.MyConstants.DEFAULT_USER_PREFIX;
import static acmemedical.utility.MyConstants.PARAM1;
import static acmemedical.utility.MyConstants.PROPERTY_ALGORITHM;
import static acmemedical.utility.MyConstants.PROPERTY_ITERATIONS;
import static acmemedical.utility.MyConstants.PROPERTY_KEY_SIZE;
import static acmemedical.utility.MyConstants.PROPERTY_SALT_SIZE;
import static acmemedical.utility.MyConstants.PU_NAME;
import static acmemedical.utility.MyConstants.USER_ROLE;
import static acmemedical.entity.Physician.ALL_PHYSICIANS_QUERY_NAME;
import static acmemedical.entity.MedicalSchool.ALL_MEDICAL_SCHOOLS_QUERY_NAME;
import static acmemedical.entity.MedicalSchool.IS_DUPLICATE_QUERY_NAME;
import static acmemedical.entity.MedicalSchool.SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import jakarta.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import acmemedical.entity.MedicalTraining;
import acmemedical.entity.Patient;
import acmemedical.entity.MedicalCertificate;
import acmemedical.entity.Medicine;
import acmemedical.entity.Prescription;
import acmemedical.entity.PrescriptionPK;
import acmemedical.entity.SecurityRole;
import acmemedical.entity.SecurityUser;
import acmemedical.entity.Physician;
import acmemedical.entity.MedicalSchool;

@SuppressWarnings("unused")

/**
 * Stateless Singleton EJB Bean - ACMEMedicalService
 */
@Singleton
public class ACMEMedicalService implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final Logger LOG = LogManager.getLogger();
    
    @PersistenceContext(name = PU_NAME)
    protected EntityManager em;
    
    @Inject
    protected Pbkdf2PasswordHash pbAndjPasswordHash;

    // These methods are more generic.

    public <T> List<T> getAll(Class<T> entity, String namedQuery) {
        TypedQuery<T> allQuery = em.createNamedQuery(namedQuery, entity);
        return allQuery.getResultList();
    }
    
    public <T> T getById(Class<T> entity, String namedQuery, int id) {
        TypedQuery<T> allQuery = em.createNamedQuery(namedQuery, entity);
        allQuery.setParameter(PARAM1, id);
        return allQuery.getSingleResult();
    }
    
	/* 
	 * 
	 *
	 * 
	 * physician 
	 * 
	 * 
	 * 
	 * 
	 * */
    //Get
    public List<Physician> getAllPhysicians() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Physician> cq = cb.createQuery(Physician.class);
        cq.select(cq.from(Physician.class));
        return em.createQuery(cq).getResultList();
    }

    public Physician getPhysicianById(int id) {
        return em.find(Physician.class, id);
    }
    //Add
    @Transactional
    public Physician persistPhysician(Physician newPhysician) {
        em.persist(newPhysician);
        return newPhysician;
    }

    @Transactional
    public void buildUserForNewPhysician(Physician newPhysician) {
        SecurityUser userForNewPhysician = new SecurityUser();
        userForNewPhysician.setUsername(
            DEFAULT_USER_PREFIX + "_" + newPhysician.getFirstName() + "." + newPhysician.getLastName());
        Map<String, String> pbAndjProperties = new HashMap<>();
        pbAndjProperties.put(PROPERTY_ALGORITHM, DEFAULT_PROPERTY_ALGORITHM);
        pbAndjProperties.put(PROPERTY_ITERATIONS, DEFAULT_PROPERTY_ITERATIONS);
        pbAndjProperties.put(PROPERTY_SALT_SIZE, DEFAULT_SALT_SIZE);
        pbAndjProperties.put(PROPERTY_KEY_SIZE, DEFAULT_KEY_SIZE);
        pbAndjPasswordHash.initialize(pbAndjProperties);
        String pwHash = pbAndjPasswordHash.generate(DEFAULT_USER_PASSWORD.toCharArray());
        userForNewPhysician.setPwHash(pwHash);
        userForNewPhysician.setPhysician(newPhysician);
        /* TODO ACMECS01 - Use NamedQuery on SecurityRole to find USER_ROLE */ 
        
//        TypedQuery<SecurityRole> query = em.createNamedQuery("SecurityRole.findByName", SecurityRole.class);
//		query.setParameter(PARAM1, USER_ROLE);
//		SecurityRole userRole = query.getSingleResult();
		
//        SecurityRole userRole = em.createNamedQuery("SecurityRole.findByName", SecurityRole.class)
//        	    .setParameter("param1", USER_ROLE)
//        	    .getSingleResult();
//        userForNewPhysician.getRoles().add(userRole);
//        userRole.getUsers().add(userForNewPhysician);
//        em.persist(userForNewPhysician);
        TypedQuery<SecurityRole> query = em.createNamedQuery(SecurityRole.ROLE_BY_NAME_QUERY, SecurityRole.class);
		query.setParameter(PARAM1, USER_ROLE);
		SecurityRole userRole = query.getSingleResult();
		userForNewPhysician.getRoles().add(userRole);
		userRole.getUsers().add(userForNewPhysician);
		em.persist(userForNewPhysician);
    }

    @Transactional
    public Medicine setMedicineForPhysicianPatient(int physicianId, int patientId, Medicine newMedicine) {
        Physician physicianToBeUpdated = em.find(Physician.class, physicianId);
        if (physicianToBeUpdated != null) { // Physician exists
            Set<Prescription> prescriptions = physicianToBeUpdated.getPrescriptions();
            prescriptions.forEach(p -> {
                if (p.getPatient().getId() == patientId) {
                    if (p.getMedicine() != null) { // Medicine exists
                        Medicine medicine = em.find(Medicine.class, p.getMedicine().getId());
                        medicine.setMedicine(newMedicine.getDrugName(),
                        				  newMedicine.getManufacturerName(),
                        				  newMedicine.getDosageInformation());
                        em.merge(medicine);
                    }
                    else { // Medicine does not exist
                        p.setMedicine(newMedicine);
                        em.merge(physicianToBeUpdated);
                    }
                }
            });
            return newMedicine;
        }
        else return null;  // Physician doesn't exists
    }

    /**
     * To update a physician
     * 
     * @param id - id of entity to update
     * @param physicianWithUpdates - entity with updated information
     * @return Entity with updated information
     */
    @Transactional
    public Physician updatePhysicianById(int id, Physician physicianWithUpdates) {
    	Physician physicianToBeUpdated = getPhysicianById(id);
        if (physicianToBeUpdated != null) {
            em.refresh(physicianToBeUpdated);
            em.merge(physicianWithUpdates);
            em.flush();
        }
        return physicianToBeUpdated;
    }

    /**
     * To delete a physician by id
     * 
     * @param id - physician id to delete
     */
    @Transactional
    public void deletePhysicianById(int id) {
        Physician physician = getPhysicianById(id);
        if (physician != null) {
            em.refresh(physician);
        
            /* TODO ACMECS02 - Use NamedQuery on SecurityRole to find this related Student
               so that when we remove it, the relationship from SECURITY_USER table
               is not dangling
            */ 
            
            TypedQuery<SecurityUser> findUser = em.createNamedQuery(SecurityUser.USER_FOR_OWNING_PHYSICIAN_QUERY, SecurityUser.class);
            findUser.setParameter(PARAM1, id);
      
            
            try {
                SecurityUser sUser = findUser.getSingleResult();
                em.remove(sUser);
            } catch (NoResultException e) {
                LOG.warn("No SecurityUser found for physician id {}", id);
            }
            em.remove(physician);
        }
    }
    
    
    
	/* 
	 * 
	 *
	 * 
	 * medical school 
	 * 
	 * 
	 * 
	 * 
	 * */
    
    //get
    public List<MedicalSchool> getAllMedicalSchools() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MedicalSchool> cq = cb.createQuery(MedicalSchool.class);
        cq.select(cq.from(MedicalSchool.class));
        return em.createQuery(cq).getResultList();
    }

    // Why not use the build-in em.find?  The named query SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME
    // includes JOIN FETCH that we cannot add to the above API
    public MedicalSchool getMedicalSchoolById(int id) {
        TypedQuery<MedicalSchool> specificMedicalSchoolQuery = em.createNamedQuery(SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME, MedicalSchool.class);
        specificMedicalSchoolQuery.setParameter(PARAM1, id);
        return specificMedicalSchoolQuery.getSingleResult();
    }
    


//    @Transactional
//    public MedicalSchool deleteMedicalSchool(int id) {
//        //MedicalSchool ms = getMedicalSchoolById(id);
//    	MedicalSchool ms = getById(MedicalSchool.class, MedicalSchool.SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME, id);
//        if (ms != null) {
//            Set<MedicalTraining> medicalTrainings = ms.getMedicalTrainings();
//            List<MedicalTraining> list = new LinkedList<>();
//            medicalTrainings.forEach(list::add);
//            list.forEach(mt -> {
//                if (mt.getCertificate() != null) {
//                    MedicalCertificate mc = getById(MedicalCertificate.class, MedicalCertificate.FIND_BY_ID, mt.getCertificate().getId());
//                    mc.setMedicalTraining(null);
//                }
//                mt.setCertificate(null);
//                em.merge(mt);
//            });
//            em.remove(ms);
//            return ms;
//        }
//        return null;
//    }
	
    //delete
    @Transactional
    public MedicalSchool deleteMedicalSchool(int id) {
        MedicalSchool ms = getById(MedicalSchool.class, MedicalSchool.SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME, id);
        if (ms != null) {
            Set<MedicalTraining> medicalTrainings = ms.getMedicalTrainings();
            List<MedicalTraining> list = new LinkedList<>();
            medicalTrainings.forEach(list::add);

            list.forEach(mt -> {
                try {
                	//Find related MedicalCertificate and delete it
                    MedicalCertificate mc = getById(MedicalCertificate.class, MedicalCertificate.FIND_BY_ID, mt.getCertificate().getId());
                    if (mc != null) {
                        mc.setMedicalTraining(null);
                        em.merge(mc);
                    }
                } catch (Exception e) {
                    LOG.warn("No MedicalCertificate found for trainingId: " + mt.getId());
                }
                mt.setCertificate(null);
                em.merge(mt);
            });

            em.remove(ms);
        }
        return ms;
    }
    public boolean isDuplicated(MedicalSchool newMedicalSchool) {
        TypedQuery<Long> allMedicalSchoolsQuery = em.createNamedQuery(IS_DUPLICATE_QUERY_NAME, Long.class);
        allMedicalSchoolsQuery.setParameter(PARAM1, newMedicalSchool.getName());
        return (allMedicalSchoolsQuery.getSingleResult() >= 1);
    }

    //add
    @Transactional
    public MedicalSchool persistMedicalSchool(MedicalSchool newMedicalSchool) {
        em.persist(newMedicalSchool);
        return newMedicalSchool;
    }

    //update
    @Transactional
    public MedicalSchool updateMedicalSchool(int id, MedicalSchool updatingMedicalSchool) {
    	MedicalSchool medicalSchoolToBeUpdated = getMedicalSchoolById(id);
        if (medicalSchoolToBeUpdated != null) {
            em.refresh(medicalSchoolToBeUpdated);
            medicalSchoolToBeUpdated.setName(updatingMedicalSchool.getName());
            em.merge(medicalSchoolToBeUpdated);
            em.flush();
        }
        return medicalSchoolToBeUpdated;
    }
    
    // Please study & use the methods below in your test suites
  


	/* 
	 * 
	 * 
	 * 
	 * 
	 * medical training 
	 * 
	 * 
	 * 
	 * 
	 * */
 //add
    @Transactional
    public MedicalTraining persistMedicalTraining(MedicalTraining newMedicalTraining) {
        em.persist(newMedicalTraining);
        return newMedicalTraining;
    }
    
    //get
    public MedicalTraining getMedicalTrainingById(int mtId) {
        TypedQuery<MedicalTraining> allMedicalTrainingQuery = em.createNamedQuery(MedicalTraining.FIND_BY_ID, MedicalTraining.class);
        allMedicalTrainingQuery.setParameter(PARAM1, mtId);
        return allMedicalTrainingQuery.getSingleResult();
    }

    //update
    @Transactional
    public MedicalTraining updateMedicalTraining(int id, MedicalTraining medicalTrainingWithUpdates) {
    	MedicalTraining medicalTrainingToBeUpdated = getMedicalTrainingById(id);
        if (medicalTrainingToBeUpdated != null) {
            em.refresh(medicalTrainingToBeUpdated);
            em.merge(medicalTrainingWithUpdates);
            em.flush();
        }
        return medicalTrainingToBeUpdated;
    }
    
    public Prescription getPrescriptionById(PrescriptionPK id) {
		return em.find(Prescription.class, id);
	}
    public List<MedicalTraining> getAllMedicalTraining() {
		return this.getAll(MedicalTraining.class, MedicalTraining.FIND_ALL);
	}
    
    //delete
    @Transactional
	public MedicalTraining deleteMedicalTraining(int id) {
		MedicalTraining medicalTraining = em.find(MedicalTraining.class, id);
		if (medicalTraining != null) {
			// Remove associated MedicalCertificate if it exists
			if (medicalTraining.getCertificate() != null) {
				MedicalCertificate certificate = medicalTraining.getCertificate();
				certificate.setMedicalTraining(null);
				em.merge(certificate);
			}

			// Remove the MedicalTraining entity
			em.remove(medicalTraining);
		}
		return medicalTraining;
	}
    

	/* 
	 * 
	 * 
	 * 
	 * 
	 * medical certificate
	 * 
	 * 
	 * 
	 * 
	 * */
    //add
    @Transactional
	public MedicalCertificate persistMedicalCertificate(MedicalCertificate newMedicalCertificate) {
		em.persist(newMedicalCertificate);
		return newMedicalCertificate;
	}
//get
	public MedicalCertificate getMedicalCertificateById(int id) {
		return em.find(MedicalCertificate.class, id);
	}
	
	public List<MedicalCertificate> getAllMedicalCertificates() {
		try {
			// Create a CriteriaBuilder to build a query to retrieve all medical certificates
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<MedicalCertificate> cq = cb.createQuery(MedicalCertificate.class);
			Root<MedicalCertificate> root = cq.from(MedicalCertificate.class);

			// Select all MedicalCertificate records
			cq.select(root);

			// Execute the query and return the result list
			return em.createQuery(cq).getResultList();
		} catch (Exception e) {
			LOG.error("Error retrieving all medical certificates", e);
			return Collections.emptyList();
		}
	}
//update
	@Transactional
	public MedicalCertificate updateMedicalCertificate(int id, MedicalCertificate updatedMedicalCertificate) {
		MedicalCertificate certificateToBeUpdated = getMedicalCertificateById(id);
		if (certificateToBeUpdated != null) {
			em.refresh(certificateToBeUpdated);
			certificateToBeUpdated.setMedicalTraining(updatedMedicalCertificate.getMedicalTraining());
			certificateToBeUpdated.setSigned(updatedMedicalCertificate.getSigned());
			certificateToBeUpdated.setCreated(updatedMedicalCertificate.getCreated());
			certificateToBeUpdated.setUpdated(updatedMedicalCertificate.getUpdated());
			em.merge(certificateToBeUpdated);
			em.flush();
		}
		return certificateToBeUpdated;
	}
//delete
	@Transactional
	public void deleteMedicalCertificateById(int id) {
		MedicalCertificate certificate = getMedicalCertificateById(id);
		if (certificate != null) {
			em.remove(certificate);
		}
	}

	/* 
	 * 
	 * 
	 * 
	 * prescriptions 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * */
 // C:Method to Create prescriptions
 	@Transactional
	public Prescription persistPrescription(Prescription newPrescription) {
		em.persist(newPrescription);
		return newPrescription;
	}
 // D:Method to delete all prescriptions 
    @Transactional
	public Prescription deletePrescriptionById(PrescriptionPK id) {
		Prescription prescription = getPrescriptionById(id);
		if (prescription != null) {
			em.remove(prescription);
		}
		return prescription;
	}
    
 // R:Method to get all prescriptions
 	public List<Prescription> getAllPrescriptions() {
 		CriteriaBuilder cb = em.getCriteriaBuilder();
 		CriteriaQuery<Prescription> cq = cb.createQuery(Prescription.class);
 		Root<Prescription> prescriptionRoot = cq.from(Prescription.class);
 		cq.select(prescriptionRoot); // Select all records from Prescription entity
 		TypedQuery<Prescription> query = em.createQuery(cq);
 		return query.getResultList(); // Execute and return the result
 	}
 // U:Method to update all prescriptions
 	@Transactional
	public Prescription updatePrescription(PrescriptionPK id, Prescription updatedPrescription) {
		Prescription prescriptionToBeUpdated = getPrescriptionById(id);
		if (prescriptionToBeUpdated != null) {
			em.refresh(prescriptionToBeUpdated);
			prescriptionToBeUpdated.setMedicine(updatedPrescription.getMedicine());
			prescriptionToBeUpdated.setPatient(updatedPrescription.getPatient());
			prescriptionToBeUpdated.setPhysician(updatedPrescription.getPhysician());
			em.merge(prescriptionToBeUpdated);
			em.flush();
		}
		return prescriptionToBeUpdated;
	}
 	
	/* 
	 * 
	 * 
	 * 
	 * 
	 * medicine 
	 * 
	 * 
	 * 
	 * 
	 * */
 
 	 // C:Method to Create medicine
	@Transactional
	public Medicine persistMedicine(Medicine newMedicine) {
		em.persist(newMedicine);
		return newMedicine;
	}
	 // R:Method to get medicine
	public List<Medicine> getAllMedicines() {
		try {
			// Create the criteria builder and criteria query for Medicine class
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Medicine> cq = cb.createQuery(Medicine.class);
			Root<Medicine> root = cq.from(Medicine.class); // Define the root entity (Medicine)

			// Select all Medicine records
			cq.select(root);

			// Execute the query
			TypedQuery<Medicine> query = em.createQuery(cq);
			return query.getResultList(); // Return the list of Medicines
		} catch (Exception e) {
			// Log and handle exceptions (optional)
			LOG.error("Error retrieving all medicines", e);
			return Collections.emptyList(); // Return an empty list in case of failure
		}
	}
	public Medicine getMedicineById(int id) {
		return em.find(Medicine.class, id);
	}
	
	 // U:Method to update medicine
	@Transactional
	public Medicine updateMedicine(int id, Medicine updatedMedicine) {
		Medicine medicineToBeUpdated = getMedicineById(id);
		if (medicineToBeUpdated != null) {
			em.refresh(medicineToBeUpdated);
			medicineToBeUpdated.setDrugName(updatedMedicine.getDrugName());
			medicineToBeUpdated.setManufacturerName(updatedMedicine.getManufacturerName());
			medicineToBeUpdated.setDosageInformation(updatedMedicine.getDosageInformation());
			em.merge(medicineToBeUpdated);
			em.flush();
		}
		return medicineToBeUpdated;
	}
	// D:Method to delete medicine
	@Transactional
	public void deleteMedicineById(int id) {
		Medicine medicine = getMedicineById(id);
		if (medicine != null) {
			em.remove(medicine);
		}
	}
	
	/*
	 * 
	 *  
	 *  
	 *  
	 *  patient
	 *  
	 *   
	 *   
	 *   
	 *   
	 *   
	 *   */
	// C:Method to Create Patient
		@Transactional
		public Patient persistPatient(Patient newPatient) {
			em.persist(newPatient);
			return newPatient;
		}
		// Retrieve all Patients
		public List<Patient> getAllPatients() {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Patient> cq = cb.createQuery(Patient.class);
			cq.select(cq.from(Patient.class));
			return em.createQuery(cq).getResultList();
		}
		// Retrieve a specific Patient by ID
		public Patient getPatientById(int id) {
			return em.find(Patient.class, id);
		}
		
		 // U:Method to update Patient
		@Transactional
		public Patient updatePatient(int id, Patient updatedPatient) {
			Patient patientToBeUpdated = getPatientById(id);
			if (patientToBeUpdated != null) {
				em.refresh(patientToBeUpdated);
				patientToBeUpdated.setFirstName(updatedPatient.getFirstName());
 				patientToBeUpdated.setLastName(updatedPatient.getLastName());
 				patientToBeUpdated.setYear(updatedPatient.getYear());
 				patientToBeUpdated.setWeight(updatedPatient.getWeight());
 				patientToBeUpdated.setHeight(updatedPatient.getHeight());
 				patientToBeUpdated.setSmoker(updatedPatient.getSmoker());
				em.merge(patientToBeUpdated);
				em.flush();
			}
			return patientToBeUpdated;
		}
		// D:Method to delete medicine
		@Transactional
		public Patient deletePatientById(int id) {
			Patient patient = getPatientById(id);
			if (patient != null) {
				em.remove(patient);
			}
			return patient;
		}
		public void deletePatientPrescriptions(int patientId) {
			Query query = em.createQuery(
					"DELETE FROM Prescription p WHERE p.patient.id = :patientId");
			query.setParameter("patientId", patientId);
			query.executeUpdate();
		}
		
		
	
}