FROM openjdk:8

RUN apt-get update && apt-get install -y maven

RUN apt install -y protobuf-compiler

COPY . /usr/src/app

WORKDIR /code

ADD . /code

RUN ["mvn", "clean"]
RUN ["mvn", "install"]

EXPOSE 8070

CMD ["java", "-jar", "target/agent-dialogue-core-0.1-SNAPSHOT.jar", "https://storage.googleapis.com/agent-dialogue-system/agent-dialogue-storage/core-config.json"]
