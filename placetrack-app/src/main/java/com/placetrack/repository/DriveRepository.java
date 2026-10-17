package com.placetrack.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.placetrack.model.Drive;

/** CRUD for drives plus the HQL query for drives students can still apply to. */
public interface DriveRepository extends JpaRepository<Drive, Long> {

    /** HQL: uses the class name Drive and field name deadline, not the table and column names. */
    @Query("select d from Drive d where d.deadline >= :today order by d.deadline")
    List<Drive> findOpenDrives(@Param("today") LocalDate today);

    long countByCompanyId(Long companyId);
}
