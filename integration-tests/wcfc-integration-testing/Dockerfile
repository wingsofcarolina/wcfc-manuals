# Use Ubuntu 24.04 (Noble Numbat) as base
FROM ubuntu:24.04

# -------------------
# Version pins
# -------------------
ENV JAVA_VERSION=21
ENV MONGODB_VERSION=8.0
ENV WIREMOCK_VERSION=3.12.1
ENV PLAYWRIGHT_VERSION=1.55.0

# -------------------
# System dependencies
# -------------------
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    gnupg \
    ca-certificates \
    openjdk-${JAVA_VERSION}-jre-headless \
    unzip \
    git \
    python3 \
    python3-pip \
    python3-venv \
    python3-pymongo \
    python3-requests \
    supervisor

# -------------------
# Install MongoDB
# -------------------
RUN curl -fsSL https://www.mongodb.org/static/pgp/server-${MONGODB_VERSION}.asc \
    | gpg --dearmor -o /usr/share/keyrings/mongodb-server-${MONGODB_VERSION}.gpg \
    && echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-${MONGODB_VERSION}.gpg ] https://repo.mongodb.org/apt/ubuntu noble/mongodb-org/${MONGODB_VERSION} multiverse" \
    | tee /etc/apt/sources.list.d/mongodb-org-${MONGODB_VERSION}.list \
    && apt-get update && apt-get install -y mongodb-org
RUN mkdir -p /data/db && chown -R mongodb:mongodb /data/db

# -------------------
# Install WireMock
# -------------------
RUN curl -L "https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/${WIREMOCK_VERSION}/wiremock-standalone-${WIREMOCK_VERSION}.jar" \
    -o /usr/local/bin/wiremock.jar

# -------------------
# Install Playwright (keep existing venv for compatibility)
# -------------------
ENV VENV_PATH=/usr/local/playwright/venv
RUN python3 -m venv --system-site-packages ${VENV_PATH}
RUN ${VENV_PATH}/bin/pip install --upgrade pip
RUN ${VENV_PATH}/bin/pip install playwright==${PLAYWRIGHT_VERSION} pymongo requests
RUN ${VENV_PATH}/bin/playwright install --with-deps
RUN ln -s ${VENV_PATH}/bin/playwright /usr/local/bin/playwright

# -------------------
# Supervisor setup
# -------------------
RUN mkdir -p /var/log/supervisor
COPY supervisord.conf /etc/supervisord.conf

# -------------------
# Add entrypoint script
# -------------------
COPY entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD []

