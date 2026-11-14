#!/usr/bin/env python3
"""Capture the Play Store screenshots on a running Android emulator.

    python3 play-store/tools/capture.py --serial emulator-5554 --device phone
    python3 play-store/tools/capture.py --serial emulator-5556 --device tablet-7 --locales it-IT

What it does, per locale:
  1. writes demo data straight into the app's DataStore (no personal data: 20 working days, one
     Saturday and a day in progress, all generated here);
  2. puts the status bar in demo mode (clean icons, full battery, no notifications);
  3. walks through the app with taps found via uiautomator and saves 7 screenshots.

The emulator clock is moved to Kiritimati time (UTC+14) so that the "day in progress" falls in the
afternoon whatever the real time is. Needs a debuggable build installed (uses `run-as`).
"""
import argparse
import json
import re
import struct
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

PKG = "com.ilsecondodasinistra.workitout"
TZ = "Pacific/Kiritimati"
OUT = Path(__file__).resolve().parent.parent / "screenshots"

LABELS = {
    "it-IT": {"history": "Storico", "stats": "Statistiche", "settings": "Impostazioni", "back": "Indietro"},
    "en-US": {"history": "History", "stats": "Statistics", "settings": "Settings", "back": "Back"},
}


class Adb:
    def __init__(self, serial):
        self.serial = serial

    def run(self, *args, check=True, binary=False):
        cmd = ["adb", "-s", self.serial, *args]
        res = subprocess.run(cmd, capture_output=True, check=check)
        return res.stdout if binary else res.stdout.decode(errors="replace")

    def shell(self, command):
        return self.run("shell", command)

    def tap_node(self, *names, contains=False, timeout=8):
        """Tap the first UI node whose text or content-description equals (or contains) a name."""
        deadline = time.time() + timeout
        while time.time() < deadline:
            xml = self.run("exec-out", "uiautomator", "dump", "/dev/tty", binary=True).decode(errors="replace")
            xml = xml[xml.find("<?xml"):xml.rfind("</hierarchy>") + len("</hierarchy>")]
            try:
                root = ET.fromstring(xml)
            except ET.ParseError:
                time.sleep(0.5)
                continue
            for node in root.iter("node"):
                for value in (node.get("text", ""), node.get("content-desc", "")):
                    if any((n in value) if contains else (n == value) for n in names) and value:
                        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
                        self.shell(f"input tap {(x1 + x2) // 2} {(y1 + y2) // 2}")
                        return
            time.sleep(0.5)
        raise RuntimeError(f"UI element not found: {names}")

    def screenshot(self, path):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(self.run("exec-out", "screencap", "-p", binary=True))
        print("  saved", path.relative_to(OUT.parent))


# --- Demo data -----------------------------------------------------------------------------

def _varint(n):
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        out.append(b | (0x80 if n else 0))
        if not n:
            return bytes(out)


def _field(number, wire, payload):
    return _varint(number << 3 | wire) + payload


def _bytes(number, data):
    return _field(number, 2, _varint(len(data)) + data)


def _entry(key, value_message):
    return _bytes(1, _bytes(1, key.encode()) + _bytes(2, value_message))


def build_preferences(history, enter, pauses):
    """androidx Preferences DataStore file: map<string, Value>; Value.string=5, double=7, long=4."""
    string = lambda s: _bytes(5, s.encode())
    entries = [
        _entry("work_history_list_json", string(json.dumps(history, separators=(",", ":")))),
        _entry("daily_hours", _field(7, 1, struct.pack("<d", 8.0))),
        _entry("current_enter_time", _field(4, 0, _varint(enter))),
        _entry("current_pauses_list_json", string(json.dumps(pauses, separators=(",", ":")))),
    ]
    return b"".join(entries)


def demo_data(now):
    """Returns (history newest-first, active enter millis, active pauses)."""
    zone = ZoneInfo(TZ)
    today = now.astimezone(zone).date()
    ms = lambda dt: int(dt.timestamp() * 1000)
    at = lambda day, h, m: datetime(day.year, day.month, day.day, h, m, tzinfo=zone)

    deltas = [10, -5, 20, 0, 35, -15, 5, 15, -10, 25, 0, 40, -20, 10, 5, -5, 30, 15, 0, 20]
    enters = [(8, 40), (9, 5), (8, 50), (8, 55), (9, 0), (8, 35), (8, 45), (9, 10)]
    entries, i = [], 0
    day = today - timedelta(days=1)
    while day > today - timedelta(days=27):
        weekday = day.weekday()
        if weekday < 5 or (weekday == 5 and day == today - timedelta(days=7)):
            saturday = weekday == 5
            h, m = (10, 0) if saturday else enters[i % len(enters)]
            enter = at(day, h, m)
            if saturday:
                pauses = []
                exit_ = enter + timedelta(hours=3, minutes=10)
            else:
                lunch = 40 + (i * 7) % 25
                lunch_start = at(day, 12, 30 + (i * 5) % 20)
                pauses = [(lunch_start, lunch_start + timedelta(minutes=lunch))]
                if i % 3 == 0:
                    coffee = at(day, 15, 30)
                    pauses.append((coffee, coffee + timedelta(minutes=15)))
                paused = sum((b - a for a, b in pauses), timedelta())
                exit_ = enter + timedelta(hours=8) + paused + timedelta(minutes=deltas[i % len(deltas)])
            worked = exit_ - enter - sum((b - a for a, b in pauses), timedelta())
            total = int(worked.total_seconds() // 60)
            entries.append({
                "id": str(ms(enter)),
                "enterTime": ms(enter),
                "pauses": [{"start": ms(a), "end": ms(b)} for a, b in pauses],
                "exitTime": ms(exit_),
                "totalWorkedTime": f"{total // 60}h {total % 60}m",
                "dailyHoursTarget": 0.0 if saturday else 8.0,
            })
            i += 1
        day -= timedelta(days=1)

    # Day in progress: entered 5h30m ago, lunch break already over.
    enter = ms(now - timedelta(hours=5, minutes=30))
    pauses = [{"start": ms(now - timedelta(hours=1, minutes=36)), "end": ms(now - timedelta(minutes=46))}]
    return entries, enter, pauses


def seed(adb, now):
    history, enter, pauses = demo_data(now)
    adb.run("shell", "am", "force-stop", PKG)
    with tempfile.NamedTemporaryFile(suffix=".pb") as tmp:
        tmp.write(build_preferences(history, enter, pauses))
        tmp.flush()
        adb.run("push", tmp.name, "/data/local/tmp/seed.pb")
    adb.shell(f"run-as {PKG} sh -c 'mkdir -p files/datastore && cat /data/local/tmp/seed.pb > files/datastore/work_history_datastore.preferences_pb'")
    adb.shell("rm /data/local/tmp/seed.pb")


# --- Device setup --------------------------------------------------------------------------

def prepare_device(adb):
    adb.shell("settings put global auto_time_zone 0")
    adb.shell(f"cmd alarm set-timezone {TZ}")
    adb.shell("settings put global sysui_demo_allowed 1")
    adb.shell(f"appops set {PKG} SCHEDULE_EXACT_ALARM allow")
    adb.run("shell", "pm", "grant", PKG, "android.permission.POST_NOTIFICATIONS", check=False)
    adb.shell("cmd uimode night no")


def demo_status_bar(adb, hhmm):
    def demo(command, *extras):
        adb.shell("am broadcast -a com.android.systemui.demo -e command " + command + " " + " ".join(extras))
    demo("enter")
    demo("clock", f"-e hhmm {hhmm}")
    demo("battery", "-e level 100 -e plugged false")
    demo("network", "-e wifi show -e level 4 -e fully true")
    demo("network", "-e mobile hide")
    demo("notifications", "-e visible false")


def launch(adb, wait=4.5):
    adb.run("shell", "am", "force-stop", PKG)
    adb.run("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    time.sleep(wait)  # lets the "expected exit" toast disappear


def capture_locale(adb, device, locale):
    labels = LABELS[locale]
    out = OUT / locale / device
    now = datetime.now(ZoneInfo(TZ))
    seed(adb, now)
    demo_status_bar(adb, now.astimezone(ZoneInfo(TZ)).strftime("%H%M"))
    adb.shell(f"cmd locale set-app-locales {PKG} --locales {locale}")
    print(f"[{device} {locale}]")

    adb.shell("cmd uimode night no")
    launch(adb)
    adb.screenshot(out / "01-home.png")

    adb.tap_node(labels["history"])
    time.sleep(1.5)
    adb.screenshot(out / "02-history.png")

    adb.tap_node("→", contains=True)
    time.sleep(1.5)
    adb.screenshot(out / "03-edit-day.png")
    adb.shell("input keyevent 4")
    time.sleep(1)
    adb.shell("input keyevent 4")
    time.sleep(1)

    adb.tap_node(labels["stats"])
    time.sleep(1.5)
    adb.screenshot(out / "04-statistics.png")
    adb.shell("input keyevent 4")
    time.sleep(1)

    adb.tap_node(labels["settings"])
    time.sleep(1.5)
    adb.screenshot(out / "05-settings.png")

    adb.shell("cmd uimode night yes")
    launch(adb, wait=2)
    adb.tap_node(labels["stats"])
    time.sleep(1.5)
    adb.screenshot(out / "06-statistics-dark.png")
    adb.shell("input keyevent 4")
    time.sleep(1)
    launch(adb)
    adb.screenshot(out / "07-home-dark.png")
    adb.shell("cmd uimode night no")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--device", required=True, choices=["phone", "tablet-7", "tablet-10"])
    parser.add_argument("--locales", nargs="+", default=["it-IT", "en-US"], choices=list(LABELS))
    args = parser.parse_args()

    adb = Adb(args.serial)
    prepare_device(adb)
    for locale in args.locales:
        capture_locale(adb, args.device, locale)
    adb.shell("cmd locale set-app-locales " + PKG + " --locales it-IT")
    adb.run("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "exit", check=False)


if __name__ == "__main__":
    sys.exit(main())
