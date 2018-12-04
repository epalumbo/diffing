package com.calipsoide.diffing.business;

import com.calipsoide.diffing.persistence.DiffingStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.calipsoide.diffing.business.DiffReport.Status.LENGTH_MISMATCH;
import static com.calipsoide.diffing.business.DiffReport.Status.NOT_EQUAL;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiffingServiceTest {

    private DiffingService service;

    @Mock
    private DiffingStorage storage;

    @Mock
    private DiffingLogic logic;

    @BeforeEach
    void setUp() {
        service = new DiffingService(storage, logic);
    }

    @Test
    @DisplayName("returns an empty result if report is not present in storage")
    void reportNotFound() {
        final String name = randomAlphanumeric(32);
        when(storage.getReportByCaseName(name)).thenReturn(Mono.empty());
        StepVerifier
                .create(service.getReportOf(name))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("returns report if present in storage")
    void reportFound() {
        final String name = randomAlphanumeric(32);
        final DiffReport stored = DiffReport.of(DiffReport.Status.EQUAL);
        when(storage.getReportByCaseName(name)).thenReturn(Mono.just(stored));
        StepVerifier
                .create(service.getReportOf(name))
                .expectNext(stored)
                .verifyComplete();
    }

    @Test
    @DisplayName("safely fails if storage read fails")
    void getReportSafeFailure() {
        final String name = randomAlphanumeric(32);
        when(storage.getReportByCaseName(name)).thenReturn(Mono.error(new RuntimeException("ups!")));
        StepVerifier
                .create(service.getReportOf(name))
                .verifyErrorMessage("ups!");
    }

    @Test
    @DisplayName("creates a new case from left side only")
    void saveNewCaseUsingLeft() {
        final String name = randomAlphanumeric(32);
        final BinaryData binaryData = BinaryData.of(nextBytes(32));
        final DiffReport report = DiffReport.of(LENGTH_MISMATCH);
        when(storage.getByName(name)).thenReturn(Mono.empty());
        when(logic.diff(eq(binaryData), argThat(data -> data.getLength() == 0))).thenReturn(report);
        when(storage.save(any(DiffCase.class))).thenReturn(Mono.empty());
        StepVerifier
                .create(service.process(name, DiffSide.LEFT, binaryData))
                .verifyComplete();
        final ArgumentCaptor<DiffCase> captor = ArgumentCaptor.forClass(DiffCase.class);
        verify(storage).save(captor.capture());
        final DiffCase diffCase = captor.getValue();
        assertThat(diffCase.getName()).isEqualTo(name);
        assertThat(diffCase.getLeftData()).isEqualTo(binaryData);
        assertThat(diffCase.getRightData().getLength()).isEqualTo(0);
        assertThat(diffCase.getReport()).isEqualTo(report);
    }

    @Test
    @DisplayName("creates a new case from right side only")
    void saveNewCaseUsingRight() {
        final String name = randomAlphanumeric(32);
        final BinaryData binaryData = BinaryData.of(nextBytes(32));
        final DiffReport report = DiffReport.of(LENGTH_MISMATCH);
        when(storage.getByName(name)).thenReturn(Mono.empty());
        when(logic.diff(argThat(data -> data.getLength() == 0), eq(binaryData))).thenReturn(report);
        when(storage.save(any(DiffCase.class))).thenReturn(Mono.empty());
        StepVerifier
                .create(service.process(name, DiffSide.RIGHT, binaryData))
                .verifyComplete();
        final ArgumentCaptor<DiffCase> captor = ArgumentCaptor.forClass(DiffCase.class);
        verify(storage).save(captor.capture());
        final DiffCase diffCase = captor.getValue();
        assertThat(diffCase.getName()).isEqualTo(name);
        assertThat(diffCase.getLeftData().getLength()).isEqualTo(0);
        assertThat(diffCase.getRightData()).isEqualTo(binaryData);
        assertThat(diffCase.getReport()).isEqualTo(report);
    }

    @Test
    @DisplayName("updates case state in storage when two sides are present")
    void processWithBothSides() {
        final String name = randomAlphanumeric(32);
        final BinaryData leftData = BinaryData.of(nextBytes(32));
        final BinaryData rightData = BinaryData.of(nextBytes(32));
        final DiffCase originalDiffCase =
                DiffCase.builder()
                        .withName(name)
                        .withLeftData(leftData)
                        .withRightData(BinaryData.empty()) // right side is not added yet
                        .withReport(DiffReport.of(LENGTH_MISMATCH))
                        .build();
        final DiffReport report = DiffReport.of(NOT_EQUAL);
        when(storage.getByName(name)).thenReturn(Mono.just(originalDiffCase));
        when(logic.diff(leftData, rightData)).thenReturn(report);
        when(storage.save(any(DiffCase.class))).thenReturn(Mono.empty());
        StepVerifier
                .create(service.process(name, DiffSide.RIGHT, rightData)) // now add right side
                .verifyComplete();
        final ArgumentCaptor<DiffCase> captor = ArgumentCaptor.forClass(DiffCase.class);
        verify(storage).save(captor.capture());
        final DiffCase updatedDiffCase = captor.getValue();
        assertThat(updatedDiffCase.getName()).isEqualTo(name);
        assertThat(updatedDiffCase.getLeftData()).isEqualTo(leftData);
        assertThat(updatedDiffCase.getRightData()).isEqualTo(rightData);
        assertThat(updatedDiffCase.getReport()).isEqualTo(report);
    }

    @Test
    @DisplayName("updates case state in storage when a side is changed")
    void processOverride() {
        final String name = randomAlphanumeric(32);
        final BinaryData leftData = BinaryData.of(nextBytes(32));
        final BinaryData rightData = BinaryData.of(nextBytes(32));
        final DiffCase originalDiffCase =
                DiffCase.builder()
                        .withName(name)
                        .withLeftData(leftData)
                        .withRightData(BinaryData.of(nextBytes(16))) // right side will be replaced
                        .withReport(DiffReport.of(LENGTH_MISMATCH))
                        .build();
        when(storage.getByName(name)).thenReturn(Mono.just(originalDiffCase));
        when(logic.diff(leftData, rightData)).thenReturn(DiffReport.of(NOT_EQUAL));
        when(storage.save(any(DiffCase.class))).thenReturn(Mono.empty());
        StepVerifier
                .create(service.process(name, DiffSide.RIGHT, rightData)) // adding right side
                .verifyComplete();
        final ArgumentCaptor<DiffCase> captor = ArgumentCaptor.forClass(DiffCase.class);
        verify(storage).save(captor.capture());
        final DiffCase updatedDiffCase = captor.getValue();
        assertThat(updatedDiffCase.getName()).isEqualTo(name);
        assertThat(updatedDiffCase.getLeftData()).isEqualTo(leftData);
        assertThat(updatedDiffCase.getRightData()).isEqualTo(rightData);
    }

    @Test
    @DisplayName("safely fails if storage write fails")
    void saveCaseSafeFailure() {
        final String name = randomAlphanumeric(32);
        final BinaryData leftData = BinaryData.of(nextBytes(32));
        final BinaryData rightData = BinaryData.of(nextBytes(32));
        final DiffCase originalDiffCase =
                DiffCase.builder()
                        .withName(name)
                        .withLeftData(BinaryData.empty())
                        .withRightData(rightData)
                        .withReport(DiffReport.of(LENGTH_MISMATCH))
                        .build();
        final DiffReport report = DiffReport.of(NOT_EQUAL);
        when(storage.getByName(name)).thenReturn(Mono.just(originalDiffCase));
        when(logic.diff(leftData, rightData)).thenReturn(report);
        when(storage.save(any(DiffCase.class))).thenReturn(Mono.error(new RuntimeException("ups!")));
        StepVerifier
                .create(service.process(name, DiffSide.LEFT, leftData))
                .verifyErrorMessage("ups!");
    }

}
