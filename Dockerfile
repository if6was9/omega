FROM eclipse-temurin:21 AS build


RUN apt update -y 
RUN apt install -y ca-certificates curl gnupg lsb-release 
RUN install -m 0755 -d /etc/apt/keyrings 
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg |  gpg --dearmor -o /etc/apt/keyrings/docker.gpg 
RUN chmod a+r /etc/apt/keyrings/docker.gpg 
RUN echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" |  tee /etc/apt/sources.list.d/docker.list > /dev/null

RUN apt update -y


RUN apt install -y docker-ce-cli

RUN /usr/bin/docker
RUN /usr/libexec/docker/cli-plugins/docker-compose

RUN target/*.jar | head -1

FROM eclipse-temurin:21
COPY --from=build /usr/bin/docker /usr/local/bin/docker
COPY --from=build /usr/libexec/docker/cli-plugins/docker-compose /usr/libexec/docker/cli-plugins/docker-compose
WORKDIR /omega
COPY target/stage/lib/ /omega/lib/
COPY target/stage/app.jar /omega/lib/
CMD ["java","-cp","lib/*","omega.App"]