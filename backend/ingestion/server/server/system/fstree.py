import os
import time
from collections import deque
from typing import Dict, List, Union
import psutil
import platform
from django.apps import AppConfig
from neo4j import GraphDatabase


class FileSystemTree(AppConfig):
    def __init__(self, uri, user, password):
        self.uri = uri
        self.user = user
        self.password = password
        self.driver = GraphDatabase.driver(uri, auth=(user, password))

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()

    def ready(self):
        print("started neo4j")

    def close(self):
        self.driver.close()

    def print_greeting(self, message):
        with self.driver.session() as session:
            greeting = session.execute_write(self._create_and_return_greeting, message)
            print(greeting)

    @staticmethod
    def _create_and_return_greeting(tx, message):
        result = tx.run(
            "CREATE (a:Greeting) "
            "SET a.message = $message "
            "RETURN a.message + ', from node ' + id(a)",
            message=message,
        )
        return result.single()[0]

    def get_system_info(self) -> Dict[str, Union[str, int, float]]:
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

    def map_file_system(self, path: str = "/") -> Dict[str, Union[str, List]]:
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
                            current_contents.append(
                                {"type": "file", "name": entry.name}
                            )
                        elif entry.is_dir():
                            subdir = {
                                "type": "directory",
                                "name": entry.name,
                                "contents": [],
                            }
                            current_contents.append(subdir)
                            queue.append(
                                (
                                    subdir["contents"],
                                    os.path.join(current_path, entry.name),
                                )
                            )
            except (PermissionError, FileNotFoundError):
                pass

        return file_tree
