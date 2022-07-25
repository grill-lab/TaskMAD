# Pull official Java image
FROM openjdk:11

ENV JAVA_HOME="/usr/local/openjdk-11"
ENV JVM_PATH="/usr/local/openjdk-11/lib/server/libjvm.so"


# Gcloud utilities
RUN curl -sSL https://sdk.cloud.google.com | bash
ENV PATH $PATH:/root/google-cloud-sdk/bin

# Install Python dependencies
RUN apt-get clean
RUN apt-get update
RUN apt-get install python3.6 --yes
RUN apt-get install python3-pip --yes
RUN pip3 install --upgrade pip

# Install Rust dependencies
RUN apt-get install rustc --yes
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain nightly
ENV PATH="/root/.cargo/bin:${PATH}"
RUN rustup update
ENV CARGO_NET_GIT_FETCH_WITH_CLI=true

# Copy API files and install requirements
WORKDIR /usr/api
COPY /requirements.txt . 
RUN pip3 install -r requirements.txt
RUN mkdir data
COPY main.py .
COPY utils.py . 

EXPOSE 5050

CMD ["python3", "main.py", "deploy"]
