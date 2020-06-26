from irqueue.funs import bytearrayCopy
class RF433RXRecord:
    def __init__(self):
        self.code = int(0)
        self.pulselength = int(0)
        self.proto = int(0)
    
    # not using variable arguments to avoid an array allocation
    def setValues(self, v1, v2, v3, v4, v5):
        self.code = v1
        self.pulselength = v2
        self.proto = v3

    def copyTo(self, dest):
        dest.code = self.code
        dest.pulselength = self.pulselength
        dest.proto = self.proto