from utils.funs import bytearrayCopy
class BLEScanRecord:
    def __init__(self):
        self.addr = bytearray(6)
        self.adv_data = bytearray(31)
        self.rssi = int(0)
    
    def setValues(self, v1, v2, v3, v4, v5):
        bytearrayCopy(v1, self.addr)
        bytearrayCopy(v2, self.adv_data)
        self.rssi = v3

    def copyTo(self, dest):
        bytearrayCopy(self.addr, dest.addr)
        bytearrayCopy(self.adv_data, dest.adv_data)
        dest.rssi = self.rssi