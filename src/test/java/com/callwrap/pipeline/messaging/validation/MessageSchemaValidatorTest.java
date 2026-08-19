package com.callwrap.pipeline.messaging.validation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RabbitMQConfig.rabbitObjectMapper()와 같은 방식(snake_case)으로 만든 매퍼로
 * 실제 스키마 파일(schema/*.schema.json)을 검증한다. fixture는 src/test/resources/fixtures에 있다
 * — 계약(JobRequest/ResultMessage)이 실제로 무엇을 허용/거부하는지 문서 대신 여기서 확인한다.
 */
class MessageSchemaValidatorTest {

    private MessageSchemaValidator validator;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        validator = new MessageSchemaValidator(mapper);
    }

    @Test
    void 유효한_JobRequest는_위반이_없다() throws IOException {
        Set<ValidationMessage> violations = validator.validateJobRequest(fixture("job-request-valid.json"));

        assertThat(violations).isEmpty();
    }

    @Test
    void call_id가_없는_JobRequest는_위반이다() throws IOException {
        Set<ValidationMessage> violations =
            validator.validateJobRequest(fixture("job-request-invalid-missing-call-id.json"));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void schema_version이_1_0이_아니면_위반이다() throws IOException {
        Set<ValidationMessage> violations =
            validator.validateJobRequest(fixture("job-request-wrong-schema-version.json"));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void DONE_상태에_analysis가_있는_ResultMessage는_위반이_없다() throws IOException {
        Set<ValidationMessage> violations =
            validator.validateResultMessage(fixture("result-message-done-valid.json"));

        assertThat(violations).isEmpty();
    }

    @Test
    void DONE_상태인데_analysis가_없으면_위반이다() throws IOException {
        // ResultMessage.allOf: status=DONE이면 analysis가 required (schema/result-message.schema.json)
        Set<ValidationMessage> violations =
            validator.validateResultMessage(fixture("result-message-done-missing-analysis.json"));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void FAILED_상태에_error가_있는_ResultMessage는_위반이_없다() throws IOException {
        Set<ValidationMessage> violations =
            validator.validateResultMessage(fixture("result-message-failed-valid.json"));

        assertThat(violations).isEmpty();
    }

    @Test
    void FAILED_상태인데_error가_없으면_위반이다() throws IOException {
        // ResultMessage.allOf: status=FAILED면 error가 required
        Set<ValidationMessage> violations =
            validator.validateResultMessage(fixture("result-message-failed-missing-error.json"));

        assertThat(violations).isNotEmpty();
    }

    private JsonNode fixture(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(new ClassPathResource("fixtures/" + fileName).getFile().toPath());
        return validator.parse(bytes);
    }
}
