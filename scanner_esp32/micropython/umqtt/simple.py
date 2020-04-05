import usocket as socket
import ustruct as struct
import uasyncio as asyncio
import utime
from ubinascii import hexlify

class MQTTException(Exception):
    pass

class MQTTClient:

    def __init__(self, client_id, server, port=0, user=None, password=None, keepalive=0,
                 ssl=False, ssl_params={}):
        if port == 0:
            port = 8883 if ssl else 1883
        self.client_id = client_id
        self.sock = None
        self.server = server
        self.port = port
        self.ssl = ssl
        self.ssl_params = ssl_params
        self.pid = 0
        self.cb = None
        self.user = user
        self.pswd = password
        self.keepalive = keepalive
        self.socket_timeout = 3000
        self.lw_topic = None
        self.lw_msg = None
        self.lw_qos = 0
        self.lw_retain = False

    async def _write(self, message):
        start = utime.ticks_ms()
        while len(message):
            not_sent = self.sock.send(message)
            message = message[not_sent:]
            await asyncio.sleep_ms(100)
            if utime.ticks_diff(utime.ticks_ms(), start) > self.socket_timeout:
                raise OSError
    
    async def _read(self, num_bytes):
        start = utime.ticks_ms()
        res = bytearray()
        while num_bytes:
            rec = self.sock.read(num_bytes)
            if rec is not None:
                num_bytes -= len(rec)
                res += rec
            if num_bytes:
                await asyncio.sleep_ms(100)
                if utime.ticks_diff(utime.ticks_ms(), start) > self.socket_timeout:
                    raise OSError
        return res

    async def _send_str(self, s):
        await self._write(struct.pack("!H", len(s)))
        await self._write(s)

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
        self.sock = socket.socket()
        addr = socket.getaddrinfo(self.server, self.port)[0][-1]
        self.sock.connect(addr)
        self.sock.setblocking(False)
        if self.ssl:
            import ussl
            self.sock = ussl.wrap_socket(self.sock, **self.ssl_params)
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

        await self._write(premsg[:i + 2])
        await self._write(msg)
        await self._send_str(self.client_id)
        if self.lw_topic:
            await self._send_str(self.lw_topic)
            await self._send_str(self.lw_msg)
        if self.user is not None:
            await self._send_str(self.user)
            await self._send_str(self.pswd)
        resp = await self._read(4)
        assert resp[0] == 0x20 and resp[1] == 0x02
        if resp[3] != 0:
            raise MQTTException(resp[3])
        return resp[2] & 1

    async def disconnect(self):
        await self._write(b"\xe0\0")
        self.sock.close()

    async def ping(self):
        await self._write(b"\xc0\0")

    async def publish(self, topic, msg, retain=False, qos=0):
        if self.sock is None:
            raise OSError
        pkt = bytearray(b"\x30\0\0\0")
        pkt[0] |= qos << 1 | retain
        sz = 2 + len(topic) + len(msg)
        if qos > 0:
            sz += 2
        assert sz < 2097152
        i = 1
        while sz > 0x7f:
            pkt[i] = (sz & 0x7f) | 0x80
            sz >>= 7
            i += 1
        pkt[i] = sz

        await self._write(pkt[:i + 1])
        await self._send_str(topic)
        await self._write(msg)

    async def subscribe(self, topic, qos=0):
        assert self.cb is not None, "Subscribe callback is not set"
        pkt = bytearray(b"\x82\0\0\0")
        self.pid += 1
        struct.pack_into("!BH", pkt, 1, 2 + 2 + len(topic) + 1, self.pid)
        #print(hex(len(pkt)), hexlify(pkt, ":"))
        await self._write(pkt)
        await self._send_str(topic)
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
        self.sock.setblocking(True)
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
        topic = self.sock.read(topic_len)
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
        self.sock.setblocking(False)
        res = await self.wait_msg()
        return res
