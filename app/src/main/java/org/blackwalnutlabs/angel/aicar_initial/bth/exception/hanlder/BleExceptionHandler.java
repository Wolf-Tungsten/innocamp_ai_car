package org.blackwalnutlabs.angel.aicar_initial.bth.exception.hanlder;

import org.blackwalnutlabs.angel.aicar_initial.bth.exception.ConnectException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.BleException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.BlueToothNotEnableException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.GattException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.NotFoundDeviceException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.OtherException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.ScanFailedException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.TimeoutException;


public abstract class BleExceptionHandler {

    public BleExceptionHandler handleException(BleException exception) {

        if (exception != null) {

            if (exception instanceof ConnectException) {
                onConnectException((ConnectException) exception);

            } else if (exception instanceof GattException) {
                onGattException((GattException) exception);

            } else if (exception instanceof TimeoutException) {
                onTimeoutException((TimeoutException) exception);

            } else if (exception instanceof NotFoundDeviceException) {
                onNotFoundDeviceException((NotFoundDeviceException) exception);

            } else if (exception instanceof BlueToothNotEnableException) {
                onBlueToothNotEnableException((BlueToothNotEnableException) exception);

            } else if (exception instanceof ScanFailedException) {
                onScanFailedException((ScanFailedException) exception);

            } else {
                onOtherException((OtherException) exception);
            }
        }
        return this;
    }

    /**
     * connect failed
     */
    protected abstract void onConnectException(ConnectException e);

    /**
     * gatt error status
     */
    protected abstract void onGattException(GattException e);

    /**
     * operation timeout
     */
    protected abstract void onTimeoutException(TimeoutException e);

    /**
     * not found device error
     */
    protected abstract void onNotFoundDeviceException(NotFoundDeviceException e);

    /**
     * bluetooth not enable error
     */
    protected abstract void onBlueToothNotEnableException(BlueToothNotEnableException e);

    /**
     * scan failed error
     */
    protected abstract void onScanFailedException(ScanFailedException e);

    /**
     * other exceptions
     */
    protected abstract void onOtherException(OtherException e);
}
