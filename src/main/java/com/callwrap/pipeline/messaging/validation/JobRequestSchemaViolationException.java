package com.callwrap.pipeline.messaging.validation;

import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.networknt.schema.ValidationMessage;

import java.util.Set;
import java.util.UUID;

public class JobRequestSchemaViolationException extends CallWrapException {

    public JobRequestSchemaViolationException(UUID callId, Set<ValidationMessage> violations) {
        super(ErrorCode.JOB_REQUEST_SCHEMA_VIOLATION);
    }
}
