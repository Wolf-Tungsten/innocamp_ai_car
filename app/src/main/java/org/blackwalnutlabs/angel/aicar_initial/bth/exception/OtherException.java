package org.blackwalnutlabs.angel.aicar_initial.bth.exception;


public class OtherException extends BleException {
    public OtherException(String description) {
        super(ERROR_CODE_OTHER, description);
    }
}
