FROM openjdk:8

RUN apt-get update && apt-get install -y maven

RUN apt install -y protobuf-compiler

COPY . /usr/src/app

WORKDIR /code

ADD . /code

RUN GRPC_HEALTH_PROBE_VERSION=v0.3.2 && \
    wget -qO/bin/grpc_health_probe https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/${GRPC_HEALTH_PROBE_VERSION}/grpc_health_probe-linux-amd64 && \
    chmod +x /bin/grpc_health_probe

RUN ["mvn", "clean"]
RUN ["mvn", "install"]

EXPOSE 8070

# allow a new URL to be passed with --build-arg config_url=<URL>
ARG config_url
ENV CONFIG_URL=${config_url:-"https://storage.googleapis.com/taskmad-public-bucket/core-config.json"}
CMD java -jar target/agent-dialogue-core-0.1-SNAPSHOT.jar ${CONFIG_URL}
