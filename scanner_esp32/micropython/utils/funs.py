def bytearrayCopy(src, dest):
    s1 = len(src)
    s2 = len(dest)
    limit = s1 if s1 < s2 else s2
    i = 0
    while(i < limit):
        dest[i] = src[i] 
        i += 1
    return limit

def inc(i, size):
    i += 1
    if i >= size:
        i -= size
    return i

def dec(i, size):
    i -= 1
    if i < 0:
        i += size
    return i