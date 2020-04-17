from irqueue.funs import bytearrayCopy

# Pre allocated dynamic byte array
class PADbytearray:
    def __init__(self, maxlen = 31):
        self.maxlen = maxlen
        self.len = 0
        self.array = bytearray(self.maxlen)


    def set_bytearray(self, source):
        self.len = bytearrayCopy(source, self.array)


    def to_bytearray(self):
        return self.array[:self.len]

    
    def copyTo(self, dest):
        dest.set_bytearray(self.array)
        dest.len = min(dest.len, self.len)
        