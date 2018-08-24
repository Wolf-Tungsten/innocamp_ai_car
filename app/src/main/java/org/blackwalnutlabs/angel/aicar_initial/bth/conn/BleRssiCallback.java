package org.blackwalnutlabs.angel.aicar_initial.bth.conn;


public abstract class BleRssiCallback extends BleCallback {
    public abstract void onSuccess(int rssi);
}