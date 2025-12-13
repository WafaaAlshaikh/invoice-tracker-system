FROM tomcat:10.1-jdk17

RUN rm -rf /usr/local/tomcat/webapps/*

COPY target/invoicetracker-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

RUN mkdir -p /usr/local/tomcat/webapps/uploads

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Dspring.profiles.active=prod -Dserver.port=8080 -Dspring.jmx.enabled=false"

EXPOSE 8080
CMD ["catalina.sh", "run"]