FROM envoyproxy/envoy:v1.19-latest

ARG envoy_config_file=./envoy.yaml
COPY ${envoy_config_file} /etc/envoy/envoy.yaml
ARG envoy_ssl_cert
ARG envoy_ssl_privkey
COPY ${envoy_ssl_cert} /etc/envoy/cert.pem
COPY ${envoy_ssl_privkey} /etc/envoy/privkey.pem

ENV ENVOY_UID=0

CMD /usr/local/bin/envoy -c /etc/envoy/envoy.yaml
