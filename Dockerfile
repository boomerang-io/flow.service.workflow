# https://edu.chainguard.dev/chainguard/chainguard-images/reference/jre
FROM chainguard/jre:latest 
ENV JAVA_OPTS=""
ENV BMRG_HOME=/opt/boomerang
ENV BMRG_SVC=service-flow

USER 2000
WORKDIR $BMRG_HOME
COPY ./target/$BMRG_SVC.jar service.jar
EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "./service.jar", "-Djava.security.egd=file:/dev/./urandom", "$JAVA_OPTS" ]
