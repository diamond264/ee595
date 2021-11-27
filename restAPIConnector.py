#!/usr/bin/env python3
import requests
import json
import time

from google.oauth2 import service_account
from google.auth.transport.requests import AuthorizedSession

# database metadata
firebase_db = 'https://ee595-c30a7-default-rtdb.asia-southeast1.firebasedatabase.app/'
sdata_table = 'sdata'
J = '.json'

# Define the required scopes
scopes = [
  "https://www.googleapis.com/auth/userinfo.email",
  "https://www.googleapis.com/auth/firebase.database"
]

def get_sdata_from_sensor():
    sdata = {
        "attention": "monitor",
        "distance": 3,
        "direction": "up",
        "timestamp": {
            ".sv": "timestamp"
        }
    }

    return json.dumps(sdata)


def get_oldest_key(dict_data):
    keys = list(dict_data.keys())
    min_key = keys[0]
    min_timestamp = dict_data[min_key]["timestamp"]
    for key in keys:
        if dict_data[key]["timestamp"] < min_timestamp:
            min_key = key
            min_timestamp = dict_data[key]["timestamp"]
    
    return min_key


def loop(time_interval=1, max_datasize=10):
    # Authenticate a credential with the service account
    credentials = service_account.Credentials.from_service_account_file(
        "api_key.json", scopes=scopes)

    # Use the credentials object to authenticate a Requests session.
    authed_session = AuthorizedSession(credentials)

    while True:
        sdata_new = get_sdata_from_sensor()
        response = authed_session.get(firebase_db+sdata_table+J)
        if response.status_code != 200: print("GET failed")
        
        resp_data = response.json()
        if resp_data == None or len(list(resp_data.keys())) < max_datasize:
            response = authed_session.post(
                firebase_db+sdata_table+J, data=sdata_new
            )
            if response.status_code != 200: print("POST failed")
        else:
            oldest = get_oldest_key(resp_data)
            response = authed_session.put(
                firebase_db+sdata_table+'/'+oldest+J, data=sdata_new
            )
            if response.status_code != 200: print("PUT failed")
        
        print(resp_data)
        time.sleep(time_interval)


if __name__ == "__main__":
    loop()