"""Serialises heavy work across parallel agents on the Windows workspace (native Windows, no WSL).

Usage, through the Git Bash shims next to this file:
  locked gradle <command...>   one Gradle build machine-wide, after the memory wait
  locked adb <command...>      one agent driving the phone at a time; take adb before gradle when a command needs both
  locked <name> <command...>   any other named lock, without waits
  locked emu ...               refused: this machine has no emulator, the phone is the test device
  locked status                the locks and their holders, free memory, CPU temperature, Java processes
  memwait [command...]         the memory wait alone, then the command if one is given
  thermlog [seconds]           temperature, CPU and memory every 10 s into logs/thermal-windows.log

Each lock is a byte-range lock on a handle that child processes do not inherit: Windows drops it when this process
exits, and the adb server or Gradle daemon a command starts can never hold it (on Linux, flock without -o let the adb
server keep adb.lock forever, Oct 16). Commands run in Git Bash, because agents write them as shell commands
(`./gradlew ...`). Gradle's heap and daemon idle time come from D:\\.caches\\gradle\\gradle.properties, never from here.
"""
import ctypes
import ctypes.wintypes as wintypes
import errno
import json
import msvcrt
import os
import re
import subprocess
import sys
import time
import winreg

OV = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# Claude's shells start without the user's environment, so a bare build would create ~\.gradle and miss the SDK.
USER_VARS = ("JAVA_HOME", "ANDROID_HOME", "GRADLE_USER_HOME", "GOMODCACHE", "GOCACHE", "PIP_CACHE_DIR",
             "NPM_CONFIG_CACHE", "KONAN_DATA_DIR")
PATH_KEYS = ((winreg.HKEY_LOCAL_MACHINE, r"SYSTEM\CurrentControlSet\Control\Session Manager\Environment"),
             (winreg.HKEY_CURRENT_USER, "Environment"))
# Robolectric downloads the Android framework jars once; without this they land in the user profile's .m2.
JAVA_TOOL_OPTIONS = r"-Dmaven.repo.local=D:\.caches\m2"
# Device paths stay as typed for adb while local /d/... and /tmp/... paths are still converted for native programs.
DEVICE_PATHS = "/sdcard;/storage;/data;/system;/vendor;/product;/apex;/proc;/sys;/dev;/mnt;/odm;/metadata"
FLOOR_MB = 1000
# Parallel test tasks each fork a JVM; on a 16 GB laptop that already runs Chrome and Claude, more than four at once
# exhausts memory.
GRADLE_WORKERS = os.environ.get("OVERHAUL_GRADLE_WORKERS", "4")
TEMP_COUNTER = r"\Thermal Zone Information(*)\High Precision Temperature"
CPU_COUNTER = r"\Processor Information(_Total)\% Processor Utility"

kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
kernel32.OpenProcess.restype = wintypes.HANDLE
kernel32.OpenProcess.argtypes = (wintypes.DWORD, wintypes.BOOL, wintypes.DWORD)
kernel32.WaitForSingleObject.argtypes = (wintypes.HANDLE, wintypes.DWORD)
kernel32.GetProcessTimes.argtypes = (wintypes.HANDLE,) + (ctypes.POINTER(wintypes.FILETIME),) * 4
kernel32.CloseHandle.argtypes = (wintypes.HANDLE,)


class MemoryStatus(ctypes.Structure):
    _fields_ = [("dwLength", wintypes.DWORD), ("dwMemoryLoad", wintypes.DWORD),
                ("ullTotalPhys", ctypes.c_ulonglong), ("ullAvailPhys", ctypes.c_ulonglong),
                ("ullTotalPageFile", ctypes.c_ulonglong), ("ullAvailPageFile", ctypes.c_ulonglong),
                ("ullTotalVirtual", ctypes.c_ulonglong), ("ullAvailVirtual", ctypes.c_ulonglong),
                ("ullAvailExtendedVirtual", ctypes.c_ulonglong)]


def say(kind, message):
    print(f"[locked:{kind}] {message}", file=sys.stderr, flush=True)


def env_number(name, default):
    try:
        return float(os.environ[name]) if os.environ.get(name) else default
    except ValueError:
        say("config", f"ignoring {name}={os.environ[name]!r}: not a number")
        return default


COLD_MB = env_number("MEMWAIT_MB", 3500)
WARM_MB = env_number("MEMWAIT_WARM_MB", 1500)
# Off unless asked for: under sustained load TZ00 already sits near 97 C on this laptop, so a fixed bar there would
# stall every build while it holds the lock; the user considers the Linux power-offs a Linux problem (Oct 17).
HOT_C = env_number("THERMWAIT_C", None)


def memory_mb():
    """Available physical memory and available commit (RAM plus page file), in MB."""
    status = MemoryStatus(dwLength=ctypes.sizeof(MemoryStatus))
    if not kernel32.GlobalMemoryStatusEx(ctypes.byref(status)):
        raise ctypes.WinError(ctypes.get_last_error())
    return status.ullAvailPhys >> 20, status.ullAvailPageFile >> 20


class PerformanceInfo(ctypes.Structure):
    _fields_ = [("cb", wintypes.DWORD), ("CommitTotal", ctypes.c_size_t), ("CommitLimit", ctypes.c_size_t),
                ("CommitPeak", ctypes.c_size_t), ("PhysicalTotal", ctypes.c_size_t),
                ("PhysicalAvailable", ctypes.c_size_t), ("SystemCache", ctypes.c_size_t),
                ("KernelTotal", ctypes.c_size_t), ("KernelPaged", ctypes.c_size_t),
                ("KernelNonpaged", ctypes.c_size_t), ("PageSize", ctypes.c_size_t),
                ("HandleCount", wintypes.DWORD), ("ProcessCount", wintypes.DWORD), ("ThreadCount", wintypes.DWORD)]


def cache_working_set_mb(avail):
    """The system cache's working set, in MB: memory Windows lends to the file cache and takes back the moment a
    program allocates, but does not count as available until it is trimmed. SystemCache is that working set plus the
    standby list, and available is the standby list plus free pages, so the difference slightly undercounts it."""
    info = PerformanceInfo(cb=ctypes.sizeof(PerformanceInfo))
    if not ctypes.WinDLL("psapi").GetPerformanceInfo(ctypes.byref(info), info.cb):
        return 0
    return max(0, ((info.SystemCache * info.PageSize) >> 20) - avail)


def java_processes():
    out = subprocess.run(["tasklist", "/FI", "IMAGENAME eq java.exe", "/FO", "CSV", "/NH"],
                         capture_output=True, text=True, errors="replace").stdout
    return out.count('"java.exe"')


def counters():
    """Hottest thermal zone in degrees C and total CPU utility in %, or None where Windows reports nothing."""
    try:
        out = subprocess.run(["typeperf", TEMP_COUNTER, CPU_COUNTER, "-sc", "1"],
                             capture_output=True, text=True, errors="replace", timeout=30).stdout
    except (OSError, subprocess.TimeoutExpired):
        return None, None
    rows = [line.strip().strip('"').split('","') for line in out.splitlines() if line.startswith('"')]
    if len(rows) < 2:
        return None, None
    temps, cpu = [], None
    for name, value in zip(rows[0][1:], rows[-1][1:]):
        try:
            number = float(value)
        except ValueError:
            continue
        if "Thermal Zone" in name:
            temps.append(number / 10 - 273.15)
        elif "Processor Utility" in name:
            cpu = round(number)
    return (round(max(temps), 1) if temps else None), cpu


def process_started(pid):
    """Creation time of a running process, or None once it has exited; the time guards against a reused pid."""
    handle = kernel32.OpenProcess(0x00101000, False, pid)  # SYNCHRONIZE | PROCESS_QUERY_LIMITED_INFORMATION
    if not handle:
        return None
    try:
        if kernel32.WaitForSingleObject(handle, 0) == 0:
            return None
        times = [wintypes.FILETIME() for _ in range(4)]
        if not kernel32.GetProcessTimes(handle, *(ctypes.byref(t) for t in times)):
            return None
        return times[0].dwHighDateTime << 32 | times[0].dwLowDateTime
    finally:
        kernel32.CloseHandle(handle)


def prepare_env(kind):
    """Edits this process's environment in place: a dict copy of os.environ upper-cases every name on Windows, which
    would hand the command MY_VAR for the caller's my_var."""
    env = os.environ
    with winreg.OpenKey(winreg.HKEY_CURRENT_USER, "Environment") as key:
        for name in USER_VARS:
            if env.get(name):
                continue
            try:
                value, value_type = winreg.QueryValueEx(key, name)
            except FileNotFoundError:
                continue
            env[name] = winreg.ExpandEnvironmentStrings(value) if value_type == winreg.REG_EXPAND_SZ else value
    # Tools installed after this session started (Node, ffmpeg) are on the registry PATH but not on the shell's.
    path = env.get("PATH", "")
    known = {p.rstrip("\\/").lower() for p in path.split(os.pathsep) if p}
    for hive, subkey in PATH_KEYS:
        try:
            with winreg.OpenKey(hive, subkey) as key:
                value, _ = winreg.QueryValueEx(key, "Path")
        except FileNotFoundError:
            continue
        for entry in winreg.ExpandEnvironmentStrings(value).split(os.pathsep):
            if entry and entry.rstrip("\\/").lower() not in known and os.path.isdir(entry):
                path += os.pathsep + entry
                known.add(entry.rstrip("\\/").lower())
    env["PATH"] = path
    if kind == "gradle" and "maven.repo.local" not in env.get("JAVA_TOOL_OPTIONS", ""):
        env["JAVA_TOOL_OPTIONS"] = (env.get("JAVA_TOOL_OPTIONS", "") + " " + JAVA_TOOL_OPTIONS).strip()
    # The shims set MSYS_NO_PATHCONV so arguments reach Python unchanged; give the command the caller's setting back.
    caller = env.pop("LOCKED_CALLER_NOPATHCONV", "unset")
    if caller == "unset":
        env.pop("MSYS_NO_PATHCONV", None)
    else:
        env["MSYS_NO_PATHCONV"] = caller
    if kind == "adb" and "MSYS_NO_PATHCONV" not in env:
        env["MSYS2_ARG_CONV_EXCL"] = DEVICE_PATHS
    if kind == "gradle" and "org.gradle.workers.max" not in env.get("GRADLE_OPTS", ""):
        env["GRADLE_OPTS"] = (env.get("GRADLE_OPTS", "") + f" -Dorg.gradle.workers.max={GRADLE_WORKERS}").strip()


def git_bash():
    # The shim passes the Git Bash that called it. A bare "bash" from a Windows process resolves to System32's WSL
    # launcher, and Git's bin\bash.exe is only a native wrapper around usr\bin\bash.exe.
    bash = os.environ.pop("LOCKED_BASH", "")
    if not bash:
        exec_path = subprocess.run(["git", "--exec-path"], capture_output=True, text=True, check=True).stdout.strip()
        bash = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(exec_path))), "usr", "bin", "bash.exe")
    if not os.path.isfile(bash):
        sys.exit(f"[locked] Git Bash not found at {bash}")
    return bash


def shell_status(code):
    # An MSYS process killed by signal N exits with N << 8, which the shim's bash would read as 0 (success).
    if code > 255:
        return 128 + (code >> 8) if code & 0xFF == 0 and code >> 8 < 128 else (code & 0xFF or 1)
    return code


def run(kind, argv, on_start=None):
    # The command travels in an environment variable: a Windows command line would let Git Bash expand globs
    # such as --tests 'Foo*' before the command sees them.
    os.environ["LOCKED_COMMAND"] = "exec -- " + " ".join("'" + arg.replace("'", "'\\''") + "'" for arg in argv)
    child = subprocess.Popen([git_bash(), "-c", 'c=$LOCKED_COMMAND; unset LOCKED_COMMAND; eval "$c"'])
    if on_start:
        on_start(child.pid)
    while True:
        try:
            return shell_status(child.wait())
        except KeyboardInterrupt:
            # Never kill: the command got the same Ctrl+C, and the lock must outlive it.
            say(kind, "interrupted; the lock is released when the command exits")


def lock_path(kind):
    return os.path.join(OV, f"{kind}.lock")


def try_lock(fd):
    os.lseek(fd, 0, os.SEEK_SET)
    try:
        msvcrt.locking(fd, msvcrt.LK_NBLCK, 1)
        return True
    except OSError as e:
        if e.errno in (errno.EACCES, errno.EDEADLOCK):
            return False
        raise


def unlock(fd):
    os.lseek(fd, 0, os.SEEK_SET)
    msvcrt.locking(fd, msvcrt.LK_UNLCK, 1)


def read_holder(kind):
    try:
        with open(lock_path(kind) + ".holder", encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return {}


def write_holder(kind, info):
    with open(lock_path(kind) + ".holder", "w", encoding="utf-8") as f:
        json.dump(info, f)


def holder(kind):
    info = read_holder(kind)
    if "pid" not in info:
        return "holder unknown"
    return f"pid {info['pid']} since {info['since']}: {info.get('command', '')[:120]}"


def acquire(kind, argv):
    fd = os.open(lock_path(kind), os.O_RDWR | os.O_CREAT)
    said = None
    while not try_lock(fd):
        if said is None or time.monotonic() - said >= 300:
            say(kind, f"waiting for the lock ({holder(kind)})")
            said = time.monotonic()
        time.sleep(2)
    # A holder that died (killed, crashed) freed the lock but not its command; wait for that command to finish.
    previous, said = read_holder(kind), None
    while previous.get("child") and process_started(previous["child"]) == previous.get("child_started"):
        if said is None or time.monotonic() - said >= 300:
            say(kind, f"holder pid {previous.get('pid')} died; waiting for its command (pid {previous['child']})")
            said = time.monotonic()
        time.sleep(5)
    info = {"pid": os.getpid(), "since": time.strftime("%Y-%m-%d %H:%M:%S"), "cwd": os.getcwd(),
            "command": " ".join(argv)}
    write_holder(kind, info)
    return fd, info


def is_held(kind):
    if not os.path.exists(lock_path(kind)):
        return False
    fd = os.open(lock_path(kind), os.O_RDWR)
    try:
        if try_lock(fd):
            unlock(fd)
            return False
        return True
    finally:
        os.close(fd)


def holds(kind):
    """True when an enclosing `locked <kind>` of this command still holds the lock (nesting, never a stale variable)."""
    pid = dict(entry.split(":", 1) for entry in os.environ.get("LOCKED_HELD", "").split(",") if ":" in entry).get(kind)
    return pid is not None and str(read_holder(kind).get("pid")) == pid and is_held(kind)


def wait_for_resources(kind):
    start, said = time.monotonic(), None
    temp = counters()[0] if HOT_C is not None else None
    while temp is not None and temp >= HOT_C and time.monotonic() - start < 600:
        if said is None or time.monotonic() - said >= 60:
            say(kind, f"CPU zone at {temp} C, waiting to fall under {HOT_C} C (at most 10 min)")
            said = time.monotonic()
        time.sleep(15)
        temp, _ = counters()
    start, said = time.monotonic(), None
    while True:
        need = WARM_MB if java_processes() else COLD_MB
        avail, commit = memory_mb()
        if avail >= need and commit >= FLOOR_MB:
            return
        waited = time.monotonic() - start
        # Oct 19: after Dev Drive builds and recordings Windows kept 7.7 GB in the cache's working set with 0.4 GB
        # available, and this wait would have held a test run for 2.5 hours. Half of that working set counts after 5
        # minutes, as long as the commit charge has room for the build (the page file still covers a misjudgement).
        cache = cache_working_set_mb(avail)
        if waited >= 300 and commit >= need + FLOOR_MB and avail + cache // 2 >= need:
            say(kind, f"{avail} MB available plus {cache} MB of system cache Windows will trim; continuing")
            return
        # Past 30 minutes go ahead unless the machine is critically short; past 2.5 hours go ahead regardless.
        if (waited >= 1800 and avail >= FLOOR_MB and commit >= FLOOR_MB) or waited >= 9000:
            say(kind, f"still {avail} MB available after {int(waited // 60)} min (wanted {need:.0f} MB); continuing")
            return
        if said is None or time.monotonic() - said >= 60:
            say(kind, f"{avail} MB available, {commit} MB commit left; waiting for {need:.0f} MB")
            said = time.monotonic()
        time.sleep(10)


def status():
    for kind in sorted(f[:-5] for f in os.listdir(OV) if f.endswith(".lock")):
        print(f"{kind:8} {'HELD by ' + holder(kind) if is_held(kind) else 'free'}")
    avail, commit = memory_mb()
    temp, cpu = counters()
    print(f"memory   {avail} MB available (+{cache_working_set_mb(avail)} MB system cache), {commit} MB commit left")
    print(f"cpu      zone {temp} C (wait {'at ' + str(HOT_C) + ' C' if HOT_C is not None else 'off'}), utility {cpu} %")
    print(f"java     {java_processes()} java.exe process(es)")


def thermlog(seconds):
    os.makedirs(os.path.join(OV, "logs"), exist_ok=True)
    out = os.path.join(OV, "logs", "thermal-windows.log")
    end = time.monotonic() + seconds if seconds else None
    while end is None or time.monotonic() < end:
        tick = time.monotonic()
        temp, cpu = counters()
        avail, commit = memory_mb()
        with open(out, "a", encoding="utf-8") as f:
            f.write(f"{time.strftime('%Y-%m-%d %H:%M:%S')} zone={temp}C cpu={cpu}% memavail={avail}MB "
                    f"commitleft={commit}MB java={java_processes()}\n")
            f.flush()
            os.fsync(f.fileno())
        time.sleep(max(0.0, 10 - (time.monotonic() - tick)))


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    kind, argv = sys.argv[1], sys.argv[2:]
    if kind == "status":
        return status()
    if kind == "thermlog":
        return thermlog(int(argv[0]) if argv else 0)
    if kind == "emu":
        say(kind, "no emulator on this machine: the Redmi 9a7c74f4 is the test device; record the step as pending")
        return 2
    if kind == "memwait":
        prepare_env(kind)
        wait_for_resources(kind)
        return run(kind, argv) if argv else 0
    if not re.fullmatch(r"[a-z0-9-]+", kind) or not argv:
        sys.exit(__doc__)
    prepare_env(kind)
    if holds(kind):
        return run(kind, argv)
    if kind == "adb" and holds("gradle"):
        say(kind, "this command holds gradle; take adb first: locked adb locked gradle <command...>")
        return 2
    fd, info = acquire(kind, argv)
    held = os.environ.get("LOCKED_HELD", "")
    os.environ["LOCKED_HELD"] = (held + "," if held else "") + f"{kind}:{os.getpid()}"

    def record_child(pid):
        write_holder(kind, dict(info, child=pid, child_started=process_started(pid)))
    try:
        if kind == "gradle":
            wait_for_resources(kind)
        return run(kind, argv, record_child)
    finally:
        unlock(fd)
        os.close(fd)


if __name__ == "__main__":
    sys.exit(main())
