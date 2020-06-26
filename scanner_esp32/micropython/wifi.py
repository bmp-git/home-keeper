import network
import uasyncio as asyncio
from config import WIFI_SSID, WIFI_PASSWORD

wlan = network.WLAN(network.STA_IF)
wlan.active(True)

async def wifi_daemon_start(loop):    
    old_wlan_isconnected = wlan.isconnected()
    current_wlan_isconnected = old_wlan_isconnected
    while True:
        current_wlan_isconnected = wlan.isconnected()
        if current_wlan_isconnected and old_wlan_isconnected:
            pass
        elif current_wlan_isconnected and not old_wlan_isconnected:
            print('[WiFi] Connected! Network config:', wlan.ifconfig())
        elif not current_wlan_isconnected and old_wlan_isconnected:
            print('[WiFi] Connection lost!')
            print('[WiFi] Reconnecting to network...')
            wlan.connect(WIFI_SSID, WIFI_PASSWORD)
        elif not current_wlan_isconnected and not old_wlan_isconnected:
            print('[WiFi] Connecting to network...')
            wlan.connect(WIFI_SSID, WIFI_PASSWORD)
            
        old_wlan_isconnected = current_wlan_isconnected
        await asyncio.sleep(5)

async def await_wifi_connected():
    while not wlan.isconnected():
        await asyncio.sleep(1)