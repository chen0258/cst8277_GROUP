/********************************************************************************************************
 * File:  Patient.java Course Materials CST 8277
 *
 * @author Teddy Yap
 *
 * @author Qina yu
 * @author Tao Chen
 * @author Weiwei Liu
 * *
 * @modified_date 2025-03-28
 *
 */
package acmemedical.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@SuppressWarnings("unused")

/**
 * The persistent class for the patient database table.
 */
// TODOo PA01 - Add the missing annotations.
@Entity
@Table(name = "patient")
@AttributeOverrides({
		@AttributeOverride(name = "id", column = @Column(name = "patient_id"))
})
// TODOo PA02 - Do we need a mapped super class? If so, which one?
// Answer: Yes, we need a mapped super class. The superclass is PojoBase.
@NamedQuery(name = Patient.ALL_PATIENTS_QUERY_NAME, query = "SELECT p FROM Patient p")
public class Patient extends PojoBase implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String ALL_PATIENTS_QUERY_NAME = "Patient.findAll";

	// TODOo PA03 - Add missing annotations.
	@Column(name = "first_name")
	private String firstName;

	// TODOo PA04 - Add missing annotations.
	@Column(name = "last_name")
	private String lastName;

	// TODOo PA05 - Add missing annotations.
	@Column(name = "year_of_birth")
	private int year;

	// TODOo PA06 - Add missing annotations.
	@Column(name = "home_address")
	private String address;

	// TODOo PA07 - Add missing annotations.
	@Column(name = "height_cm")
	private int height;

	// TODOo PA08 - Add missing annotations.
	@Column(name = "weight_kg")
	private int weight;

	// TODOo PA09 - Add missing annotations.
	@Column(name = "smoker")
	private byte smoker;

	// TODOo PA10 - Add annotations for 1:M relation. What should be the cascade and fetch types?
	@OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference("patient-prescriptions")
	private Set<Prescription> prescriptions = new HashSet<>();

	public Patient() {
		super();
	}

	public Patient(String firstName, String lastName, int year, String address, int height, int weight, byte smoker) {
		this();
		this.firstName = firstName;
		this.lastName = lastName;
		this.year = year;
		this.address = address;
		this.height = height;
		this.weight = weight;
		this.smoker = smoker;
	}

	public Patient setPatient(String firstName, String lastName, int year, String address, int height, int weight, byte smoker) {
		setFirstName(firstName);
		setLastName(lastName);
		setYear(year);
		setAddress(address);
		setHeight(height);
		setWeight(weight);
		setSmoker(smoker);
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public byte getSmoker() {
		return smoker;
	}

	public void setSmoker(byte smoker) {
		this.smoker = smoker;
	}

	public Set<Prescription> getPrescriptions() {
		return prescriptions;
	}

	public void setPrescription(Set<Prescription> prescriptions) {
		this.prescriptions = prescriptions;
	}

	// Inherited hashCode/equals is sufficient for this Entity class
}