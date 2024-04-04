# https://edu.chainguard.dev/chainguard/chainguard-images/reference/jre
FROM chainguard/jre:latest 
ENV JAVA_OPTS=""
ENV BMRG_HOME=/opt/boomerang
ENV BMRG_SVC=service-flow

WORKDIR $BMRG_HOME
COPY ./target/$BMRG_SVC.jar service.jar
# RUN sh -c 'touch /service.jar'

# Create user, chown, and chmod. 
# OpenShift requires that a numeric user is used in the USER declaration instead of the user name
# RUN chmod -R u+x $BMRG_HOME \
#    && chgrp -R 0 $BMRG_HOME \
#    && chmod -R g=u $BMRG_HOME
# USER 2000

EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "./service.jar", "-Djava.security.egd=file:/dev/./urandom", "$JAVA_OPTS" ]
