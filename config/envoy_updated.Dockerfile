FROM envoyproxy/envoy:v1.19-latest

COPY ./envoy.yaml /etc/envoy/envoy.yaml

RUN cat ./certs/fullchain.pem
COPY ./certs/fullchain.pem /etc/envoy/certs/fullchain.pem

RUN cat ./certs/privkey.pem
COPY ./certs/privkey.pem /etc/envoy/certs/privkey.pem

ENV ENVOY_UID=0

CMD /usr/local/bin/envoy -c /etc/envoy/envoy.yaml