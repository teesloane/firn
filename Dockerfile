FROM rust:1.47.0-slim as rustbuild
COPY rust /app/rust
WORKDIR /app/rust
RUN cargo build --release

FROM oracle/graalvm-ce:20.2.0-java11 as graalvm
ENV GRAALVM_VERSION=20.2.0 \
  JAVA_VERSION=11
RUN gu install native-image

FROM clojure:openjdk-15-lein-2.9.3-slim-buster as clojurebuild
COPY clojure /app/clojure
COPY --from=rustbuild /app/rust/target/release/libmylib.so /app/clojure/resources/
WORKDIR /app/clojure
COPY --from=graalvm /opt/graalvm-ce-java11-20.2.0/bin/native-image /usr/local/bin/native-image
RUN native-image -jar target/firn-0.0.5-SNAPSHOT-standalone.jar \
  -H:Name=firn \
  -H:+ReportExceptionStackTraces \
  -J-Dclojure.spec.skip-macros=true \
  -J-Dclojure.compiler.direct-linking=true \
  --initialize-at-build-time \
  --report-unsupported-elements-at-runtime \
  -H:IncludeResources=libmylib.dylib \
  -H:IncludeResources=libmylib.so \
  -H:IncludeResources=firn/.* \
  -H:Log=registerResource: \
  -H:ReflectionConfigurationFiles=reflection.json \
  -H:+JNI \
  --verbose \
  --allow-incomplete-classpath \
  --no-server

FROM ubuntu:20.04 as final
