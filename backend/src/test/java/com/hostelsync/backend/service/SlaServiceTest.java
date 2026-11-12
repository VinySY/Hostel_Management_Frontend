package com.hostelsync.backend.service;

import com.hostelsync.backend.entity.ComplaintPriority;
import com.hostelsync.backend.entity.SlaConfiguration;
import com.hostelsync.backend.repository.SlaConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SlaService calculation and deadline logic.
 */
@ExtendWith(MockitoExtension.class)
class SlaServiceTest {

    @Mock
    private SlaConfigurationRepository slaConfigurationRepository;

    @InjectMocks
    private SlaService slaService;

    @Test
    void getConfigurationForPriority_returnsConfiguredValue() {
        // Arrange
        SlaConfiguration config = new SlaConfiguration();
        config.setPriority(ComplaintPriority.Urgent);
        config.setDeadlineHours(1);
        config.setReminderThresholdPercent(60);

        when(slaConfigurationRepository.findByPriority(ComplaintPriority.Urgent))
                .thenReturn(Optional.of(config));

        // Act
        SlaConfiguration result = slaService.getConfigurationForPriority(ComplaintPriority.Urgent);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getDeadlineHours());
        assertEquals(60, result.getReminderThresholdPercent());
        verify(slaConfigurationRepository).findByPriority(ComplaintPriority.Urgent);
    }

    @Test
    void getConfigurationForPriority_returnsDefaultWhenNotConfigured() {
        // Arrange
        when(slaConfigurationRepository.findByPriority(ComplaintPriority.High))
                .thenReturn(Optional.empty());

        // Act
        SlaConfiguration result = slaService.getConfigurationForPriority(ComplaintPriority.High);

        // Assert: Default for High is 8 hours, 50% threshold
        assertNotNull(result);
        assertEquals(8, result.getDeadlineHours());
        assertEquals(50, result.getReminderThresholdPercent());
    }

    @Test
    void getConfigurationForPriority_defaultsMatchExpected() {
        // Arrange
        when(slaConfigurationRepository.findByPriority(any())).thenReturn(Optional.empty());

        // Act & Assert: Verify all default SLA values
        SlaConfiguration urgent = slaService.getConfigurationForPriority(ComplaintPriority.Urgent);
        assertEquals(2, urgent.getDeadlineHours());
        assertEquals(50, urgent.getReminderThresholdPercent());

        SlaConfiguration high = slaService.getConfigurationForPriority(ComplaintPriority.High);
        assertEquals(8, high.getDeadlineHours());

        SlaConfiguration medium = slaService.getConfigurationForPriority(ComplaintPriority.Medium);
        assertEquals(24, medium.getDeadlineHours());

        SlaConfiguration low = slaService.getConfigurationForPriority(ComplaintPriority.Low);
        assertEquals(48, low.getDeadlineHours());
    }

    @Test
    void calculateDeadline_addsCorrectHours() {
        // Arrange
        SlaConfiguration config = new SlaConfiguration();
        config.setPriority(ComplaintPriority.Urgent);
        config.setDeadlineHours(2);

        when(slaConfigurationRepository.findByPriority(ComplaintPriority.Urgent))
                .thenReturn(Optional.of(config));

        LocalDateTime startTime = LocalDateTime.of(2026, 10, 20, 10, 0, 0);

        // Act
        LocalDateTime deadline = slaService.calculateDeadline(ComplaintPriority.Urgent, startTime);

        // Assert
        assertEquals(LocalDateTime.of(2026, 10, 20, 12, 0, 0), deadline);
    }

    @Test
    void isOverdue_nullDeadline_returnsFalse() {
        // Act
        boolean result = slaService.isOverdue(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void isOverdue_pastDeadline_returnsTrue() {
        // Arrange
        LocalDateTime pastDeadline = LocalDateTime.now().minusHours(2);

        // Act
        boolean result = slaService.isOverdue(pastDeadline);

        // Assert
        assertTrue(result);
    }

    @Test
    void isOverdue_futureDeadline_returnsFalse() {
        // Arrange
        LocalDateTime futureDeadline = LocalDateTime.now().plusHours(2);

        // Act
        boolean result = slaService.isOverdue(futureDeadline);

        // Assert
        assertFalse(result);
    }

    @Test
    void isApproachingDeadline_nullDeadline_returnsFalse() {
        // Act
        boolean result = slaService.isApproachingDeadline(LocalDateTime.now(), null, 50);

        // Assert
        assertFalse(result);
    }

    @Test
    void isApproachingDeadline_nullCreatedAt_returnsFalse() {
        // Act
        boolean result = slaService.isApproachingDeadline(null, LocalDateTime.now().plusHours(5), 50);

        // Assert
        assertFalse(result);
    }

    @Test
    void isApproachingDeadline_withinThreshold_returnsTrue() {
        // Arrange: 10-hour SLA, 50% threshold = approaching when 5 hours or less remain
        LocalDateTime createdAt = LocalDateTime.now().minusHours(7); // 7 hours ago
        LocalDateTime deadline = createdAt.plusHours(10); // 3 hours from now
        // Elapsed: 7 hours, Remaining: 3 hours, Percent remaining: 30%

        // Act
        boolean result = slaService.isApproachingDeadline(createdAt, deadline, 50);

        // Assert: 30% remaining is below 50% threshold
        assertTrue(result);
    }

    @Test
    void isApproachingDeadline_outsideThreshold_returnsFalse() {
        // Arrange: 10-hour SLA, 50% threshold = approaching when 5 hours or less remain
        LocalDateTime createdAt = LocalDateTime.now().minusHours(3); // 3 hours ago
        LocalDateTime deadline = createdAt.plusHours(10); // 7 hours from now
        // Elapsed: 3 hours, Remaining: 7 hours, Percent remaining: 70%

        // Act
        boolean result = slaService.isApproachingDeadline(createdAt, deadline, 50);

        // Assert: 70% remaining is above 50% threshold
        assertFalse(result);
    }

    @Test
    void isApproachingDeadline_exactlyAtThreshold_returnsTrue() {
        // Arrange: 10-hour SLA, 50% threshold
        LocalDateTime createdAt = LocalDateTime.now().minusHours(5); // 5 hours ago
        LocalDateTime deadline = createdAt.plusHours(10); // 5 hours from now
        // Percent remaining: exactly 50%

        // Act
        boolean result = slaService.isApproachingDeadline(createdAt, deadline, 50);

        // Assert: At or below threshold
        assertTrue(result);
    }

    @Test
    void upsertConfiguration_createsNewConfig() {
        // Arrange
        when(slaConfigurationRepository.findByPriority(ComplaintPriority.Urgent))
                .thenReturn(Optional.empty());
        when(slaConfigurationRepository.save(any(SlaConfiguration.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        SlaConfiguration result = slaService.upsertConfiguration(ComplaintPriority.Urgent, 1, 75);

        // Assert
        assertNotNull(result);
        assertEquals(ComplaintPriority.Urgent, result.getPriority());
        assertEquals(1, result.getDeadlineHours());
        assertEquals(75, result.getReminderThresholdPercent());
        verify(slaConfigurationRepository).save(any(SlaConfiguration.class));
    }

    @Test
    void upsertConfiguration_updatesExistingConfig() {
        // Arrange
        SlaConfiguration existingConfig = new SlaConfiguration();
        existingConfig.setPriority(ComplaintPriority.High);
        existingConfig.setDeadlineHours(8);
        existingConfig.setReminderThresholdPercent(50);

        when(slaConfigurationRepository.findByPriority(ComplaintPriority.High))
                .thenReturn(Optional.of(existingConfig));
        when(slaConfigurationRepository.save(any(SlaConfiguration.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        SlaConfiguration result = slaService.upsertConfiguration(ComplaintPriority.High, 6, 60);

        // Assert
        assertNotNull(result);
        assertEquals(6, result.getDeadlineHours());
        assertEquals(60, result.getReminderThresholdPercent());
        verify(slaConfigurationRepository).save(existingConfig);
    }

    @Test
    void serviceValidation_rejectsNegativeDeadlineOrThresholdAndAcceptsZeroBoundary() {
        assertThrows(com.hostelsync.backend.exception.BadRequestException.class, () ->
                slaService.upsertConfiguration(ComplaintPriority.High, -1, 50));
        assertThrows(com.hostelsync.backend.exception.BadRequestException.class, () ->
                slaService.upsertConfiguration(ComplaintPriority.High, 8, -1));
        assertThrows(com.hostelsync.backend.exception.BadRequestException.class, () ->
                slaService.createConfiguration(ComplaintPriority.Medium, -1, 50));
        assertThrows(com.hostelsync.backend.exception.BadRequestException.class, () ->
                slaService.updateConfiguration(1L, ComplaintPriority.Low, 10, -5));

        when(slaConfigurationRepository.findByPriority(ComplaintPriority.Low))
                .thenReturn(Optional.empty());
        when(slaConfigurationRepository.save(any(SlaConfiguration.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SlaConfiguration zeroBoundary = slaService.upsertConfiguration(ComplaintPriority.Low, 0, 0);
        assertEquals(0, zeroBoundary.getDeadlineHours());
        assertEquals(0, zeroBoundary.getReminderThresholdPercent());
    }
}
