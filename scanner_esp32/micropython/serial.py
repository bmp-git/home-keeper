import uasyncio as asyncio
from machine import UART
import ujson
from config import RF433_PUBLISH_TOPIC, WIFI_PUBLISH_TOPIC
import mqtt

uart = UART(2, 115200)

async def serial_daemon_start(loop):
    global uart
    sreader = asyncio.StreamReader(uart)
    while True:
        res = await sreader.readline()
        print("[Serial] Received: ", res)
        try:
            json = ujson.loads(res)
            data = ujson.dumps(json['data'])
            t = json['type']
            if t == "wifi":
                await mqtt.mqtt_publish(WIFI_PUBLISH_TOPIC, data)
            elif t == "433":
                await mqtt.mqtt_publish(RF433_PUBLISH_TOPIC, data)
        except Exception as e:
            print("ERROR", e)
            pass