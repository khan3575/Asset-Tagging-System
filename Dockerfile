FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --chown=app:app target/*.war app.war
USER app
EXPOSE 8080
ENTRYPOINT ["java", \
  "-Xmx384m", "-Xss512k", \
  "-XX:MaxMetaspaceSize=192m", \
  "-XX:+UseSerialGC", \
  "-XX:TieredStopAtLevel=1", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.war"]
