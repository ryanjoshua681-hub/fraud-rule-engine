# The jar is built on the host first (mvn package) — see start.bat. This image only packages it,
# so the build needs no network access to Maven Central and finishes in seconds.
# Eclipse Temurin 25 JRE: JRE-only image (smaller, no compiler) is all the runtime needs.
FROM eclipse-temurin:25-jre

WORKDIR /app

# Run as non-root. A numeric UID needs no named user and matches the k8s securityContext
# (runAsUser: 1000, runAsNonRoot: true in k8s/deployment.yaml).
COPY --chown=1000:1000 target/fraud-rule-engine-1.0.0.jar app.jar

USER 1000

EXPOSE 8080

# --enable-preview: required for Java 25 preview features (StructuredTaskScope)
# --enable-native-access: suppresses Unsafe deprecation warnings from transitive deps
# -XX:+UseZGC: low-pause garbage collector suitable for latency-sensitive services
# -XX:MaxRAMPercentage: let the JVM use up to 75% of the container's memory limit
ENTRYPOINT ["java",   "--enable-preview",   "--enable-native-access=ALL-UNNAMED",   "-XX:+UseZGC",   "-XX:MaxRAMPercentage=75.0",   "-jar", "app.jar"]
