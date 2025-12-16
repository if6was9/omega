FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/stage/lib/ /app/lib/
COPY target/stage/app.jar /app/lib/
CMD ["java","-cp","lib/*","vfrmap.App"]



