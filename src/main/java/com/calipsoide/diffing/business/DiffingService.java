package com.calipsoide.diffing.business;

import com.calipsoide.diffing.persistence.DiffingStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.calipsoide.diffing.business.DiffSide.LEFT;

/**
 * Main class of the business layer.
 */
@Service
public class DiffingService {

    private final DiffingStorage diffingStorage;

    private final DiffingLogic diffingLogic;

    @Autowired
    DiffingService(DiffingStorage diffingStorage, DiffingLogic diffingLogic) {
        this.diffingStorage = diffingStorage;
        this.diffingLogic = diffingLogic;
    }

    private static Mono<DiffCase> newCase(String name) {
        return Mono.create(callback ->
                callback.success(
                        DiffCase.builder()
                                .withName(name)
                                .withLeftData(BinaryData.empty())
                                .withRightData(BinaryData.empty())
                                .build()));
    }

    /**
     * Given a case name, creates or updates the diff with the binary data provided for the specified side.
     * Diff case is persisted in database for further query / data override.
     *
     * @param name of the diff case
     * @param side of the data
     * @param data binary to put in the case
     * @return completion signal if operation succeeded, error in any other case
     */
    public Mono<Void> process(String name, DiffSide side, BinaryData data) {
        return diffingStorage
                .getByName(name)
                .switchIfEmpty(newCase(name))
                .map(diffCase -> {
                    final BinaryData leftData = LEFT.equals(side) ? data : diffCase.getLeftData();
                    final BinaryData rightData = LEFT.equals(side) ? diffCase.getRightData() : data;
                    final DiffReport report = diffingLogic.diff(leftData, rightData);
                    return diffCase
                            .copy()
                            .withLeftData(leftData)
                            .withRightData(rightData)
                            .withReport(report)
                            .build();
                })
                .flatMap(diffingStorage::save)
                .then();
    }

    /**
     * Returns the diff results that were previously computed for a case.
     *
     * @param caseName to find results of
     * @return the diff results, if present
     */
    public Mono<DiffReport> getReportOf(String caseName) {
        return diffingStorage.getReportByCaseName(caseName);
    }

}
