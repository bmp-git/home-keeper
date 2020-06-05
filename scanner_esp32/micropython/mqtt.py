from umqtt.asynclient import MQTTClient
import uasyncio as asyncio
from config import MQTT_CLIENT_ID, MQTT_SERVER_ADDRESS, MQTT_PORT, MQTT_USER, MQTT_PASSWORD, MQTT_KEEPALIVE
import wifi
import gc

mqtt_client = MQTTClient(client_id=MQTT_CLIENT_ID, server=MQTT_SERVER_ADDRESS, port=MQTT_PORT, user=MQTT_USER, password=MQTT_PASSWORD, keepalive=MQTT_KEEPALIVE)


async def mqtt_daemon_start(loop):
    loop.create_task(mqtt_reconnector())
    loop.create_task(mqtt_pinger())

async def mqtt_reconnector():
    global mqtt_client
    while True:
        await wifi.await_wifi_connected()
        if not mqtt_client.isConnected():
            await mqtt_client.connect(clean_session=True)         
        await asyncio.sleep(5)

async def mqtt_pinger():
    global mqtt_client
    while True:
        if mqtt_client.isConnected():
            await mqtt_client.ping()
        
        await asyncio.sleep(MQTT_KEEPALIVE // 2)


def mqtt_publish(topic, message):
    await mqtt_client.publish(topic=topic, msg=message, retain=False)