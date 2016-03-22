# Microservice - Pipeline - HTTP

This demo illustrates the pipeline pattern (a service calling another service, calling another service).

The demo is composed by four services: A -> B -> C -> D. All services are exposing a simple REST API, returning a 
json object. The json object is extended by all services. So, the result of A has 4 entries.

## Compilation

Launch `mvn clean install`

## Running locally

### Launch A

TODO - Add local cluster.xml 

````
cd A
java \
  -Djava.net.preferIPv4Stack=true \
  -jar target/pipeline-http-A-1.0-SNAPSHOT-fat.jar \
 -conf src/main/config/config.json \
 -cluster \ 
 -cp ../etc
````

Then open a browser:

````
open http://localhost:8080/assets/index.html
````

### Launch B

````
cd B
java -Djava.net.preferIPv4Stack=true \ 
 -jar target/pipeline-http-B-1.0-SNAPSHOT-fat.jar \
 -conf src/main/config/config.json \
 -cluster \ 
 -cp ../etc
````

### Launch C

````
cd C
java -Djava.net.preferIPv4Stack=true \ 
    -jar target/pipeline-http-C-1.0-SNAPSHOT-fat.jar \
    -conf src/main/config/config.json \
    -cluster \ 
    -cp ../etc
````

### Launch D

````
cd D
java -Djava.net.preferIPv4Stack=true \ 
    -jar target/pipeline-http-D-1.0-SNAPSHOT-fat.jar \
    -conf src/main/config/config.json \
    -cluster \ 
    -cp ../etc
````