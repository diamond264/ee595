/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.ee595android;

import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.UUID;


public class YoutubeFragment extends ServiceFragment {

  private static final UUID BATTERY_SERVICE_UUID = UUID
      .fromString("0000180F-0000-1000-8000-00805f9b34fb");

  private static final UUID BATTERY_LEVEL_UUID = UUID
      .fromString("00002A19-0000-1000-8000-00805f9b34fb");
  private static final UUID LINK_UUID = UUID
          .fromString("00002A20-0000-1000-8000-00805f9b34fb");
  private static final int INITIAL_BATTERY_LEVEL = 50;
  private static final int BATTERY_LEVEL_MAX = 100;
  private static final String BATTERY_LEVEL_DESCRIPTION = "The current charge level of a " +
      "battery. 100% represents fully charged while 0% represents fully discharged.";

  private ServiceFragmentDelegate mDelegate;


  // GATT
  private BluetoothGattService mBatteryService;
  private BluetoothGattCharacteristic mBatteryLevelCharacteristic;
  private BluetoothGattCharacteristic mLinkCharacteristic;

  public YoutubeFragment() {
    mBatteryLevelCharacteristic =
        new BluetoothGattCharacteristic(BATTERY_LEVEL_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ);

    mBatteryLevelCharacteristic.addDescriptor(
        YoutubeService.getClientCharacteristicConfigurationDescriptor());

    mBatteryLevelCharacteristic.addDescriptor(
        YoutubeService.getCharacteristicUserDescriptionDescriptor(BATTERY_LEVEL_DESCRIPTION));

    mBatteryService = new BluetoothGattService(BATTERY_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mBatteryService.addCharacteristic(mBatteryLevelCharacteristic);

    mLinkCharacteristic =
            new BluetoothGattCharacteristic(LINK_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);

    mBatteryService.addCharacteristic(mLinkCharacteristic);


  }

  // Lifecycle callbacks

  public BluetoothGattService getBluetoothGattService() {
    return mBatteryService;
  }

  @Override
  public ParcelUuid getServiceUUID() {
    return new ParcelUuid(BATTERY_SERVICE_UUID);
  }


  @Override
  public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
    characteristic.setValue(value);
    return value.length;
  }

  @Override
  public void notificationsEnabled(BluetoothGattCharacteristic characteristic, boolean indicate) {
    if (characteristic.getUuid() != BATTERY_LEVEL_UUID) {
      return;
    }
    if (indicate) {
      return;
    }
  }

  @Override
  public void notificationsDisabled(BluetoothGattCharacteristic characteristic) {
    if (characteristic.getUuid() != BATTERY_LEVEL_UUID) {
      return;
    }
  }

}
