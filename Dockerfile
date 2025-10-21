ARG VERSION="22.2.0-SNAPSHOT"

FROM maven:3.9-eclipse-temurin-21-alpine as build

ARG VERSION

WORKDIR /lighty-netconf-simulator

COPY . ./

RUN mvn install -DskipTests

WORKDIR /lighty-netconf-simulator/examples/devices/lighty-network-topology-device

RUN unzip target/lighty-network-topology-device-$VERSION-bin.zip -d target/

RUN mv target/lighty-network-topology-device-$VERSION target/lighty-network-topology-device
RUN mv target/lighty-network-topology-device/lighty-network-topology-device-$VERSION.jar target/lighty-network-topology-device/lighty-network-topology-device.jar

FROM eclipse-temurin:21-jre-alpine

ARG VERSION

COPY --from=build /lighty-netconf-simulator/examples/devices/lighty-network-topology-device/target/lighty-network-topology-device /app/target
COPY --from=build /lighty-netconf-simulator/examples/devices/lighty-network-topology-device/src/main/resources /app/target/resources

WORKDIR /app/target

EXPOSE 17380

ENTRYPOINT ["java", "-jar", "lighty-network-topology-device.jar"]