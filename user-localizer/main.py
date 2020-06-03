from locationsharinglib import Service
from flask import Flask
import json
import time

def millis():
    return int(round(time.time() * 1000))

def getPersons():
    global service
    return list(service.get_all_people())

app = Flask(__name__)

http_server_port = 8086
time_window_ms = 500

cookies_file = './.google_maps_location_sharing.cookies.bmpprogetti_gmail_com'
google_email = 'bmpprogetti@gmail.com'
service = Service(cookies_file=cookies_file, authenticating_account=google_email)
persons = getPersons()
last_update_time = millis()


@app.route('/users/<user>', methods=['GET'])
def personDataRoute(user):
    global persons
    global last_update_time
    now = millis()
    if (now - last_update_time) > time_window_ms:
        persons = getPersons()
        last_update_time = millis()

    result = 'null'
    for person in persons:
        if person.full_name.lower().replace(' ', '') == user:
            obj = {
                'id' : person.id,
                'picture_url' : person.picture_url,
                'full_name' : person.full_name,
                'nickname' : person.nickname,
                'latitude' : person.latitude,
                'longitude' : person.longitude,
                'timestamp' : person.timestamp,
                'accuracy' : person.accuracy,
                'address' : person.address,
                'country_code' : person.country_code,
                'charging' : person.charging,
                'battery_level' : person.battery_level
            }
            result = json.dumps(obj, ensure_ascii=False).encode('utf8')
            break
    return result


if __name__ == '__main__':
    app.run(port = http_server_port)