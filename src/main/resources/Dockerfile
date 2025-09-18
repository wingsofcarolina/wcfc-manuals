FROM azul/zulu-openjdk-alpine:21-latest

RUN apk update && apk add --no-cache curl less tzdata

ENV TZ=America/New_York

COPY . /opt/app/
RUN chmod +x /opt/app/entrypoint.sh

WORKDIR /opt/app

ENTRYPOINT ["/opt/app/entrypoint.sh"]
