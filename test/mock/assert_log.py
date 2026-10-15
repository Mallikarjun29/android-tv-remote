import json, sys
ev = [json.loads(l) for l in open(sys.argv[1])]
def of(n): return [e for e in ev if e["ev"] == n]
fails = []
def check(name, cond):
    print(("PASS " if cond else "FAIL ") + name)
    if not cond: fails.append(name)
pr = of("pairing_request"); check("pairing_request has service atvremote + client name", len(pr) >= 3 and all(e["service"] == "atvremote" and e["client"] == "TV Remote" for e in pr))
check("options: hex encoding, 6 symbols, role INPUT", all(e["enc"] == 3 and e["sym"] == 6 and e["role"] == 1 for e in of("options")))
check("configuration: hex/6/INPUT", all(e["enc"] == 3 and e["sym"] == 6 and e["role"] == 1 for e in of("configuration")))
sec = of("secret"); check("TV received exactly one secret (the correct one); bad codes were stopped client-side before sending", [e["ok"] for e in sec] == [True])
check("protocol_version=2 / status=200 on every outer message", not of("bad_hdr"))
cc = of("client_configure")[0]; check("client configure: has field, features=622&ours, package", cc["has"] and cc["code1"] == (622 & 615) and cc["pkg"] == "atvremote")
ca = of("client_active")[0]; check("client set_active echoes features", ca["has"] and ca["active"] == (622 & 615))
check("pong echoes ping val1=77", any(e["val1"] == 77 for e in of("pong")))
keys = [(e["code"], e["dir"]) for e in of("key")]
exp = [(19,3),(20,1),(20,2),(24,3),(25,3),(164,3),(166,3),(167,3),(26,3),(7,3),(16,3)]
check("key injects decoded exactly: %s" % keys, keys == exp)
tx = of("text")
check("text 'abc' with start=end=len-1, insert=1, counters echoed from TV (4/9)", tx[0]["value"] == "abc" and tx[0]["start"] == 2 and tx[0]["end"] == 2 and tx[0]["insert"] == 1 and tx[0]["ime"] == 4 and tx[0]["field"] == 9)
check("unicode text survives", tx[1]["value"] == "é ü" and tx[1]["start"] == 2)
check("launch links decoded", [e["link"] for e in of("launch")] == ["market://launch?id=com.netflix.ninja", "https://www.youtube.com/watch?v=xyz"])
check("no unexpected messages", not of("other") and not of("unknown_pair_msg"))
print("ALL PASS" if not fails else "FAILURES: %s" % fails); sys.exit(1 if fails else 0)
