FROM openjdk:11

ENV SBT_VERSION 1.6.1

RUN curl -L -o sbt-$SBT_VERSION.zip https://github.com/sbt/sbt/releases/download/v1.6.1/sbt-$SBT_VERSION.zip
RUN unzip sbt-$SBT_VERSION.zip -d ops

WORKDIR /voting_bot

COPY . /voting_bot

CMD /ops/sbt/bin/sbt run
