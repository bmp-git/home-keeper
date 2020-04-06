from umqtt.asynclient import MQTTClient
import uasyncio as asyncio
from config import MQTT_CLIENT_ID, MQTT_SERVER_ADDRESS, MQTT_PORT, MQTT_USER, MQTT_PASSWORD, MQTT_KEEPALIVE
import wifi
import gc

mqtt_client = MQTTClient(client_id=MQTT_CLIENT_ID, server=MQTT_SERVER_ADDRESS, port=MQTT_PORT, user=MQTT_USER, password=MQTT_PASSWORD, keepalive=MQTT_KEEPALIVE)
lock = asyncio.Lock()

async def mqtt_daemon_start():
    global mqtt_client
    global lock
    while True:
        await wifi.await_wifi_connected()
        async with lock:
            if not mqtt_client.isConnected():
                print("[Mqtt] Connecting...")
                await mqtt_client.connect(clean_session=True)                
        await asyncio.sleep(5)



async def mqtt_publish(topic, message):
    global mqtt_client
    global lock
    async with lock:
        await mqtt_client.publish(topic=topic, msg=message, retain=False)