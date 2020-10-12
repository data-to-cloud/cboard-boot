FROM openjdk:8-jre-alpine

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV  TIME_ZONE Asiz/Shanghai
####设置时区
RUN apk add --no-cache tzdata  \
&& echo "Asia/Shanghai" > /etc/timezone \
&& ln -sf /usr/share/zoneinfo/${TIME_ZONE} /etc/localtime


VOLUME /tmp

ADD ./target/cboard-0.0.1-SNAPSHOT.jar /cboard.jar
ADD ./src/main/resources/template /template

EXPOSE 8089

ENTRYPOINT ["java","-XX:+UnlockExperimentalVMOptions","-XX:+UseCGroupMemoryLimitForHeap","-Djava.security.egd=file:/dev/./urandom","-jar","/cboard.jar"]