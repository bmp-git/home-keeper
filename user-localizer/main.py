from locationsharinglib import Service
from flask import Flask
import json

app = Flask(__name__)

http_server_port = 8086

cookies_file = './.google_maps_location_sharing.cookies.bmpprogetti_gmail_com'
google_email = 'bmpprogetti@gmail.com'
service = Service(cookies_file=cookies_file, authenticating_account=google_email)
persons = service.get_all_people()


@app.route('/users/<user>', methods=['GET'])
def personDataRoute(user):
    global persons
    persons = service.get_all_people()
    result = '{}'
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