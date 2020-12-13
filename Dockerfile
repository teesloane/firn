FROM rust:1.47.0-slim as rustbuild
COPY rust /app/rust
WORKDIR /app/rust
RUN cargo build --release

FROM clojure:openjdk-11-lein-2.9.3-slim-buster as clojure
COPY clojure /app/clojure
WORKDIR /app/clojure
RUN lein do clean, uberjar

FROM oracle/graalvm-ce:20.2.0-java11 as graalvm
ENV GRAALVM_VERSION=20.2.0 \
  JAVA_VERSION=11
RUN gu install native-image
COPY clojure /app/clojure
COPY --from=rustbuild /app/rust/target/release/libmylib.so /app/clojure/resources/
COPY --from=clojure /app/clojure/target /app/clojure/target
WORKDIR /app/clojure
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

FROM openjdk:11 as final
RUN useradd -m user
WORKDIR /app/bin
COPY --from=graalvm /app/clojure/firn /app/bin/firn
COPY --from=rustbuild /app/rust/target/release/libmylib.so /home/user/.firn/libmylib.so
USER user
ENV PATH=$PATH:/app/bin
ENTRYPOINT ["/app/bin/firn"]
