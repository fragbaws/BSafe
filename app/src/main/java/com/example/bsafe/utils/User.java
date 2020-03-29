package com.example.bsafe.utils;

import java.io.Serializable;

/**
 * Used to store information about a specific user that registered or logged in.
 */
public class User implements Serializable {

    public User(){}

    private String firstName;
    private String surname;
    private String dob;
    private String mobile;
    private String emergency;
    private String email;
    private String password;
    private String height;
    private String weight;
    private String bloodType;
    private String smoker;
    private String bibulous;
    private String medicalCondition;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getSmoker() {
        return smoker;
    }

    public void setSmoker(String smoker) {
        this.smoker = smoker;
    }

    public String getBibulous() {
        return bibulous;
    }

    public void setBibulous(String bibulous) {
        this.bibulous = bibulous;
    }

    public String getMedicalCondition() {
        return medicalCondition;
    }

    public void setMedicalCondition(String medicalCondition) { this.medicalCondition = medicalCondition; }

    public String getEmergency() { return emergency; }

    public void setEmergency(String emergency) { this.emergency = emergency; }

    public String getMobile() { return mobile; }

    public void setMobile(String mobile) { this.mobile = mobile; }



    @Override
    public String toString() {
        return "User{" +
                "firstName='" + firstName + '\'' +
                ", surname='" + surname + '\'' +
                ", dob='" + dob + '\'' +
                ", mobile='" + mobile + '\'' +
                ", emergency='" + emergency + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", height='" + height + '\'' +
                ", weight='" + weight + '\'' +
                ", bloodType='" + bloodType + '\'' +
                ", smoker='" + smoker + '\'' +
                ", bibulous='" + bibulous + '\'' +
                ", medicalCondition='" + medicalCondition + '\'' +
                '}';
    }
}
