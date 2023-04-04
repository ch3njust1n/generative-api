import os
import time
import hashlib
from collections import deque
from typing import Dict, List, Union
import psutil
import platform
from django.apps import AppConfig
from neo4j import GraphDatabase
import networkx as nx


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
        file_tree = self.map_file_system("/")
        nx.write_graphml(file_tree, "file_tree.graphml")

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

    def map_file_system(self, path: str = "/") -> nx.DiGraph:
        file_tree = nx.DiGraph()
        file_tree.add_node(path, type="directory")

        queue = deque()
        queue.append(path)

        while queue:
            current_path = queue.popleft()

            if "/dev" in current_path or "/proc" in current_path:
                continue

            try:
                with os.scandir(current_path) as entries:
                    for entry in entries:
                        if entry.is_file():
                            file_tree.add_node(
                                os.path.join(current_path, entry.name),
                                type="file",
                                name=entry.name,
                            )
                            file_tree.add_edge(
                                current_path, os.path.join(current_path, entry.name)
                            )
                        elif entry.is_dir():
                            subdir = os.path.join(current_path, entry.name)
                            file_tree.add_node(
                                subdir, type="directory", name=entry.name
                            )
                            file_tree.add_edge(current_path, subdir)
                            queue.append(subdir)

            except (PermissionError, FileNotFoundError):
                pass

        return file_tree

    def update_graph_with_merkle_tree(
        self, file_tree: nx.DiGraph, graphml_file: str
    ) -> nx.DiGraph:
        # load the graph from the graphml file
        G = nx.read_graphml(graphml_file)

        # calculate the Merkle Trees for the original and new file trees
        original_merkle_tree = self._calculate_merkle_tree(G)
        new_merkle_tree = self._calculate_merkle_tree(file_tree)

        # find the differences between the two trees
        differences = self._find_differences(original_merkle_tree, new_merkle_tree)

        # apply the differences to the original graph
        for node, attributes in differences.items():
            if attributes is None:
                G.remove_node(node)
            else:
                G.add_node(node, **attributes)

        return G

    def _calculate_merkle_tree(self, file_tree: nx.DiGraph) -> dict:
        merkle_tree = {}
        for node in sorted(file_tree.nodes):
            hash_value = hashlib.sha256(
                repr(file_tree.nodes[node]).encode()
            ).hexdigest()
            merkle_tree[node] = hash_value

        return merkle_tree

    def _find_differences(self, original_tree: dict, new_tree: dict) -> dict:
        differences = {}

        # find nodes in new_tree that are not in original_tree
        for node, hash_value in new_tree.items():
            if node not in original_tree:
                differences[node] = new_tree[node]
            elif original_tree[node] != hash_value:
                differences[node] = new_tree[node]

        # find nodes in original_tree that are not in new_tree
        for node, hash_value in original_tree.items():
            if node not in new_tree:
                differences[node] = None

        return differences
