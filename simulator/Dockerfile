FROM openjdk:8

RUN apt-get update && apt-get install -y maven

WORKDIR /code

ADD . /code

RUN ["mvn", "install"]

EXPOSE 8070 8080

CMD ["mvn", "jetty:run"]

