ARG VERSION="22.0.0-SNAPSHOT"

FROM maven:3.9-eclipse-temurin-21-alpine as build

ARG VERSION

WORKDIR /lighty-netconf-simulator

COPY . ./

RUN mvn install -DskipTests

WORKDIR /lighty-netconf-simulator/examples/devices/lighty-network-topology-device

RUN cp target/lighty-network-topology-device-$VERSION.jar target/lighty-network-topology-device.jar

EXPOSE 17380

ENTRYPOINT ["java", "-jar", "target/lighty-network-topology-device.jar"]
