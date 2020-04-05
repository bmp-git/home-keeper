import time
import uasyncio as asyncio
import mqtt
import ujson
from rpi_rf.rpi_rf import RFDevice
from config import RF433_PUBLISH_TOPIC


async def publish_task(rx_code, rx_pulselength, rx_proto):
    print("Rf433 received: ", rx_code, rx_pulselength, rx_proto)
    payload = {
        'code' : rx_code,
        'pulselength' : rx_pulselength,
        'proto' : rx_proto
    }
    await mqtt.mqtt_publish(RF433_PUBLISH_TOPIC, ujson.dumps(payload))

def on_data_received(rx_code, rx_pulselength, rx_proto):
    asyncio.get_event_loop().create_task(publish_task(rx_code, rx_pulselength, rx_proto))

async def rf433_start_receiving(gpio):
    rfdevice = RFDevice(gpio)
    rfdevice.enable_rx(on_data_received)
    print("Listening for codes on GPIO ", gpio)
    #rfdevice.cleanup()
