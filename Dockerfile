FROM adoptopenjdk/openjdk11-openj9:jdk-11.0.5_10_openj9-0.17.0-alpine-slim
ARG BMRG_TAG
VOLUME /tmp
EXPOSE 7730
ADD target/service-flow-$BMRG_TAG.jar service.jar
RUN sh -c 'touch /service.jar'
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /service.jar" ]
