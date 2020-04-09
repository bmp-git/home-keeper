import time
import uasyncio as asyncio
import mqtt
import ujson
import gc
from rpi_rf.rpi_rf import RFDevice
from config import RF433_PUBLISH_TOPIC


def on_data_received(rx_code, rx_pulselength, rx_proto):
    print("Rf433 received: ", rx_code, rx_pulselength, rx_proto)
    #payload = {
    #    'code' : rx_code,
    #    'pulselength' : rx_pulselength,
    #    'proto' : rx_proto
    #}
    #mqtt.mqtt_publish(RF433_PUBLISH_TOPIC, ujson.dumps(payload))
    #gc.collect()

async def rf433_start_receiving(gpio):
    rfdevice = RFDevice(gpio)
    rfdevice.enable_rx(on_data_received)
    print("Listening for codes on GPIO ", gpio)
    #rfdevice.cleanup()
