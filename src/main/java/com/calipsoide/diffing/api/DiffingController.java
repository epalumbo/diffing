package com.calipsoide.diffing.api;

import com.calipsoide.diffing.api.DiffReportResource.DiffInsightResource;
import com.calipsoide.diffing.business.BinaryData;
import com.calipsoide.diffing.business.DiffSide;
import com.calipsoide.diffing.business.DiffingService;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Iterables.toArray;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Defines the HTTP endpoints of the application.
 * <p>
 * Implements the contract as described in the specification:
 * provides two endpoints to POST binary data to apply diff on
 * and another endpoint to GET diff results. These endpoints target resources
 * linked to what is called "diff case", with URI /v1/diff/:id.
 * <p>
 * Given that we don't know the context in which this API would be used,
 * its contract tries to respect the definitions of the specification. However,
 * different approaches could be taken into account depending on the context.
 * For instance, if both files can be submitted by client together then this
 * API could provide a POST endpoint to create the entire diff case at once,
 * return an HTTP 201 status code with the results and a location header
 * for further access by GET. Also, subsequent requests could be accepted
 * to modify any of the diff sides (PATCH or PUT, depending on the targeted
 * resource), triggering a diff update. Then, the endpoint of the API could be
 * organized in an alternative way:
 * - POST /v1/diff
 * - GET,PATCH,DELETE /v1/diff/:id
 * - GET,PUT,DELETE /v1/diff/:id/left
 * - GET,PUT,DELETE /v1/diff/:id/right
 * - GET /v1/diff/:id/results
 */
@RestController
@RequestMapping(path = "/v1/diff")
public class DiffingController {

    private final DiffingService diffingService;

    @Autowired
    public DiffingController(DiffingService diffingService) {
        this.diffingService = diffingService;
    }

    /**
     * POST endpoint that links binary data to a diff case.
     * The ID provided in the URI path is used as "case name" in order to link both sides of the diff.
     * Note that this endpoint accepts the both sides, "left" and "right".
     *
     * @param name of the diff resource to add this side data to
     * @param side of the data provided in request body
     * @param body JSON with base64 encoded binary data
     * @return 204 if operation succeeded, 400 if data is not readable
     */
    @RequestMapping(method = POST, path = "/{name}/{side:left|right}", consumes = APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> saveDataToDiff(
            @PathVariable("name") String name,
            @PathVariable("side") String side,
            @RequestBody BinaryDataResource body) {
        return Mono
                .justOrEmpty(body.data)
                .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("missing data")))
                .map(BinaryData::read)
                .flatMap(data -> {
                    final DiffSide diffSide = DiffSide.valueOf(side.toUpperCase());
                    return diffingService.process(name, diffSide, data);
                })
                .thenReturn(noContent().build())
                .onErrorResume(IllegalArgumentException.class, e -> {
                    final Map<String, String> errorBody = ImmutableMap.of("error", e.getMessage());
                    return Mono.just(badRequest().body(errorBody));
                });
    }

    /**
     * GET endpoint that returns diff results, if present.
     *
     * @param caseName to find diff results of
     * @return 200 with the diff results, 404 if no case is present with the URI path name
     */
    @RequestMapping(method = GET, path = "/{name}", produces = APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<DiffReportResource>> getDiffReport(@PathVariable("name") String caseName) {
        return diffingService
                .getReportOf(caseName)
                .map(report -> {
                    final String status = report.getStatus().toString().toLowerCase();
                    final List<DiffInsightResource> insights =
                            report.getInsights().stream()
                                    .map(insight -> new DiffInsightResource(insight.getOffset(), insight.getLength()))
                                    .collect(toList());
                    final DiffReportResource resource =
                            new DiffReportResource(status, toArray(insights, DiffInsightResource.class));
                    return ok(resource);
                })
                .defaultIfEmpty(notFound().build());
    }

}
