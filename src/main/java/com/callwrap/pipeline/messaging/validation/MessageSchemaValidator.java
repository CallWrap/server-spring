package com.callwrap.pipeline.messaging.validation;

import com.callwrap.pipeline.messaging.ResultMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Component
public class MessageSchemaValidator {

    private static final String JOB_REQUEST_SCHEMA_PATH = "schema/job-request.schema.json";
    private static final String RESULT_MESSAGE_SCHEMA_PATH = "schema/result-message.schema.json";

    private final ObjectMapper mapper;
    private final JsonSchema jobRequestSchema;
    private final JsonSchema resultMessageSchema;

    public MessageSchemaValidator(@Qualifier("rabbitObjectMapper") ObjectMapper mapper) {
        this.mapper = mapper;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        this.jobRequestSchema = factory.getSchema(load(JOB_REQUEST_SCHEMA_PATH));
        this.resultMessageSchema = factory.getSchema(load(RESULT_MESSAGE_SCHEMA_PATH));
    }

    public JsonNode toJsonNode(Object message) {
        return mapper.valueToTree(message);
    }

    public JsonNode parse(byte[] rawJson) throws IOException {
        return mapper.readTree(rawJson);
    }

    public Set<ValidationMessage> validateJobRequest(JsonNode node) {
        return jobRequestSchema.validate(node);
    }

    public Set<ValidationMessage> validateResultMessage(JsonNode node) {
        return resultMessageSchema.validate(node);
    }

    public ResultMessage toResultMessage(JsonNode node) {
        return mapper.convertValue(node, ResultMessage.class);
    }

    private InputStream load(String classpathLocation) {
        try {
            return new ClassPathResource(classpathLocation).getInputStream();
        } catch (IOException e) {
            throw new IllegalStateException("스키마 파일을 못 읽음: " + classpathLocation, e);
        }
    }
}
