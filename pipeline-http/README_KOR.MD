# Microservice - Pipeline - HTTP

이번 데모는 파이프라인 패턴에 대해서 설명할 예정입니다.(파이프라인 패턴이란 하나의 서비스가 또다른 서비스를 부르고, 그 서비스가 또 다른 서비스를 부르는 것을 말합니다.)

이 데모는 다음과 같은 구조로 4개의 서비스를 구성하고 있습니다: A -> B-> C-> D. 모든 서비스는 json 객체를 반환하는 간단한 REST API로 구성되어 있습니다.
서비스 A의 결과는 모든 서비스(A,B,C,D)로부터 json 객체를 가져오기 때문에 4개의 json 객체를 가지고 있을 것 입니다.

## 컴파일 방법

`mvn clean install`를 입력하세요.

## 로컬에서 구동하는 법

### 서비스 A 구동

우선 로컬에 cluster.xml 추가하세요

````
cd A
java \
  -Djava.net.preferIPv4Stack=true \
  -jar target/pipeline-http-A-1.0-SNAPSHOT-fat.jar \
 -conf src/main/config/config.json \
 -cluster \
 -cp ../etc
````

그리고 나서 브라우저를 실행시키세요:

````
open http://localhost:8080/assets/index.html
````

### 서비스 B 구동

````
cd B
java -Djava.net.preferIPv4Stack=true \
 -jar target/pipeline-http-B-1.0-SNAPSHOT-fat.jar \
 -conf src/main/config/config.json \
 -cluster \
 -cp ../etc
````

### 서비스 C 구동

````
cd C
java -Djava.net.preferIPv4Stack=true \
    -jar target/pipeline-http-C-1.0-SNAPSHOT-fat.jar \
    -conf src/main/config/config.json \
    -cluster \
    -cp ../etc
````

### 서비스 D 구동

````
cd D
java -Djava.net.preferIPv4Stack=true \
    -jar target/pipeline-http-D-1.0-SNAPSHOT-fat.jar \
    -conf src/main/config/config.json \
    -cluster \
    -cp ../etc
````
