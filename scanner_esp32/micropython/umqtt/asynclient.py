import usocket as socket
import ustruct as struct
import uasyncio as asyncio
import utime
from ubinascii import hexlify

class MQTTException(Exception):
    pass

class MQTTClient:

    def __init__(self, client_id, server, port=1883, user=None, password=None, keepalive=0):
        self.client_id = client_id

        self.reader = None
        self.writer = None

        self.server = server
        self.port = port
        self.pid = 0
        self.cb = None
        self.user = user
        self.pswd = password
        self.keepalive = keepalive
        self.socket_timeout = 3
        self.lw_topic = None
        self.lw_msg = None
        self.lw_qos = 0
        self.lw_retain = False

        self.connected = False
        self.lock = asyncio.Lock()

    def isConnected(self):
        return self.connected


    async def _write(self, message):
        self.writer.write(message)
        await asyncio.wait_for(self.writer.drain(), self.socket_timeout)
    
    async def _read(self, num_bytes):
        res = await asyncio.wait_for(self.reader.read(num_bytes), self.socket_timeout)
        return res

    def _create_str(self, s):
        return bytearray(struct.pack("!H", len(s)) + s)

    async def _recv_len(self):
        n = 0
        sh = 0
        while 1:
            b = await self._read(1)[0]
            n |= (b & 0x7f) << sh
            if not b & 0x80:
                return n
            sh += 7

    def set_callback(self, f):
        self.cb = f

    def set_last_will(self, topic, msg, retain=False, qos=0):
        assert 0 <= qos <= 2
        assert topic
        self.lw_topic = topic
        self.lw_msg = msg
        self.lw_qos = qos
        self.lw_retain = retain

    async def connect(self, clean_session=True):
        try:
            if self.connected:
                print("[Mqtt] Already connected!")
                return
            print("[Mqtt] Connecting...")
            self.reader, self.writer = await asyncio.wait_for(asyncio.open_connection(self.server, self.port), self.socket_timeout * 3)
            packet = self._create_connect_packet(clean_session)
            async with self.lock:
                await self._write(packet)
                resp = await self._read(4)
            if not (resp[0] == 0x20 and resp[1] == 0x02):
                raise OSError
            if resp[3] != 0:
                raise OSError
            self.connected = True
            print("[Mqtt] Connected!")
        except:
            print("[Mqtt] Connection failed!")

    def _create_connect_packet(self, clean_session=True):
        premsg = bytearray(b"\x10\0\0\0\0\0")
        msg = bytearray(b"\x04MQTT\x04\x02\0\0")

        sz = 10 + 2 + len(self.client_id)
        msg[6] = clean_session << 1
        if self.user is not None:
            sz += 2 + len(self.user) + 2 + len(self.pswd)
            msg[6] |= 0xC0
        if self.keepalive:
            assert self.keepalive < 65536
            msg[7] |= self.keepalive >> 8
            msg[8] |= self.keepalive & 0x00FF
        if self.lw_topic:
            sz += 2 + len(self.lw_topic) + 2 + len(self.lw_msg)
            msg[6] |= 0x4 | (self.lw_qos & 0x1) << 3 | (self.lw_qos & 0x2) << 3
            msg[6] |= self.lw_retain << 5

        i = 1
        while sz > 0x7f:
            premsg[i] = (sz & 0x7f) | 0x80
            sz >>= 7
            i += 1
        premsg[i] = sz

        packet = premsg[:i + 2] + bytearray(msg) + self._create_str(self.client_id)
        if self.lw_topic:
            packet += self._create_str(self.lw_topic)
            packet += self._create_str(self.lw_msg)
        if self.user is not None:
            packet += self._create_str(self.user)
            packet += self._create_str(self.pswd)
        
        return packet
        

    async def disconnect(self):
        await self._write(b"\xe0\0")
        self.writer.close()
        await self.writer.wait_closed()

    async def ping(self):
        await self._write(b"\xc0\0")

    async def publish(self, topic, msg, retain=False):
        res = await self._send_packet(self._create_publish_packet(topic, msg, retain))
        if not res:
            print("[Mqtt] Publish failed! Message has been lost (QoS 0).")
        else:
            print("[Mqtt] Publish on " + topic + " succeeded!")
    
    async def _send_packet(self, packet):
        if not self.connected:
            return False
        try:
            async with self.lock:
                await self._write(packet)
            return True
        except:
            self.connected = False
            return False

    def _create_publish_packet(self, topic, msg, retain=False):
        pkt = bytearray(b"\x30\0\0\0")
        pkt[0] |= 0 << 1 | retain
        sz = 2 + len(topic) + len(msg)
        assert sz < 2097152
        i = 1
        while sz > 0x7f:
            pkt[i] = (sz & 0x7f) | 0x80
            sz >>= 7
            i += 1
        pkt[i] = sz
        return pkt[:i + 1] + self._create_str(topic) + bytearray(msg)

    async def subscribe(self, topic, qos=0):
        assert self.cb is not None, "Subscribe callback is not set"
        pkt = bytearray(b"\x82\0\0\0")
        self.pid += 1
        struct.pack_into("!BH", pkt, 1, 2 + 2 + len(topic) + 1, self.pid)
        #print(hex(len(pkt)), hexlify(pkt, ":"))
        await self._write(pkt)
        await self._write(self._create_str(topic))
        await self._write(qos.to_bytes(1, "little"))
        while 1:
            op = self.wait_msg()
            if op == 0x90:
                resp = await self._read(4)
                #print(resp)
                assert resp[1] == pkt[2] and resp[2] == pkt[3]
                if resp[3] == 0x80:
                    raise MQTTException(resp[3])
                return

    # Wait for a single incoming MQTT message and process it.
    # Subscribed messages are delivered to a callback previously
    # set by .set_callback() method. Other (internal) MQTT
    # messages processed internally.
    async def wait_msg(self):
        res = await self._read(1)
        if res is None:
            return None
        if res == b"":
            raise OSError(-1)
        if res == b"\xd0":  # PINGRESP
            sz = await self._read(1)[0]
            assert sz == 0
            return None
        op = res[0]
        if op & 0xf0 != 0x30:
            return op
        sz = await self._recv_len()
        topic_len = await self._read(2)
        topic_len = (topic_len[0] << 8) | topic_len[1]
        topic = await self._read(topic_len)
        sz -= topic_len + 2
        if op & 6:
            pid = await self._read(2)
            pid = pid[0] << 8 | pid[1]
            sz -= 2
        msg = await self._read(sz)
        self.cb(topic, msg)
        if op & 6 == 2:
            pkt = bytearray(b"\x40\x02\0\0")
            struct.pack_into("!H", pkt, 2, pid)
            await self._write(pkt)
        elif op & 6 == 4:
            assert 0

    # Checks whether a pending message from server is available.
    # If not, returns immediately with None. Otherwise, does
    # the same processing as wait_msg.
    async def check_msg(self):
        res = await self.wait_msg()
        return res
