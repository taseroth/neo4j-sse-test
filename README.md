Neo4j plugin to resolve urls to their final destination (due to url shorteners). 
Resolved urls will result in an :Url node with a relation from the :Link and a :Site node.

Currently, this is work in process and reliable kills the database.

In https://github.com/taseroth/neo4j-sse-test/blob/master/src/main/java/org/faboo/example/twitter/sse/LinkResolver.java nodes of Label Link are queried and put into a queue. The queue is worked on from threads.
