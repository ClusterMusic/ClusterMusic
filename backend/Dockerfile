FROM eclipse-temurin:11-jdk-jammy AS builder

RUN apt-get update && apt-get install -y \
    curl \
    wget \
    && rm -rf /var/lib/apt/lists/*

RUN wget -O /tmp/sbt.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-1.9.7.deb \
    && dpkg -i /tmp/sbt.deb \
    && rm /tmp/sbt.deb

WORKDIR /app

COPY build.sbt ./
COPY project/ ./project/
COPY conf/ ./conf/

COPY app/ ./app/
COPY public/ ./public/

RUN sbt clean compile stage

FROM eclipse-temurin:11-jre-jammy

RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

COPY --from=builder /app/target/universal/stage /app

RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 9000

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:9000/ || exit 1

CMD ["bin/cluster-dummy"] 