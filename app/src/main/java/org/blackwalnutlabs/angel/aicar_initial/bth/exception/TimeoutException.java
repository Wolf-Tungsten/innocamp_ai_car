package org.blackwalnutlabs.angel.aicar_initial.bth.exception;


public class TimeoutException extends BleException {
    public TimeoutException() {
        super(ERROR_CODE_TIMEOUT, "Timeout Exception Occurred!");
    }
}
