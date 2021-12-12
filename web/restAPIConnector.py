#!/usr/bin/env python3
import requests
import json
import time
import asyncio

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

def get_data_from_sensor():
    return -1

def get_data_from_phone():
    return -1

def get_data_from_web(authed_session):
    response = authed_session.get(firebase_db+link_table+J)
    if response.status_code != 200: print("GET failed")
    resp_data = response.json()
    
    return resp_data['url']

def gen_data_from_data(sensor_data, phone_data, web_link):
    # sensor, phone default orientation / location -> (0,0,0) / (0,0,0)
    # monitor default location -> (0, -50, 30)
    monitor_loc_x = 0
    monitor_loc_y = -50
    # monitor_loc_z = 30

    sensor_loc_x = sensor_data[0]
    sensor_loc_y = sensor_data[1]
    # sensor_loc_z = sensor_data[2]

    sensor_ori_pitch = sensor_data[3]
    # sensor_ori_yaw = sensor_data[4]
    sensor_ori_roll = sensor_data[5]

    phone_loc_x = phone_data[0]
    phone_loc_y = phone_data[1]
    # phone_loc_z = phone_data[2]

    video_link = phone_data[3]
    link = video_link

    video_time = int(video_link.split('?t=')[1])
    web_time = int(web_link.split('?t=')[1])
    if video_time < web_time: link = web_link

    sensor_pos = np.array([sensor_loc_x, sensor_loc_y])
    phone_pos = np.array([phone_loc_x, phone_loc_y])
    dis_sensor_phone = np.linalg.norm(sensor_pos, phone_pos)
    xdiff_sensor_phone = np.abs(sensor_loc_x - phone_loc_x)
    degree_sensor_phone = np.arccos(xdiff_sensor_phone / dis_sensor_phone)/np.pi*180
    if sensor_loc_x > phone_loc_x and sensor_loc_y < phone_loc_y:
        degree_sensor_phone = 90 - degree_sensor_phone
    if sensor_loc_x > phone_loc_x and sensor_loc_y > phone_loc_y:
        degree_sensor_phone = 90 + degree_sensor_phone
    if sensor_loc_x < phone_loc_x and sensor_loc_y < phone_loc_y:
        degree_sensor_phone = 270 + degree_sensor_phone
    if sensor_loc_x < phone_loc_x and sensor_loc_y > phone_loc_y:
        degree_sensor_phone = 270 - degree_sensor_phone

    pitch_to_phone = np.abs(degree_sensor_phone - sensor_ori_pitch)
    if pitch_to_phone > 180: pitch_to_phone = 360 - pitch_to_phone

    sensor_pos = np.array([sensor_loc_x, sensor_loc_y])
    monitor_pos = np.array([monitor_loc_x, monitor_loc_y])
    dis_sensor_monitor = np.linalg.norm(sensor_pos, monitor_pos)
    xdiff_sensor_monitor = np.abs(sensor_loc_x - monitor_loc_x)
    degree_sensor_monitor = np.arccos(xdiff_sensor_monitor / dis_sensor_monitor)/np.pi*180
    if sensor_loc_x > monitor_loc_x and sensor_loc_y < monitor_loc_y:
        degree_sensor_monitor = 90 - degree_sensor_monitor
    if sensor_loc_x > monitor_loc_x and sensor_loc_y > monitor_loc_y:
        degree_sensor_monitor = 90 + degree_sensor_monitor
    if sensor_loc_x < monitor_loc_x and sensor_loc_y < monitor_loc_y:
        degree_sensor_monitor = 270 + degree_sensor_monitor
    if sensor_loc_x < monitor_loc_x and sensor_loc_y > monitor_loc_y:
        degree_sensor_monitor = 270 - degree_sensor_monitor

    pitch_to_monitor = np.abs(degree_sensor_monitor - sensor_ori_pitch)
    if pitch_to_monitor > 180: pitch_to_monitor = 360 - pitch_to_monitor

    direction = 'up'
    if sensor_ori_roll > 45: direction = 'left'
    if sensor_ori_roll < -45: direction = 'right'

    summary = {}
    if pitch_to_monitor > pitch_to_phone:
        summary = {
            "attention": "phone",
            "distance": dis_sensor_phone,
            "direction": direction,
            "last_link": link,
            "timestamp": {
                ".sv": "timestamp"
            }
        }
    else:
        summary = {
            "attention": "monitor",
            "distance": dis_sensor_monitor,
            "direction": direction,
            "last_link": link,
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


async def loop(time_interval=1, max_datasize=10):
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
        sensor_data = get_data_from_sensor()
        phone_data = get_data_from_phone()
        web_link = get_data_from_web(authed_session) # get latest link from the web

        # summary = gen_summary_from_data(sensor_data, phone_data, web_link)
        summary = gen_sample_summary()

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
        
        print(resp_data)
        await asyncio.sleep(time_interval)


if __name__ == "__main__":
    asyncio.run(loop())
