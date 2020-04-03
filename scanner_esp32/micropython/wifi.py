import network

def do_connect():    
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    if not wlan.isconnected():
        print('Connecting to WiFi network...')
        wlan.connect('Home-Keeper', '8CUAgjwyuaJu')
        while not wlan.isconnected():
            pass
    print('WiFi connected! Network config:', wlan.ifconfig())