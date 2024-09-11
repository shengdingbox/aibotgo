FROM d.wgets.org/openjdk:17

ENV LANG="en_US.UTF-8" \
    LANGUAGE="en_US:en" \
    LC_ALL="en_US.UTF-8" \
    TZ="Asia/Shanghai" \
    JAVA_OPTS="-Xms1024m -Xmx2048m -XX:+HeapDumpOnOutOfMemoryError"

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone


ADD ./target/*.jar  /data/app/aibotgo.jar

ENTRYPOINT ["sh","-c", "java $JAVA_OPTS -jar /data/app/aibotgo.jar --spring.profiles.active=$APP_PROFILE"]

EXPOSE 9090
