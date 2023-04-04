import os
import unittest
import platform
import networkx as nx
from hashlib import sha256
from unittest.mock import MagicMock, patch
from server.server.system.fstree import FileSystemTree


class TestFileSystemTree(unittest.TestCase):
    def setUp(self) -> None:
        self.test_uri = "neo4j://localhost:7687"
        self.test_user = "neo4j"
        self.test_password = "password"

    def test_init(self) -> None:
        with patch("neo4j.GraphDatabase.driver") as mock_driver:
            fst = FileSystemTree(self.test_uri, self.test_user, self.test_password)
            mock_driver.assert_called_once_with(
                self.test_uri, auth=(self.test_user, self.test_password)
            )

    def test_ready(self) -> None:
        with patch("builtins.print") as mock_print:
            with FileSystemTree(
                self.test_uri, self.test_user, self.test_password
            ) as fst:
                fst.ready()
                mock_print.assert_called_once_with("started neo4j")

    def test_close(self) -> None:
        with patch("neo4j.GraphDatabase.driver") as mock_driver:
            with FileSystemTree(
                self.test_uri, self.test_user, self.test_password
            ) as fst:
                fst.close()
                fst.driver.close.assert_called_once()

    def test_print_greeting(self) -> None:
        test_message = "Hello, World!"
        with patch("neo4j.GraphDatabase.driver") as mock_driver:
            session = MagicMock()
            mock_driver.return_value.session.return_value.__enter__.return_value = (
                session
            )
            fst = FileSystemTree(self.test_uri, self.test_user, self.test_password)
            with patch("builtins.print") as mock_print:
                fst.print_greeting(test_message)
                session.execute_write.assert_called_once()
                mock_print.assert_called_once()

    def test_get_system_info(self) -> None:
        # get system info using the function
        with FileSystemTree(self.test_uri, self.test_user, self.test_password) as fst:
            system_info = fst.get_system_info()

            # verify that the output is correct
            assert isinstance(system_info, dict)
            assert (
                "platform" in system_info
                and system_info["platform"] == platform.system()
            )
            assert (
                "platform-release" in system_info
                and system_info["platform-release"] == platform.release()
            )
            assert (
                "platform-version" in system_info
                and system_info["platform-version"] == platform.version()
            )
            assert (
                "architecture" in system_info
                and system_info["architecture"] == platform.machine()
            )
            assert (
                "hostname" in system_info and system_info["hostname"] == platform.node()
            )
            assert (
                "processor" in system_info
                and system_info["processor"] == platform.processor()
            )
            assert (
                "ram" in system_info
                and isinstance(system_info["ram"], str)
                and "GB" in system_info["ram"]
            )
            assert (
                "uptime" in system_info
                and isinstance(system_info["uptime"], int)
                and system_info["uptime"] > 0
            )

    def test_map_file_system(self) -> None:
        def cleanup(test_dir: str) -> None:
            # clean up the temporary directory
            os.remove(os.path.join(test_dir, "file1.txt"))
            os.remove(os.path.join(test_dir, "subdir", "file2.txt"))
            os.rmdir(os.path.join(test_dir, "subdir"))
            os.rmdir(test_dir)

        # create a temporary directory with some files and directories
        base_dir = os.path.dirname(os.path.abspath(__file__))
        test_dir = os.path.join(base_dir, "test_dir")
        cleanup(test_dir)
        os.makedirs(test_dir)
        os.makedirs(os.path.join(test_dir, "subdir"))
        with open(os.path.join(test_dir, "file1.txt"), "w") as f:
            f.write("Hello, world!")
        with open(os.path.join(test_dir, "subdir", "file2.txt"), "w") as f:
            f.write("Goodbye, world!")

        # run the map_file_system function
        with FileSystemTree(self.test_uri, self.test_user, self.test_password) as fst:
            file_tree = fst.map_file_system(test_dir)
            print(nx.to_dict_of_dicts(file_tree))

            # verify that the output is correct
            assert isinstance(file_tree, nx.DiGraph)
            assert file_tree.has_node(test_dir)

            print(f'file1: {os.path.join(test_dir, "file1.txt")}')
            print(f"tree: {nx.to_dict_of_dicts(file_tree)}")

            assert file_tree.has_node(os.path.join(test_dir, "file1.txt"))
            assert file_tree.has_node(os.path.join(test_dir, "subdir"))
            assert file_tree.has_node(os.path.join(test_dir, "subdir", "file2.txt"))
            assert file_tree.nodes[test_dir]["type"] == "directory"
            assert (
                file_tree.nodes[os.path.join(test_dir, "file1.txt")]["type"] == "file"
            )
            assert (
                file_tree.nodes[os.path.join(test_dir, "subdir")]["type"] == "directory"
            )
            assert (
                file_tree.nodes[os.path.join(test_dir, "subdir", "file2.txt")]["type"]
                == "file"
            )
            assert file_tree.successors(test_dir) == [
                os.path.join(test_dir, "file1.txt"),
                os.path.join(test_dir, "subdir"),
            ]
            assert file_tree.successors(os.path.join(test_dir, "subdir")) == [
                os.path.join(test_dir, "subdir", "file2.txt")
            ]

    def test__calculate_merkle_tree(self) -> None:
        # Create a graph with nodes and attributes
        G = nx.DiGraph()
        G.add_node("a", type="directory")
        G.add_node("b", type="file", name="file1.txt")
        G.add_node("c", type="file", name="file2.txt")
        G.add_edge("a", "b")
        G.add_edge("a", "c")

        with FileSystemTree(self.test_uri, self.test_user, self.test_password) as fst:

            # Calculate the Merkle tree
            merkle_tree = fst._calculate_merkle_tree(G)

            # Verify the expected hash values for each node
            expected_hashes = {
                "a": sha256(repr({"type": "directory"}).encode()).hexdigest(),
                "b": sha256(
                    repr({"type": "file", "name": "file1.txt"}).encode()
                ).hexdigest(),
                "c": sha256(
                    repr({"type": "file", "name": "file2.txt"}).encode()
                ).hexdigest(),
            }
            assert merkle_tree == expected_hashes

    def test__find_differences(self) -> None:
        # Create two Merkle trees
        original_tree = {"a": "hash1", "b": "hash2", "c": "hash3"}
        new_tree = {"a": "hash1", "b": "newhash", "d": "hash4"}

        with FileSystemTree(self.test_uri, self.test_user, self.test_password) as fst:
            # Find the differences between the two trees
            differences = fst._find_differences(original_tree, new_tree)

            # Verify the expected differences
            expected_differences = {"b": "newhash", "c": None, "d": "hash4"}
            assert differences == expected_differences

    def test_update_graph_with_merkle_tree(self) -> None:
        # Create a graph with nodes and edges
        G = nx.DiGraph()
        G.add_node("a", type="directory")
        G.add_node("b", type="file", name="file1.txt")
        G.add_node("c", type="file", name="file2.txt")
        G.add_edge("a", "b")
        G.add_edge("a", "c")

        # Write the graph to a file
        graphml_file = "test_graph.graphml"
        nx.write_graphml(G, graphml_file)

        # Create a new file tree with some modifications
        new_tree = nx.DiGraph()
        new_tree.add_node("a", type="directory")
        new_tree.add_node("b", type="file", name="file1.txt", size="100KB")
        new_tree.add_edge("a", "b")
        new_tree.add_node("d", type="file", name="file3.txt")
        new_tree.add_edge("a", "d")

        with FileSystemTree(self.test_uri, self.test_user, self.test_password) as fst:

            # Update the original graph with the differences
            updated_graph = fst.update_graph_with_merkle_tree(new_tree, graphml_file)

            # Verify that the graph has been updated correctly
            assert updated_graph.has_node("a")
            assert updated_graph.has_node("b")
            assert updated_graph.has_node("d")
            assert not updated_graph.has_node("c")
            assert updated_graph.has_edge("a", "b")
            assert updated_graph.has_edge("a", "d")

            # Clean up the test file
            os.remove(graphml_file)
