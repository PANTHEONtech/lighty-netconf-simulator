FROM eclipse-temurin:21-jre-alpine

WORKDIR /lighty-netconf-simulator

COPY maven/${lighty.app.zip} ./

RUN unzip ${lighty.app.zip} && \
    rm ${lighty.app.zip}

WORKDIR /lighty-netconf-simulator/${lighty.app.name}

ENTRYPOINT ["java", "-jar", "${lighty.app.jar}"]