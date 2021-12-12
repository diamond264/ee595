package com.example.ee595android;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.content.Intent;
import android.util.Log;
import android.net.Uri;
import android.app.Activity;

import com.example.ee595android.ServiceFragment.ServiceFragmentDelegate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

public class YoutubeService extends Service implements ServiceFragmentDelegate {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "EE595B";

    private static final UUID CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
            .fromString("00002901-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    private ServiceFragment mCurrentServiceFragment;
    private BluetoothGattService mBluetoothGattService;
    private HashSet<BluetoothDevice> mBluetoothDevices;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private AdvertiseData mAdvData;
    private AdvertiseData mAdvScanResponse;
    private AdvertiseSettings mAdvSettings;
    private BluetoothLeAdvertiser mAdvertiser;
    private SensorManager sensorManager;

    private final AdvertiseCallback mAdvCallback = new AdvertiseCallback(){
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect){
            super.onStartSuccess(settingsInEffect);
            Log.v(TAG, "Broadcasting");
        }
    };

    private BluetoothGattServer mGattServer;
    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBluetoothDevices.add(device);
                    mAdvertiser.stopAdvertising(mAdvCallback);
                    Log.v(TAG, "Connected to device: " + device.getAddress());
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevices.remove(device);
                    mAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvScanResponse, mAdvCallback);
                    Log.v(TAG, "Disconnected from device");
                }
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
                        /* value (optional) */ null);
                return;
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.getValue());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value);
            String link = new String(value, StandardCharsets.UTF_8);
            Log.v(TAG, "Characteristic Write request: " + link);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("Link", link);
            intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            int status = mCurrentServiceFragment.writeCharacteristic(characteristic, offset, value);
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, status,
                        /* No need to respond with an offset */ 0,
                        /* No need to respond with a value */ null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                            int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.d(TAG, "Device tried to read descriptor: " + descriptor.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(descriptor.getValue()));
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
                        /* value (optional) */ null);
                return;
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    descriptor.getValue());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                             int offset,
                                             byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
                    offset, value);
            Log.v(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
            int status = BluetoothGatt.GATT_SUCCESS;
            if (descriptor.getUuid() == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                boolean supportsNotifications = (characteristic.getProperties() &
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
                boolean supportsIndications = (characteristic.getProperties() &
                        BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;

                if (!(supportsNotifications || supportsIndications)) {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                } else if (value.length != 2) {
                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
                } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    mCurrentServiceFragment.notificationsDisabled(characteristic);
                    descriptor.setValue(value);
                } else if (supportsNotifications &&
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    mCurrentServiceFragment.notificationsEnabled(characteristic, false /* indicate */);
                    descriptor.setValue(value);
                } else if (supportsIndications &&
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    mCurrentServiceFragment.notificationsEnabled(characteristic, true /* indicate */);
                    descriptor.setValue(value);
                } else {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                }
            } else {
                status = BluetoothGatt.GATT_SUCCESS;
                descriptor.setValue(value);
            }
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, status,
                        /* No need to respond with offset */ 0,
                        /* No need to respond with a value */ null);
            }
        }
    };

    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];
    private float[] linAccelerometerReading = new float[3];
    private float[] gyroReading = new float[3];

    private int accReadingNum = 0;
    private int magReadingNum = 0;
    private int linAccReadingNum = 0;
    private int gyroReadingNum = 0;

    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    private Sensor accSensor;
    private Sensor linAccSensor;
    private Sensor magSensor;
    private Sensor gyroSensor;

    private double vx_add, vy_add, vz_add;
    private double pos_vx, pos_vy, pos_vz;
    private double x, y, z;
    private double vx, vy, vz;
    private long tOld, tCurrent, tDelta;

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                for(int i=0;i<3;i++){
                    accelerometerReading[i] += sensorEvent.values[i];
                }
                accReadingNum += 1;
            }
            else if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                for(int i=0;i<3;i++){
                    magnetometerReading[i] += sensorEvent.values[i];
                }
                magReadingNum += 1;
            }
            else if(sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
                for(int i=0;i<3;i++){
                    linAccelerometerReading[i] += sensorEvent.values[i];
                }
                linAccReadingNum += 1;
            }
            else if(sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                for(int i=0;i<3;i++){
                    gyroReading[i] += sensorEvent.values[i];
                }
                gyroReadingNum += 1;
            }

            if(accReadingNum > 10 && magReadingNum > 10 && linAccReadingNum > 10 && gyroReadingNum > 10){
                for(int i=0;i<3;i++){
                    accelerometerReading[i] /= accReadingNum;
                    magnetometerReading[i] /= magReadingNum;
                    linAccelerometerReading[i] /= linAccReadingNum;
                    gyroReading[i] /= gyroReadingNum;
                }

                float gx = gyroReading[0];
                float gy = gyroReading[1];
                float gz = gyroReading[2];
                float ax = linAccelerometerReading[0];
                float ay = linAccelerometerReading[1];
                float az = linAccelerometerReading[2];

                tCurrent = System.currentTimeMillis();
                tDelta = tCurrent - tOld;
                tOld = tCurrent;

                vx_add = 4.9 * ax * tDelta / 1000.0;
                vy_add = 4.9 * ay * tDelta / 1000.0;
                vz_add = 4.9 * az * tDelta / 1000.0;


                pos_vx = vx + vx_add;
                pos_vy = vy + vy_add;
                pos_vz = vz + vz_add;

                vx = pos_vx + vx_add;
                vy = pos_vy + vy_add;
                vz = pos_vz + vz_add;

                float thres = gx*gx + gy*gy + gz*gz;
                if(thres < 0.0005){
                    pos_vx = 0;
                    pos_vy = 0;
                    pos_vz = 0;

                    vx = 0;
                    vy = 0;
                    vz = 0;
                }

                x = x + pos_vx * tDelta / 1000.0;
                y = y + pos_vy * tDelta / 1000.0;
                z = z + pos_vz * tDelta / 1000.0;


                SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
                SensorManager.getOrientation(rotationMatrix, orientationAngles);
                BluetoothGattService curService = mCurrentServiceFragment.getBluetoothGattService();
                BluetoothGattCharacteristic curCharacteristic = curService.getCharacteristic(UUID
                        .fromString("00002A19-0000-1000-8000-00805f9b34fb"));

                float[] send_arr = new float[]{orientationAngles[0], orientationAngles[1], orientationAngles[2], (float) x*100, (float) y*100, (float) z*100};
                curCharacteristic.setValue(Arrays.toString(send_arr));
                sendNotificationToDevices(curCharacteristic);


                for(int i=0;i<3;i++){
                    accelerometerReading[i] = 0;
                    magnetometerReading[i] = 0;
                    linAccelerometerReading[i] = 0;
                    gyroReading[i] = 0;
                    gyroReadingNum = 0;
                    magReadingNum = 0;
                    accReadingNum = 0;
                    linAccReadingNum = 0;
                }
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            Log.d(TAG, sensor.toString() + "-" + i);
        }
    };


    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();

        mBluetoothDevices = new HashSet<>();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothAdapter.setName("EE595B_Phone");

        mCurrentServiceFragment = new YoutubeFragment();

        mBluetoothGattService = mCurrentServiceFragment.getBluetoothGattService();

        mAdvSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();
        mAdvData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(mCurrentServiceFragment.getServiceUUID())
                .build();
        mAdvScanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(accSensor != null){
            sensorManager.registerListener(sensorEventListener, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        linAccSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if(linAccSensor != null){
            sensorManager.registerListener(sensorEventListener, linAccSensor, SensorManager.SENSOR_DELAY_FASTEST);
            tOld = System.currentTimeMillis();
        }
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if(linAccSensor != null){
            sensorManager.registerListener(sensorEventListener, magSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if(gyroSensor != null){
            sensorManager.registerListener(sensorEventListener, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mGattServer == null) {
            ensureBleFeaturesAvailable();
            return;
        }
        // Add a service for a total of three services (Generic Attribute and Generic Access
        // are present by default).
        mGattServer.addService(mBluetoothGattService);

        if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            mAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvScanResponse, mAdvCallback);
        }

    }

    @Override
    public void sendNotificationToDevices(BluetoothGattCharacteristic characteristic) {
        boolean indicate = (characteristic.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_INDICATE)
                == BluetoothGattCharacteristic.PROPERTY_INDICATE;
        for (BluetoothDevice device : mBluetoothDevices) {
            // true for indication (acknowledge) and false for notification (unacknowledge).
            mGattServer.notifyCharacteristicChanged(device, characteristic, indicate);
        }
    }

    public static BluetoothGattDescriptor getClientCharacteristicConfigurationDescriptor() {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        descriptor.setValue(new byte[]{0, 0});
        return descriptor;
    }

    public static BluetoothGattDescriptor getCharacteristicUserDescriptionDescriptor(String defaultValue) {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CHARACTERISTIC_USER_DESCRIPTION_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        try {
            descriptor.setValue(defaultValue.getBytes("UTF-8"));
        } finally {
            return descriptor;
        }
    }

    private void ensureBleFeaturesAvailable() {
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported");
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Make sure bluetooth is enabled.
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        }
    }
    private void disconnectFromDevices() {
        Log.d(TAG, "Disconnecting devices...");
        for (BluetoothDevice device : mBluetoothManager.getConnectedDevices(
                BluetoothGattServer.GATT)) {
            Log.d(TAG, "Devices: " + device.getAddress() + " " + device.getName());
            mGattServer.cancelConnection(device);
        }
    }


}
