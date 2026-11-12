package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.SlaConfigurationRequest;
import com.hostelsync.backend.dto.SlaConfigurationResponse;
import com.hostelsync.backend.entity.ComplaintPriority;
import com.hostelsync.backend.entity.SlaConfiguration;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.SlaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaConfigurationControllerTest {

    @Mock
    private SlaService slaService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SlaConfigurationController slaConfigurationController;

    private User wardenUser;
    private User studentUser;
    private UserDetails wardenPrincipal;
    private UserDetails studentPrincipal;
    private SlaConfiguration urgentConfig;

    @BeforeEach
    void setUp() {
        wardenUser = new User();
        wardenUser.setId(1L);
        wardenUser.setEmail("warden@hostelsync.com");
        wardenUser.setName("Warden John");
        wardenUser.setRole(UserRole.WARDEN);

        studentUser = new User();
        studentUser.setId(2L);
        studentUser.setEmail("student@hostelsync.com");
        studentUser.setName("Student Jane");
        studentUser.setRole(UserRole.STUDENT);

        wardenPrincipal = org.springframework.security.core.userdetails.User
                .withUsername("warden@hostelsync.com").password("pass").roles("WARDEN").build();
        studentPrincipal = org.springframework.security.core.userdetails.User
                .withUsername("student@hostelsync.com").password("pass").roles("STUDENT").build();

        urgentConfig = new SlaConfiguration(ComplaintPriority.Urgent, 8);
        urgentConfig.setId(1L);
        urgentConfig.setReminderThresholdPercent(50);
    }

    @Test
    void getAllConfigurations_returnsList() {
        when(slaService.getConfigurationForPriority(any(ComplaintPriority.class))).thenReturn(urgentConfig);

        ResponseEntity<List<SlaConfigurationResponse>> response = slaConfigurationController.getAllConfigurations();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().size());
        assertEquals(ComplaintPriority.Urgent, response.getBody().get(0).getPriority());
    }

    @Test
    void getConfiguration_byId_returnsConfig() {
        when(slaService.getById(1L)).thenReturn(urgentConfig);

        ResponseEntity<SlaConfigurationResponse> response = slaConfigurationController.getConfiguration("1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    void getConfiguration_byPriority_returnsConfig() {
        when(slaService.getConfigurationForPriority(ComplaintPriority.Urgent)).thenReturn(urgentConfig);

        ResponseEntity<SlaConfigurationResponse> response = slaConfigurationController.getConfiguration("urgent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ComplaintPriority.Urgent, response.getBody().getPriority());
    }

    @Test
    void getConfiguration_invalidPriority_throwsException() {
        assertThrows(com.hostelsync.backend.exception.BadRequestException.class, () ->
                slaConfigurationController.getConfiguration("invalid_priority"));
    }

    @Test
    void createConfiguration_asWarden_returnsCreated() {
        when(userRepository.findByEmail("warden@hostelsync.com")).thenReturn(Optional.of(wardenUser));

        SlaConfigurationRequest request = new SlaConfigurationRequest(
                ComplaintPriority.Urgent,
                8,
                50
        );

        when(slaService.createConfiguration(eq(ComplaintPriority.Urgent), eq(8), eq(50)))
                .thenReturn(urgentConfig);

        ResponseEntity<SlaConfigurationResponse> response = slaConfigurationController.createConfiguration(request, wardenPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        verify(slaService).createConfiguration(ComplaintPriority.Urgent, 8, 50);
    }

    @Test
    void createConfiguration_asStudent_throwsForbidden() {
        when(userRepository.findByEmail("student@hostelsync.com")).thenReturn(Optional.of(studentUser));

        SlaConfigurationRequest request = new SlaConfigurationRequest(
                ComplaintPriority.Urgent,
                8,
                50
        );

        assertThrows(ForbiddenException.class, () ->
                slaConfigurationController.createConfiguration(request, studentPrincipal));

        verify(slaService, never()).createConfiguration(any(), anyInt(), anyInt());
    }

    @Test
    void updateConfiguration_byId_asWarden_returnsUpdated() {
        when(userRepository.findByEmail("warden@hostelsync.com")).thenReturn(Optional.of(wardenUser));

        SlaConfigurationRequest request = new SlaConfigurationRequest(
                ComplaintPriority.Urgent,
                12,
                60
        );

        SlaConfiguration updatedConfig = new SlaConfiguration(ComplaintPriority.Urgent, 12);
        updatedConfig.setId(1L);
        updatedConfig.setReminderThresholdPercent(60);

        when(slaService.updateConfiguration(eq(1L), eq(ComplaintPriority.Urgent), eq(12), eq(60))).thenReturn(updatedConfig);

        ResponseEntity<SlaConfigurationResponse> response = slaConfigurationController.updateConfiguration("1", request, wardenPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(12, response.getBody().getDeadlineHours());
    }

    @Test
    void updateConfiguration_byPriority_asWarden_returnsUpdated() {
        when(userRepository.findByEmail("warden@hostelsync.com")).thenReturn(Optional.of(wardenUser));

        SlaConfigurationRequest request = new SlaConfigurationRequest(
                ComplaintPriority.Urgent,
                10,
                40
        );

        SlaConfiguration updatedConfig = new SlaConfiguration(ComplaintPriority.Urgent, 10);
        updatedConfig.setId(1L);
        updatedConfig.setReminderThresholdPercent(40);

        when(slaService.upsertConfiguration(eq(ComplaintPriority.Urgent), eq(10), eq(40)))
                .thenReturn(updatedConfig);

        ResponseEntity<SlaConfigurationResponse> response = slaConfigurationController.updateConfiguration("Urgent", request, wardenPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ComplaintPriority.Urgent, response.getBody().getPriority());
    }
}
