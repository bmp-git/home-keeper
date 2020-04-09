from umqtt.asynclient import MQTTClient
import uasyncio as asyncio
from config import MQTT_CLIENT_ID, MQTT_SERVER_ADDRESS, MQTT_PORT, MQTT_USER, MQTT_PASSWORD, MQTT_KEEPALIVE, MQTT_PUBLISH_QUEUE_SIZE
import wifi
import gc

mqtt_client = MQTTClient(client_id=MQTT_CLIENT_ID, server=MQTT_SERVER_ADDRESS, port=MQTT_PORT, user=MQTT_USER, password=MQTT_PASSWORD, keepalive=MQTT_KEEPALIVE)

message_queue = []

async def mqtt_daemon_start(loop):
    loop.create_task(mqtt_reconnector())
    loop.create_task(mqtt_publisher())

async def mqtt_reconnector():
    global mqtt_client
    while True:
        await wifi.await_wifi_connected()
        if not mqtt_client.isConnected():
            await mqtt_client.connect(clean_session=True)         
        await asyncio.sleep(5)

async def mqtt_publisher():
    global message_queue
    while True:
        if len(message_queue):
            topic, message = message_queue.pop(0)
            await mqtt_client.publish(topic=topic, msg=message, retain=False)
        await asyncio.sleep_ms(50)

def mqtt_publish(topic, message):
    global message_queue
    if len(message_queue) >= MQTT_PUBLISH_QUEUE_SIZE:
        print("[Mqtt] Warning! Publish queue is full! Deleting some.")
        message_queue = message_queue[-int(MQTT_PUBLISH_QUEUE_SIZE / 2):]
    message_queue.append((topic, message))