package com.calipsoide.diffing.persistence;

import com.calipsoide.diffing.business.BinaryData;
import com.calipsoide.diffing.business.DiffCase;
import com.calipsoide.diffing.business.DiffInsight;
import com.calipsoide.diffing.business.DiffReport;
import com.calipsoide.diffing.persistence.DiffCaseDocument.DiffInsightDocument;
import com.calipsoide.diffing.persistence.DiffCaseDocument.DiffReportDocument;
import com.google.common.collect.ImmutableList;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static com.calipsoide.diffing.business.DiffReport.Status.LENGTH_MISMATCH;
import static com.calipsoide.diffing.business.DiffReport.Status.NOT_EQUAL;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiffingStorageTest {

    private DiffingStorage storage;

    @Mock
    private ReactiveMongoTemplate mongo;

    @BeforeEach
    void setUp() {
        storage = new DiffingStorage(mongo);
    }

    @Test
    void saveInsert() {
        final DiffCase diffCase =
                DiffCase.builder()
                        .withName(randomAlphanumeric(32))
                        .withLeftData(BinaryData.of(nextBytes(64)))
                        .withRightData(BinaryData.empty())
                        .withReport(DiffReport.of(LENGTH_MISMATCH))
                        .build();
        when(mongo.save(any(DiffCaseDocument.class))).thenReturn(Mono.empty());
        StepVerifier
                .create(storage.save(diffCase))
                .verifyComplete();
        final ArgumentCaptor<DiffCaseDocument> captor = ArgumentCaptor.forClass(DiffCaseDocument.class);
        verify(mongo).save(captor.capture());
        final DiffCaseDocument document = captor.getValue();
        assertThat(document.id).isNull();
        assertThat(document.left).isEqualTo(diffCase.getLeftData().getBytes());
        assertThat(document.right).isEmpty();
        final DiffReportDocument reportDocument = document.report;
        assertThat(reportDocument.status).isEqualTo(diffCase.getReport().getStatus().toString());
        assertThat(reportDocument.insights).isEmpty();
    }

    @Test
    void saveUpdate() {
        final DiffCase diffCase =
                DiffCase.builder()
                        .withId(randomAlphanumeric(16))
                        .withName(randomAlphanumeric(32))
                        .withLeftData(BinaryData.of(nextBytes(64)))
                        .withRightData(BinaryData.of(nextBytes(64)))
                        .withReport(DiffReport.of(NOT_EQUAL, ImmutableList.of(new DiffInsight(0, 64))))
                        .build();
        when(mongo.save(any(DiffCaseDocument.class))).thenReturn(Mono.empty());
        StepVerifier
                .create(storage.save(diffCase))
                .verifyComplete();
        final ArgumentCaptor<DiffCaseDocument> captor = ArgumentCaptor.forClass(DiffCaseDocument.class);
        verify(mongo).save(captor.capture());
        final DiffCaseDocument document = captor.getValue();
        assertThat(document.id).isEqualTo(diffCase.getId());
        assertThat(document.name).isEqualTo(diffCase.getName());
        assertThat(document.left).isEqualTo(diffCase.getLeftData().getBytes());
        assertThat(document.right).isEqualTo(diffCase.getRightData().getBytes());
        final DiffReportDocument reportDocument = document.report;
        assertThat(reportDocument.status).isEqualTo(diffCase.getReport().getStatus().toString());
        final List<DiffInsightDocument> insightDocuments = reportDocument.insights;
        assertThat(insightDocuments).hasSize(1);
        final DiffInsightDocument insightDocument = insightDocuments.get(0);
        assertThat(insightDocument.offset).isEqualTo(0);
        assertThat(insightDocument.length).isEqualTo(64);
    }

    @Test
    void saveSafeFailure() {
        final DiffCase diffCase =
                DiffCase.builder()
                        .withId(randomAlphanumeric(16))
                        .withName(randomAlphanumeric(32))
                        .withLeftData(BinaryData.of(nextBytes(64)))
                        .withRightData(BinaryData.empty())
                        .withReport(DiffReport.of(LENGTH_MISMATCH))
                        .build();
        when(mongo.save(any(DiffCaseDocument.class))).thenReturn(Mono.error(new RuntimeException("ups!")));
        StepVerifier
                .create(storage.save(diffCase))
                .verifyErrorMessage("ups!");
    }

    @Test
    void getByName() {
        final String name = randomAlphanumeric(32);
        final DiffCaseDocument document = new DiffCaseDocument();
        document.id = randomAlphanumeric(16);
        document.name = name;
        document.left = nextBytes(64);
        document.right = nextBytes(64);
        final DiffReportDocument reportDocument = new DiffReportDocument();
        reportDocument.status = NOT_EQUAL.name();
        final DiffInsightDocument insightDocument = new DiffInsightDocument();
        insightDocument.offset = 3;
        insightDocument.length = 7;
        reportDocument.insights = ImmutableList.of(insightDocument);
        document.report = reportDocument;
        when(mongo.findOne(argThat(query -> {
            final Document queryObject = query.getQueryObject();
            return queryObject.get("name").equals(name);
        }), eq(DiffCaseDocument.class))).thenReturn(Mono.just(document));
        StepVerifier
                .create(storage.getByName(name))
                .assertNext(diffCase -> {
                    assertThat(diffCase.getId()).isEqualTo(document.id);
                    assertThat(diffCase.getName()).isEqualTo(name);
                    assertThat(diffCase.getLeftData().getBytes()).isEqualTo(document.left);
                    assertThat(diffCase.getRightData().getBytes()).isEqualTo(document.right);
                    final DiffReport report = diffCase.getReport();
                    assertThat(report.getStatus()).isEqualTo(NOT_EQUAL);
                    final List<DiffInsight> insights = report.getInsights();
                    assertThat(insights).hasSize(1);
                    final DiffInsight insight = insights.get(0);
                    assertThat(insight.getOffset()).isEqualTo(insightDocument.offset);
                    assertThat(insight.getLength()).isEqualTo(insightDocument.length);
                })
                .verifyComplete();
    }

    @Test
    void getByNameNotFound() {
        final String name = randomAlphanumeric(32);
        when(mongo.findOne(any(Query.class), eq(DiffCaseDocument.class))).thenReturn(Mono.empty());
        StepVerifier
                .create(storage.getByName(name))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void getByNameSafeFailure() {
        final String name = randomAlphanumeric(32);
        when(mongo.findOne(any(Query.class), eq(DiffCaseDocument.class)))
                .thenReturn(Mono.error(new RuntimeException("ups!")));
        StepVerifier
                .create(storage.getByName(name))
                .verifyErrorMessage("ups!");
    }

    @Test
    void getReportByName() {
        final String name = randomAlphanumeric(32);
        final DiffCaseDocument document = new DiffCaseDocument();
        final DiffReportDocument reportDocument = new DiffReportDocument();
        reportDocument.status = NOT_EQUAL.name();
        final DiffInsightDocument insightDocument = new DiffInsightDocument();
        insightDocument.offset = 3;
        insightDocument.length = 7;
        reportDocument.insights = ImmutableList.of(insightDocument);
        document.report = reportDocument;
        when(mongo.findOne(argThat(query -> {
            final Document queryObject = query.getQueryObject();
            final Document fieldsObject = query.getFieldsObject();
            return queryObject.get("name").equals(name) && fieldsObject.get("report").equals(1);
        }), eq(DiffCaseDocument.class))).thenReturn(Mono.just(document));
        StepVerifier
                .create(storage.getReportByCaseName(name))
                .assertNext(report -> {
                    assertThat(report.getStatus()).isEqualTo(NOT_EQUAL);
                    final List<DiffInsight> insights = report.getInsights();
                    assertThat(insights).hasSize(1);
                    final DiffInsight insight = insights.get(0);
                    assertThat(insight.getOffset()).isEqualTo(insightDocument.offset);
                    assertThat(insight.getLength()).isEqualTo(insightDocument.length);
                })
                .verifyComplete();
    }

    @Test
    void getReportByNameNotFound() {
        final String name = randomAlphanumeric(32);
        when(mongo.findOne(any(Query.class), eq(DiffCaseDocument.class))).thenReturn(Mono.empty());
        StepVerifier
                .create(storage.getReportByCaseName(name))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void getReportByNameSafeFailure() {
        final String name = randomAlphanumeric(32);
        when(mongo.findOne(any(Query.class), eq(DiffCaseDocument.class)))
                .thenReturn(Mono.error(new RuntimeException("ups!")));
        StepVerifier
                .create(storage.getReportByCaseName(name))
                .verifyErrorMessage("ups!");
    }

}