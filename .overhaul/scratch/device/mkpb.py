# Writes a DataStore Preferences protobuf holding two string keys, so isAuthenticated reads true.
def varint(n):
    out = bytearray()
    while True:
        b = n & 0x7F; n >>= 7
        if n: out.append(b | 0x80)
        else: out.append(b); return bytes(out)
def ld(field, payload):
    return varint((field << 3) | 2) + varint(len(payload)) + payload
def entry(k, v):
    value = ld(5, v.encode())
    return ld(1, ld(1, k.encode()) + ld(2, value))
data = entry("encrypted_access_token", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") + entry("encrypted_refresh_token", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
open("/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/session.preferences_pb", "wb").write(data)
print(len(data))
