from umqtt.simple import MQTTClient
import uasyncio as asyncio
from config import MQTT_CLIENT_ID, MQTT_SERVER_ADDRESS, MQTT_PORT, MQTT_USER, MQTT_PASSWORD, MQTT_KEEPALIVE
import wifi

mqtt_client = MQTTClient(client_id=MQTT_CLIENT_ID, server=MQTT_SERVER_ADDRESS, port=MQTT_PORT, user=MQTT_USER, password=MQTT_PASSWORD, keepalive=MQTT_KEEPALIVE)
connected = False

async def mqtt_daemon_start():
    global connected
    global mqtt_client
    while True:
        if not connected:
            try:
                print("[Mqtt] Connecting...")
                await wifi.await_wifi_connected()
                mqtt_client.connect(clean_session=True)
                print("[Mqtt] Connected!")
                connected = True
            except OSError as e:
                print("[Mqtt] Connection failed, retrying.")
        await asyncio.sleep(5)

    


def mqtt_publish(topic, message):
    global connected
    global mqtt_client
    try:
        mqtt_client.publish(topic=topic, msg=message, retain=False, qos=0)
        print("[Mqtt] Publish on " + topic + " succeeded!")
    except OSError as e:
        print("[Mqtt] Publish failed! Message has been lost (QoS 0).")
        connected = False