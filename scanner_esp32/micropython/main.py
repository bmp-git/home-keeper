import uasyncio as asyncio
import wifi
import ble
import mqtt
import rf433

loop = asyncio.get_event_loop()
loop.create_task(wifi.wifi_daemon_start())
loop.create_task(mqtt.mqtt_daemon_start())
loop.create_task(ble.ble_scan_start())
loop.run_forever()


#ble.ble_scan_start()
#rf433.start_receiving(gpio=27)
