from django.core.management.base import BaseCommand, CommandError
from neo4j import GraphDatabase

class Command(BaseCommand):
    help = 'Start Neo4j database'

    def handle(self, *args, **options):
        # Code to start the Neo4j database
        driver = GraphDatabase.driver('bolt://localhost:7687', auth=('neo4j', 'password'))
        session = driver.session()
        result = session.run('CREATE (n:Node {name: "example"}) RETURN n')
        print(list(result))
        session.close()
        driver.close()
        self.stdout.write(self.style.SUCCESS('Neo4j database started successfully.'))
