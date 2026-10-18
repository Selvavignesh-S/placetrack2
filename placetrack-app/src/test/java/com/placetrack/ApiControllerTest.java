package com.placetrack;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.placetrack.api.ApiController;
import com.placetrack.exception.ApplyException;
import com.placetrack.exception.EligibilityUnavailableException;
import com.placetrack.model.Application;
import com.placetrack.model.ApplicationStatus;
import com.placetrack.model.Company;
import com.placetrack.model.Drive;
import com.placetrack.model.Student;
import com.placetrack.service.ApplicationService;
import com.placetrack.service.CompanyService;
import com.placetrack.service.DriveService;

/** Calls the REST API through Spring MVC (no real server, no database). The services are Mockito fakes. */
@WebMvcTest(ApiController.class)
class ApiControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DriveService driveService;
    @MockitoBean CompanyService companyService;
    @MockitoBean ApplicationService applicationService;

    private static final String APPLY_BODY = "{\"studentId\": 3, \"driveId\": 2}";

    private Drive drive() {
        return new Drive(new Company("Nimbus Cloud", "Cloud"), "Cloud Engineer", 9, 7.5, 0, "IT,CSE",
                LocalDate.now().plusDays(7));
    }

    @Test
    void listDrivesReturnsJson() throws Exception {
        when(driveService.findOpenDrives()).thenReturn(List.of(drive()));

        mockMvc.perform(get("/api/drives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roleTitle").value("Cloud Engineer"))
                .andExpect(jsonPath("$[0].company.name").value("Nimbus Cloud"));
    }

    @Test
    void applyReturns201AndHidesThePasswordHash() throws Exception {
        Student student = new Student("Aarav", "a@x.edu", "R1", "IT", 8.6, 0);
        student.setPassword("secret-hash");
        when(applicationService.apply(3L, 2L))
                .thenReturn(new Application(student, drive(), ApplicationStatus.APPLIED));

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(APPLY_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.student.password").doesNotExist());
    }

    @Test
    void ineligibleApplyReturns400WithReasons() throws Exception {
        when(applicationService.apply(3L, 2L)).thenThrow(
                new ApplyException("You are not eligible for this drive", List.of("CGPA 7.2 is below the required 7.5")));

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(APPLY_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You are not eligible for this drive"))
                .andExpect(jsonPath("$.reasons[0]").value("CGPA 7.2 is below the required 7.5"));
    }

    @Test
    void serviceDownReturns503() throws Exception {
        when(applicationService.apply(3L, 2L))
                .thenThrow(new EligibilityUnavailableException(new RuntimeException("refused")));

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(APPLY_BODY))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void missingDriveIdReturns400() throws Exception {
        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content("{\"studentId\": 3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.driveId").exists());
    }

    @Test
    void createCompanyIgnoresAnIdInTheBody() throws Exception {
        when(companyService.save(argThat(c -> c.getId() == null))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 1, \"name\": \"Acme\", \"sector\": \"Robotics\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme"));
        verify(companyService).save(argThat(c -> c.getId() == null));
    }
}
