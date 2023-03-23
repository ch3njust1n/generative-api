import unittest
from unittest.mock import MagicMock, patch
from server.server.fstree.neo4j import FileSystemTree

class TestFileSystemTree(unittest.TestCase):

    def setUp(self) -> None:
        self.test_uri = 'neo4j://localhost:7687'
        self.test_user = 'neo4j'
        self.test_password = 'password'

    def test_init(self) -> None:
        with patch('neo4j.GraphDatabase.driver') as mock_driver:
            fst = FileSystemTree(self.test_uri, self.test_user, self.test_password)
            mock_driver.assert_called_once_with(self.test_uri, auth=(self.test_user, self.test_password))

    def test_ready(self) -> None:
        with patch('builtins.print') as mock_print:
            with FileSystemTree(self.test_uri, self.test_user, self.test_password) as fst:
                fst.ready()
                mock_print.assert_called_once_with('started neo4j')

    def test_close(self) -> None:
        with patch('neo4j.GraphDatabase.driver') as mock_driver:
            with FileSystemTree(self.test_uri, self.test_user, self.test_password) as fst:
                fst.close()
                fst.driver.close.assert_called_once()

    def test_print_greeting(self) -> None:
        test_message = 'Hello, World!'
        with patch('neo4j.GraphDatabase.driver') as mock_driver:
            session = MagicMock()
            mock_driver.return_value.session.return_value.__enter__.return_value = session
            fst = FileSystemTree(self.test_uri, self.test_user, self.test_password)
            with patch('builtins.print') as mock_print:
                fst.print_greeting(test_message)
                session.execute_write.assert_called_once()
                mock_print.assert_called_once()


