"""
File system information ingestion
"""

import os
import time
from collections import deque
from typing import Dict, List, Union
from dotenv import load_dotenv
import psutil
import platform

load_dotenv()


def get_system_info() -> Dict[str, Union[str, int, float]]:
    system_info = {
        "platform": platform.system(),
        "platform-release": platform.release(),
        "platform-version": platform.version(),
        "architecture": platform.machine(),
        "hostname": platform.node(),
        "processor": platform.processor(),
        "ram": str(round(psutil.virtual_memory().total / (1024 * 1024 * 1024), 2))
        + " GB",
        "uptime": int(time.time() - psutil.boot_time()),
    }
    return system_info


def map_file_system(path: str = "/") -> Dict[str, Union[str, List]]:
    file_tree = {"type": "directory", "contents": []}

    queue = deque()
    queue.append((file_tree["contents"], path))

    while queue:
        current_contents, current_path = queue.popleft()

        if "/dev" in current_path or "/proc" in current_path:
            continue

        try:
            with os.scandir(current_path) as entries:
                for entry in entries:
                    if entry.is_file():
                        current_contents.append({"type": "file", "name": entry.name})
                    elif entry.is_dir():
                        subdir = {
                            "type": "directory",
                            "name": entry.name,
                            "contents": [],
                        }
                        current_contents.append(subdir)
                        queue.append(
                            (subdir["contents"], os.path.join(current_path, entry.name))
                        )
        except (PermissionError, FileNotFoundError):
            pass

    return file_tree


def main():
    start = time.perf_counter()
    system_info = get_system_info()
    print(system_info)
    print(f"{time.perf_counter() - start} s")

    start = time.perf_counter()
    file_tree = map_file_system()
    print(file_tree)
    print(f"{time.perf_counter() - start} s")


if __name__ == "__main__":
    main()
