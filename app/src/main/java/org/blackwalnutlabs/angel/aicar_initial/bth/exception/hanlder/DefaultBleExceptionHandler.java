package org.blackwalnutlabs.angel.aicar_initial.bth.exception.hanlder;

import org.blackwalnutlabs.angel.aicar_initial.bth.exception.BlueToothNotEnableException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.ConnectException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.GattException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.NotFoundDeviceException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.OtherException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.ScanFailedException;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.TimeoutException;
import org.blackwalnutlabs.angel.aicar_initial.bth.utils.BleLog;

public class DefaultBleExceptionHandler extends BleExceptionHandler {

    private static final String TAG = "BleExceptionHandler";

    public DefaultBleExceptionHandler() {

    }

    @Override
    protected void onConnectException(ConnectException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onGattException(GattException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onTimeoutException(TimeoutException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onNotFoundDeviceException(NotFoundDeviceException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onBlueToothNotEnableException(BlueToothNotEnableException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onScanFailedException(ScanFailedException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onOtherException(OtherException e) {
        BleLog.e(TAG, e.getDescription());
    }
}
