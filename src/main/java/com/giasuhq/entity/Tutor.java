package com.giasuhq.entity;

public class Tutor {

    private Long id;
    private String fullName;
    private String subject;
    private String email;
    private double hourlyRate;

    public Tutor() {
    }

    public Tutor(Long id, String fullName, String subject, String email, double hourlyRate) {
        this.id = id;
        this.fullName = fullName;
        this.subject = subject;
        this.email = email;
        this.hourlyRate = hourlyRate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }
}
