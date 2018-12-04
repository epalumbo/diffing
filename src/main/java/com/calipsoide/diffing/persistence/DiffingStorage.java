package com.calipsoide.diffing.persistence;

import com.calipsoide.diffing.business.BinaryData;
import com.calipsoide.diffing.business.DiffCase;
import com.calipsoide.diffing.business.DiffInsight;
import com.calipsoide.diffing.business.DiffReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Encapsulates persistence logic, required by the system to save diff case state between requests.
 * <p>
 * An object-document mapping approach is implemented in this class,
 * thus separating business model from persistence issues. This way, changes in business model are
 * less error-prone regarding undesired schema changes at the database-level.
 * <p>
 * Many NoSQL databases could do this job well. A NoSQL database system like MongoDB enables the system
 * to easily scale up, as clustering is available almost without application-level changes.
 * Also, since its setup as embedded database is pretty straightforward in Spring Boot, it's a convenient
 * choice for this use case.
 * <p>
 * Note that we're using an embedded version of the database system that is not production-ready,
 * so it's not perfect to ensure integration testing, but good enough for the purposes of this development.
 */
@Repository
public class DiffingStorage {

    private final ReactiveMongoOperations mongo;

    @Autowired
    public DiffingStorage(ReactiveMongoTemplate mongo) {
        this.mongo = mongo;
    }

    private static DiffCase toDiffCaseEntity(DiffCaseDocument document) {
        return DiffCase
                .builder()
                .withId(document.id)
                .withName(document.name)
                .withLeftData(BinaryData.of(document.left))
                .withRightData(BinaryData.of(document.right))
                .withReport(toDiffReportEntity(document.report))
                .build();
    }

    private static DiffCaseDocument toDiffCaseDocument(DiffCase updated) {
        DiffCaseDocument document = new DiffCaseDocument();
        document.id = updated.getId();
        document.name = updated.getName();
        document.left = updated.getLeftData().getBytes();
        document.right = updated.getRightData().getBytes();
        document.report = toDiffReportDocument(updated.getReport());
        return document;
    }

    private static DiffReport toDiffReportEntity(DiffCaseDocument.DiffReportDocument document) {
        final DiffReport.Status status = DiffReport.Status.valueOf(document.status);
        final List<DiffInsight> insights = document.insights.stream()
                .map(item -> new DiffInsight(item.offset, item.length))
                .collect(toList());
        return DiffReport.of(status, insights);
    }

    private static DiffCaseDocument.DiffReportDocument toDiffReportDocument(DiffReport report) {
        final DiffCaseDocument.DiffReportDocument document = new DiffCaseDocument.DiffReportDocument();
        document.status = report.getStatus().toString();
        document.insights = report.getInsights().stream()
                .map(insight -> {
                    final DiffCaseDocument.DiffInsightDocument insightDocument = new DiffCaseDocument.DiffInsightDocument();
                    insightDocument.offset = insight.getOffset();
                    insightDocument.length = insight.getLength();
                    return insightDocument;
                })
                .collect(toList());
        return document;
    }

    public Mono<Void> save(DiffCase diffCase) {
        return Mono
                .just(diffCase)
                .map(DiffingStorage::toDiffCaseDocument)
                .flatMap(mongo::save)
                .then();
    }

    public Mono<DiffCase> getByName(String name) {
        final Query query = query(where("name").is(name));
        return mongo
                .findOne(query, DiffCaseDocument.class)
                .map(DiffingStorage::toDiffCaseEntity);
    }

    public Mono<DiffReport> getReportByCaseName(String name) {
        final Query query = query(where("name").is(name));
        query.fields().include("report"); // projection to load just report data
        return mongo
                .findOne(query, DiffCaseDocument.class)
                .map(document -> document.report)
                .map(DiffingStorage::toDiffReportEntity);
    }

}
