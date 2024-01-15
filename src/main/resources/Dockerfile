FROM azul/zulu-openjdk-alpine:19-latest

RUN apk update && apk add --no-cache curl less

COPY . /opt/app/
RUN chmod +x /opt/app/entrypoint.sh

RUN mkdir /root/.aws
COPY ./credentials-wcfc /root/.aws/credentials

WORKDIR /opt/app

ENTRYPOINT ["/opt/app/entrypoint.sh"]
