
package org.blackwalnutlabs.angel.aicar_initial.bth.conn;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import org.blackwalnutlabs.angel.aicar_initial.bth.data.ScanResult;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.BleException;


public abstract class BleGattCallback extends BluetoothGattCallback {

    public void onFoundDevice(ScanResult scanResult) {
    }

    public void onConnecting(BluetoothGatt gatt, int status) {
    }

    public abstract void onConnectError(BleException exception);

    public abstract void onConnectSuccess(BluetoothGatt gatt, int status);

    public abstract void onDisConnected(BluetoothGatt gatt, int status, BleException exception);

}