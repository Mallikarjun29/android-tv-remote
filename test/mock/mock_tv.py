"""Mock Android TV for protocol tests. Uses the independent androidtvremote2 protobuf definitions
(not our Java encoder) to decode what the app sends and to build what a TV sends."""
import asyncio, hashlib, json, os, ssl, sys, time
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID
import datetime
from google.protobuf.internal.decoder import _DecodeVarint
from google.protobuf.internal.encoder import _VarintBytes
from androidtvremote2.polo_pb2 import OuterMessage, Options
from androidtvremote2.remotemessage_pb2 import RemoteMessage

D = sys.argv[1]
PP, RP = (6467, 6466) if len(sys.argv) > 2 and sys.argv[2] == "real" else (16467, 16466)
LOG = open(os.path.join(D, "tv_log.jsonl"), "a", buffering=1)
def log(**k): LOG.write(json.dumps(k) + "\n")

key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "MockTV")])
now = datetime.datetime.utcnow()
cert = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
        .serial_number(x509.random_serial_number()).not_valid_before(now - datetime.timedelta(days=1))
        .not_valid_after(now + datetime.timedelta(days=365)).sign(key, hashes.SHA256()))
certf, keyf = os.path.join(D, "tv_cert.pem"), os.path.join(D, "tv_key.pem")
open(certf, "wb").write(cert.public_bytes(serialization.Encoding.PEM))
open(keyf, "wb").write(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
client_cert = x509.load_der_x509_certificate(open(os.path.join(D, "client_cert.der"), "rb").read())
client_pem = client_cert.public_bytes(serialization.Encoding.PEM).decode()

def ctx():
    c = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    c.load_cert_chain(certf, keyf)
    c.verify_mode = ssl.CERT_REQUIRED
    c.load_verify_locations(cadata=client_pem)
    return c

def mn(c): n = c.public_key().public_numbers(); return n.n, n.e
TAIL = "7F3A"
def secret():
    cn, ce = mn(client_cert); sn, se = mn(cert)
    h = hashlib.sha256()
    h.update(bytes.fromhex(f"{cn:X}")); h.update(bytes.fromhex(f"0{ce:X}"))
    h.update(bytes.fromhex(f"{sn:X}")); h.update(bytes.fromhex(f"0{se:X}"))
    h.update(bytes.fromhex(TAIL))
    return h.digest()
CODE = f"{secret()[0]:02X}{TAIL}"
open(os.path.join(D, "tv_code.txt"), "w").write(CODE)

async def read_frame(r):
    n = 0; shift = 0
    while True:
        b = (await r.readexactly(1))[0]
        n |= (b & 0x7F) << shift
        if not b & 0x80: break
        shift += 7
    return await r.readexactly(n)
def frame(msg): b = msg.SerializeToString(); return _VarintBytes(len(b)) + b

PAIRED = False

async def pairing(r, w):
    global PAIRED
    log(ev="pair_conn")
    try:
        while True:
            m = OuterMessage(); m.ParseFromString(await read_frame(r))
            out = OuterMessage(); out.protocol_version = 2; out.status = 200
            if m.protocol_version != 2 or m.status != 200: log(ev="bad_hdr", pv=m.protocol_version, st=m.status)
            if m.HasField("pairing_request"):
                log(ev="pairing_request", service=m.pairing_request.service_name, client=m.pairing_request.client_name)
                out.pairing_request_ack.server_name = "Mock TV"
            elif m.HasField("options"):
                e = m.options.input_encodings[0]
                log(ev="options", enc=e.type, sym=e.symbol_length, role=m.options.preferred_role)
                e2 = out.options.input_encodings.add(); e2.type = 3; e2.symbol_length = 6
                out.options.preferred_role = Options.RoleType.ROLE_TYPE_OUTPUT
            elif m.HasField("configuration"):
                log(ev="configuration", enc=m.configuration.encoding.type, sym=m.configuration.encoding.symbol_length, role=m.configuration.client_role)
                out.configuration_ack.SetInParent()
            elif m.HasField("secret"):
                ok = m.secret.secret == secret()
                log(ev="secret", ok=ok)
                if ok: out.secret_ack.secret = m.secret.secret; PAIRED = True
                else: out.status = 402
            else:
                log(ev="unknown_pair_msg"); continue
            w.write(frame(out)); await w.drain()
            if out.status != 200: break
    except (asyncio.IncompleteReadError, ConnectionError, ssl.SSLError): pass
    finally: w.close()

async def remote(r, w):
    if not PAIRED:
        log(ev="remote_rejected_unpaired"); w.close(); return
    log(ev="remote_conn")
    try:
        m = RemoteMessage()
        m.remote_configure.code1 = 622; m.remote_configure.device_info.model = "MockTV"; m.remote_configure.device_info.vendor = "Mock"
        m.remote_configure.device_info.unknown1 = 1; m.remote_configure.device_info.unknown2 = "1"
        m.remote_configure.device_info.package_name = "androidtv-remote"; m.remote_configure.device_info.app_version = "1.0"
        w.write(frame(m)); await w.drain()
        got = RemoteMessage(); got.ParseFromString(await read_frame(r))
        log(ev="client_configure", code1=got.remote_configure.code1, pkg=got.remote_configure.device_info.package_name, has=got.HasField("remote_configure"))
        m = RemoteMessage(); m.remote_set_active.active = 622; w.write(frame(m)); await w.drain()
        got = RemoteMessage(); got.ParseFromString(await read_frame(r))
        log(ev="client_active", active=got.remote_set_active.active, has=got.HasField("remote_set_active"))
        m = RemoteMessage(); v = m.remote_set_volume_level; v.unknown1 = 1; v.unknown2 = 1; v.player_model = "mock"; v.volume_max = 100; v.volume_level = 18; v.volume_muted = False
        w.write(frame(m)); await w.drain()
        m = RemoteMessage(); m.remote_start.started = True; w.write(frame(m)); await w.drain()
        m = RemoteMessage(); m.remote_ping_request.val1 = 77; m.remote_ping_request.val2 = 5; w.write(frame(m)); await w.drain()
        async def later():
            await asyncio.sleep(0.6)  # app start: auto focus on its search box (app must NOT jump to keyboard)
            x = RemoteMessage(); x.remote_ime_key_inject.app_info.app_package = "com.netflix.ninja"; x.remote_ime_key_inject.text_field_status.value = ""
            w.write(frame(x)); await w.drain()
            x = RemoteMessage(); x.remote_ime_batch_edit.ime_counter = 4; x.remote_ime_batch_edit.field_counter = 9
            w.write(frame(x)); await w.drain()
            await asyncio.sleep(10.8)  # later: user really taps a text field
            x = RemoteMessage(); x.remote_ime_key_inject.app_info.app_package = "com.netflix.ninja"; x.remote_ime_key_inject.text_field_status.value = ""
            w.write(frame(x)); await w.drain()
        ime_started = False
        if RP == 6466:  # real TVs can report a focused field right at connect; the app must not jump to the keyboard for it
            x = RemoteMessage(); x.remote_ime_key_inject.app_info.app_package = "com.google.android.tvlauncher"; w.write(frame(x)); await w.drain()
        while True:
            m = RemoteMessage(); m.ParseFromString(await read_frame(r))
            if m.HasField("remote_ping_response"): log(ev="pong", val1=m.remote_ping_response.val1)
            elif m.HasField("remote_key_inject"):
                log(ev="key", code=m.remote_key_inject.key_code, dir=m.remote_key_inject.direction)
                if RP == 6466 and m.remote_key_inject.key_code == 3:  # TV home screen focuses a text field after Home
                    async def home_ime():
                        await asyncio.sleep(0.3)
                        x = RemoteMessage(); x.remote_ime_key_inject.app_info.app_package = "com.sony.dtv.home"; w.write(frame(x)); await w.drain()
                    asyncio.ensure_future(home_ime())
            elif m.HasField("remote_app_link_launch_request"):
                log(ev="launch", link=m.remote_app_link_launch_request.app_link)
                if not ime_started: ime_started = True; asyncio.ensure_future(later())  # app opened -> its search box takes focus
            elif m.HasField("remote_ime_batch_edit"):
                b = m.remote_ime_batch_edit; e = b.edit_info[0]
                log(ev="text", value=e.text_field_status.value, start=e.text_field_status.start, end=e.text_field_status.end, insert=e.insert, ime=b.ime_counter, field=b.field_counter)
            else: log(ev="other", s=str(m)[:80])
    except (asyncio.IncompleteReadError, ConnectionError, ssl.SSLError): pass
    finally: w.close()

async def main():
    s1 = await asyncio.start_server(pairing, "127.0.0.1", PP, ssl=ctx())
    s2 = await asyncio.start_server(remote, "127.0.0.1", RP, ssl=ctx())
    open(os.path.join(D, "tv_ready"), "w").write("1")
    await asyncio.gather(s1.serve_forever(), s2.serve_forever())
asyncio.run(main())
