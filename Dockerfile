FROM openjdk

MAINTAINER Adam Tkaczyk (adamtkaczyk90@gmail.com)

WORKDIR /app

COPY build/libs/ProvAppAuthenticator-0.1.0.jar /app

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "ProvAppAuthenticator-0.1.0.jar"]
