package com.calipsoide.diffing;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureWebTestClient
class DiffingLogicApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void invalidSide() {
        webTestClient
                .post()
                .uri("/v1/diff/{id}/up", randomAlphanumeric(32))
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of("data",
                        Base64.getEncoder().encodeToString(randomAlphanumeric(32).getBytes(UTF_8)))))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void invalidData() {
        webTestClient
                .post()
                .uri("/v1/diff/{id}/left", randomAlphanumeric(32))
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of("data", ":-%-&-#")))// special characters out of Base64 range
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void missingData() {
        webTestClient
                .post()
                .uri("/v1/diff/{id}/left", randomAlphanumeric(32))
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of())) // no data attribute
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void invalidCase() {
        webTestClient
                .get()
                .uri("/v1/diff/{id}", randomAlphanumeric(32))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }


    @Test
    void processEqual() {
        final String id = randomAlphanumeric(32);
        final String data = Base64.getEncoder().encodeToString(randomAlphanumeric(32).getBytes(UTF_8));
        webTestClient
                .post()
                .uri("/v1/diff/{id}/left", id)
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of("data", data)))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
        webTestClient
                .post()
                .uri("/v1/diff/{id}/right", id)
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of("data", data)))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
        webTestClient
                .get()
                .uri("/v1/diff/{id}", id)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("status", "equal");
    }

    @Test
    void processJustLeftSide() {
        final String id = randomAlphanumeric(32);
        final String data = Base64.getEncoder().encodeToString(randomAlphanumeric(32).getBytes(UTF_8));
        webTestClient
                .post()
                .uri("/v1/diff/{id}/left", id)
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of("data", data)))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
        webTestClient
                .get()
                .uri("/v1/diff/{id}", id)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("status", "length_mismatch");
    }

    @Test
    void processJustRightSide() {
        final String id = randomAlphanumeric(32);
        final String data = Base64.getEncoder().encodeToString(randomAlphanumeric(32).getBytes(UTF_8));
        webTestClient
                .post()
                .uri("/v1/diff/{id}/right", id)
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of("data", data)))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
        webTestClient
                .get()
                .uri("/v1/diff/{id}", id)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("status", "length_mismatch");
    }

    @Test
    void processNotEqual() {
        final String id = randomAlphanumeric(32);
        final byte[] bytes = randomAlphanumeric(32).getBytes(UTF_8); // 16 bytes
        final String leftData = Base64.getEncoder().encodeToString(bytes);
        for (int i = 6; i < 9; i++) { // changing from 6 to 8 inclusive
            final int changed = ~bytes[i];
            bytes[i] = (byte) changed;
        }
        final String rightData = Base64.getEncoder().encodeToString(bytes);
        webTestClient
                .post()
                .uri("/v1/diff/{id}/left", id)
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of("data", leftData)))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
        webTestClient
                .post()
                .uri("/v1/diff/{id}/right", id)
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of("data", rightData)))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
        webTestClient
                .get()
                .uri("/v1/diff/{id}", id)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("status").isEqualTo("not_equal")
                .jsonPath("insights").isArray()
                .jsonPath("insights").value(hasSize(1))
                .jsonPath("insights[0].offset").isEqualTo(6)
                .jsonPath("insights[0].length").isEqualTo(3);
    }

    @Test
    void processOverride() {
        final String id = randomAlphanumeric(32);
        final byte[] bytes = randomAlphanumeric(32).getBytes(UTF_8); // 16 bytes
        final String leftData = Base64.getEncoder().encodeToString(bytes);
        final int changed = ~bytes[7];
        bytes[7] = (byte) changed;
        final String rightData = Base64.getEncoder().encodeToString(bytes);
        webTestClient
                .post()
                .uri("/v1/diff/{id}/left", id)
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of("data", leftData)))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
        webTestClient
                .post()
                .uri("/v1/diff/{id}/right", id)
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of("data", rightData)))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
        webTestClient
                .get()
                .uri("/v1/diff/{id}", id)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("status").isEqualTo("not_equal");
        webTestClient
                .post()
                .uri("/v1/diff/{id}/right", id)
                .contentType(APPLICATION_JSON)
                .body(fromObject(ImmutableMap.of("data", leftData))) // use as right same as left
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
        webTestClient
                .get()
                .uri("/v1/diff/{id}", id)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("status").isEqualTo("equal");
    }

}
