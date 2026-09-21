package com.giasuhq.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "parents")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Parent extends User {

    private String address;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "student_grade_level")
    private String studentGradeLevel;

    @Column(name = "student_school_name")
    private String studentSchoolName;
}
