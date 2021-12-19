#!/usr/bin/env python3
import requests
import json
import time
import asyncio
from bleak import BleakScanner, BleakClient
import struct


import numpy as np

from google.oauth2 import service_account
from google.auth.transport.requests import AuthorizedSession, Request

# database metadata
firebase_db = 'https://ee595-c30a7-default-rtdb.asia-southeast1.firebasedatabase.app/'
link_table = 'linkdata'
sdata_table = 'sdata'
J = '.json'

# Define the required scopes
scopes = [
  "https://www.googleapis.com/auth/userinfo.email",
  "https://www.googleapis.com/auth/firebase.database"
]

sensor_data = [0, 0, 0, 0, 0, 0]
phone_data = (0, 0, 0, '', 0)
glob_summary = ""

def get_data_from_sensor():
    global sensor_data
    return sensor_data

def get_data_from_phone():
    global phone_data
    return phone_data

def get_data_from_web(authed_session):
    response = authed_session.get(firebase_db+link_table+J)
    if response.status_code != 200: print("GET failed")
    resp_data = response.json()

    return resp_data['url']

def gen_summary_from_data(sensor_data, phone_data, web_link):
    # sensor, phone default orientation / location -> (0,0,0) / (0,0,0)
    # monitor default location -> (0, -50, 30)
    monitor_loc_x = 0
    monitor_loc_y = -40
    # monitor_loc_z = 30

    sensor_loc_x = sensor_data[1]
    sensor_loc_y = sensor_data[0]
    # sensor_loc_z = sensor_data[2]

    sensor_ori_roll = sensor_data[5]
    # sensor_ori_yaw = sensor_data[4]
    sensor_ori_pitch = sensor_data[4]

    ### set phone location manually
    phone_loc_x = 40 # phone_data[0]
    phone_loc_y = 0 # phone_data[1]
    # phone_loc_z = phone_data[2]

    video_link = phone_data[3].split("?t=")[0]
    video_link += "?t="+str(int(phone_data[4]))
    # link = video_link

    # if video_link != "" and len(video_link.split('?t=')) > 1:
    #     video_time = int(video_link.split('?t=')[1])
    # else: video_time = 10000
    # web_time = int(web_link.split('?t=')[1])
    # if video_time < web_time: link = web_link

    sensor_pos = np.array([sensor_loc_x, sensor_loc_y])
    phone_pos = np.array([phone_loc_x, phone_loc_y])
    print(sensor_pos, phone_pos)
    dis_sensor_phone = np.linalg.norm(sensor_pos - phone_pos)
    xdiff_sensor_phone = np.abs(sensor_loc_x - phone_loc_x)
    degree_sensor_phone = np.arccos(xdiff_sensor_phone / dis_sensor_phone)/np.pi*180
    if sensor_loc_x > phone_loc_x and sensor_loc_y > phone_loc_y:
        degree_sensor_phone = 90 - degree_sensor_phone
    if sensor_loc_x > phone_loc_x and sensor_loc_y < phone_loc_y:
        degree_sensor_phone = 90 + degree_sensor_phone
    if sensor_loc_x < phone_loc_x and sensor_loc_y > phone_loc_y:
        degree_sensor_phone = 270 + degree_sensor_phone
    if sensor_loc_x < phone_loc_x and sensor_loc_y < phone_loc_y:
        degree_sensor_phone = 270 - degree_sensor_phone

    pitch_to_phone = np.abs(degree_sensor_phone - sensor_ori_pitch)
    if pitch_to_phone > 180: pitch_to_phone = 360 - pitch_to_phone

    sensor_pos = np.array([sensor_loc_x, sensor_loc_y])
    monitor_pos = np.array([monitor_loc_x, monitor_loc_y])
    dis_sensor_monitor = np.linalg.norm(sensor_pos - monitor_pos)
    xdiff_sensor_monitor = np.abs(sensor_loc_x - monitor_loc_x)
    degree_sensor_monitor = np.arccos(xdiff_sensor_monitor / dis_sensor_monitor)/np.pi*180
    if sensor_loc_x > monitor_loc_x and sensor_loc_y > monitor_loc_y:
        degree_sensor_monitor = 90 - degree_sensor_monitor
    if sensor_loc_x > monitor_loc_x and sensor_loc_y < monitor_loc_y:
        degree_sensor_monitor = 90 + degree_sensor_monitor
    if sensor_loc_x < monitor_loc_x and sensor_loc_y > monitor_loc_y:
        degree_sensor_monitor = 270 + degree_sensor_monitor
    if sensor_loc_x < monitor_loc_x and sensor_loc_y < monitor_loc_y:
        degree_sensor_monitor = 270 - degree_sensor_monitor

    pitch_to_monitor = np.abs(degree_sensor_monitor - sensor_ori_pitch)
    if pitch_to_monitor > 180: pitch_to_monitor = 360 - pitch_to_monitor

    direction = 'up'
    if sensor_ori_roll > 65: direction = 'left'
    if sensor_ori_roll < -65: direction = 'right'

    summary = {}
    # sensor_ori_roll < -70:
    if pitch_to_monitor >= pitch_to_phone:
        summary = {
            "attention": "phone",
            "distance": dis_sensor_phone,
            "direction": direction,
            "last_link": web_link,
            "last_link_from_phone": video_link,
            "timestamp": {
                ".sv": "timestamp"
            }
        }
    else:
        summary = {
            "attention": "monitor",
            "distance": dis_sensor_monitor,
            "direction": direction,
            "last_link": web_link,
            "last_link_from_phone": video_link,
            "timestamp": {
                ".sv": "timestamp"
            }
        }

    return json.dumps(summary)

def send_data_to_phone(summary):
    pass

def gen_sample_summary():
    sdata = {
        "attention": "monitor",
        "distance": 3,
        "direction": "up",
        "last_link": "",
        "timestamp": {
            ".sv": "timestamp"
        }
    }

    return json.dumps(sdata)

def get_oldest_key(dict_data):
    if dict_data == None: return None
    keys = list(dict_data.keys())
    min_key = keys[0]
    min_timestamp = dict_data[min_key]["timestamp"]
    for key in keys:
        if dict_data[key]["timestamp"] < min_timestamp:
            min_key = key
            min_timestamp = dict_data[key]["timestamp"]
    
    return min_key


async def main_loop(time_interval=1, max_datasize=10):
    # Authenticate a credential with the service account
    credentials = service_account.Credentials.from_service_account_file(
        "api_key.json", scopes=scopes)
    request = Request()
    credentials.refresh(request)
    access_token = credentials.token
    print(f'access_token\n{access_token}')
    with open("./chrome_extension/js/access.token", "w") as f: f.write(access_token)

    # Use the credentials object to authenticate a Requests session.
    authed_session = AuthorizedSession(credentials)

    while True:
        sensor_data_ = get_data_from_sensor()
        phone_data_ = get_data_from_phone()
        web_link = get_data_from_web(authed_session) # get latest link from the web
        # print(web_link)
        print("phone data")
        print(phone_data_)
        print("sensor data")
        print(sensor_data_)
        print("")

        summary = gen_summary_from_data(sensor_data, phone_data, web_link)
        global glob_summary
        glob_summary = summary
        print(summary)
        print("")
        # summary = gen_sample_summary()

        response = authed_session.get(firebase_db+sdata_table+J)
        if response.status_code != 200: print("GET failed")
        
        resp_data = response.json()
        if resp_data == None or len(list(resp_data.keys())) < max_datasize:
            response = authed_session.post(
                firebase_db+sdata_table+J, data=summary
            )
            if response.status_code != 200: print("POST failed")
        else:
            oldest = get_oldest_key(resp_data)
            response = authed_session.put(
                firebase_db+sdata_table+'/'+oldest+J, data=summary
            )
            if response.status_code != 200: print("PUT failed")

        send_data_to_phone(summary)
        
        # print(resp_data)
        await asyncio.sleep(time_interval)


loop = asyncio.get_event_loop()

# Scan all peripherals
# loop.run_until_complete(discover())

# After scanning, change the address to the appropriate value
# The Arduino device will be displayed with locally assigned name
# address = "EFABA2D7-2ED9-4888-B387-7B2370184FF2" # set your address here

# Standard BLE/Bluetooth UUID is 128-bit long
# Among them, below formats are specified/reserved by the standard 16-bit UUID assignments
uuid_extend = lambda uuid_16: "0000" + uuid_16 + "-0000-1000-8000-00805f9b34fb"

# Simple print callback for the "notify" event of the "Intermediate Temperature Characteristic"
def phone_orientation_callback(sender, data):
    data = data.decode().split(';')
    numbers = data[0].strip()[1:-1].split(',')
    numbers = [float(i.strip()) for i in numbers]

    uri = data[1].split(' ')
    global phone_data
    phone_data = (numbers[3], numbers[4], numbers[5], uri[0], float(uri[1]) if uri[1] and uri[1] != "null" else -1)
    # print("Received data in bytearray: {}".format(data))
    # print("Received data in float: {}".format(struct.unpack('f', data[1:5])[0])) # Field struct is 5-byte long, where the first byte is the "flag" field.


def arduino_orientation_callback(sender, data):
    yaw = struct.unpack('f', data[1:5])[0]
    roll = struct.unpack('f', data[5:9])[0]
    pitch = struct.unpack('f', data[9:13])[0]
    #print('arduino orientation : ', end = "")
    #print(yaw, roll, pitch)

def arduino_location_callback(sender, data):
    x = struct.unpack('f', data[1:5])[0]
    y = struct.unpack('f', data[5:9])[0]
    z = struct.unpack('f', data[9:13])[0]
    pitch = struct.unpack('f', data[13:17])[0]
    yaw = struct.unpack('f', data[17:21])[0]
    roll = struct.unpack('f', data[21:25])[0]

    global sensor_data
    sensor_data = [x, y, z, pitch, yaw, roll]

    #print('arduino location : ', end = "")
    #print(x, y, z)
# Main asynchronous function, with target address
async def run():
    client_device = None
    found_flag = False
    while(not found_flag):
        devices = await BleakScanner.discover()
        for d in devices:
            if "EE595B_Phone" in d.name:
                client_device = d
                print("Found!!  ")
                found_flag = True
                break

    print("Trying to connect...")
    async with BleakClient(client_device, use_cached=False, timeout=50.0) as client:
        print("Connected to {}".format(client_device))
        # device_name = await client.read_gatt_char(uuid_extend("2A00"))
        # appearance = await client.read_gatt_char(uuid_extend("2A01"))
        # print("Device name: {}".format(device_name))
        # print("Appearance: {}".format(appearance))

        # Set the "notify" event handler
        # Handler will be called when the "peripheral"(server) "notify" the data
        # await client.start_notify(uuid_extend("2A18"), phone_html_callback)
        await client.start_notify(uuid_extend("2A19"), phone_orientation_callback)

        # Make the main function loop forever to continuously monitor the data
        while client.is_connected:
            global glob_summary
            await client.write_gatt_char(uuid_extend("2A20"),bytes(glob_summary, 'utf-8'))
            await asyncio.sleep(1)

async def runArduino():
    client_device = None
    found_flag = False
    while(not found_flag):
        devices = await BleakScanner.discover()
        for d in devices:
            if "EE595B_Arduino" in d.name:
                client_device = d
                print("Found!!  ")
                found_flag = True
                break

    print("Trying to connect...")
    async with BleakClient(client_device, use_cached=False, timeout=30.0) as client:
        print("Connected to {}".format(client_device))

        # For tutorial purpose -- list all services installed
        # Should print out "Generic Access Profile", "Generic Attribute Profile", and the user-installed standard service "Health Thermometer"

        # Set the "notify" event handler
        # Handler will be called when the "peripheral"(server) "notify" the data
        # await client.start_notify(uuid_extend("2A1E"), arduino_orientation_callback)
        await client.start_notify(uuid_extend("2A1C"), arduino_location_callback)

        # Make the main function loop forever to continuously monitor the data
        while client.is_connected:
            await asyncio.sleep(1)


#tasks = asyncio.gather(runArduino())
tasks = asyncio.gather(run(), runArduino(), main_loop())
loop.run_until_complete(tasks)
