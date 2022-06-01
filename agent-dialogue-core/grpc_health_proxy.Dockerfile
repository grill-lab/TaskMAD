FROM openjdk:8

WORKDIR /usr/local/bin
RUN git clone https://github.com/salrashid123/grpc_health_proxy.git
RUN curl -OL https://golang.org/dl/go1.16.7.linux-amd64.tar.gz
RUN tar -C /usr/local -xvf go1.16.7.linux-amd64.tar.gz
ENV PATH="/usr/local/go/bin:$PATH"
WORKDIR grpc_health_proxy
RUN go build -o grpc_health_proxy main.go
ENV PATH="/usr/local/bin/grpc_health_proxy:$PATH"

EXPOSE 8080

CMD grpc_health_proxy --http-listen-addr 0.0.0.0:8080 --grpcaddr 0.0.0.0:8070 --logtostderr=1 -v 1