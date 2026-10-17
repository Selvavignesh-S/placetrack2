package com.placetrack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.placetrack.model.Application;

/** CRUD for applications plus the HQL queries for student, applicant and dashboard pages. */
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByStudentIdAndDriveId(Long studentId, Long driveId);

    long countByDriveId(Long driveId);

    /** HQL: one student's applications, newest first. */
    @Query("select a from Application a where a.student.id = :studentId order by a.appliedAt desc")
    List<Application> findByStudent(@Param("studentId") Long studentId);

    /** HQL with join fetch: loads each application and its student in one SQL query, best CGPA first. */
    @Query("select a from Application a join fetch a.student s where a.drive.id = :driveId order by s.cgpa desc")
    List<Application> findApplicantsOfDrive(@Param("driveId") Long driveId);

    /** HQL GROUP BY: rows of [companyName, roleTitle, applicationCount]; left join keeps drives with 0. */
    @Query("""
            select d.company.name, d.roleTitle, count(a)
            from Drive d left join Application a on a.drive = d
            group by d.id, d.company.name, d.roleTitle
            order by count(a) desc
            """)
    List<Object[]> countApplicationsPerDrive();
}
