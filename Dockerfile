ARG _IMAGE=openjdk:11
FROM $_IMAGE

ARG TELEGRAM_TOKEN_ARG

ENV token=$TELEGRAM_TOKEN_ARG

WORKDIR /voting_bot

COPY ./target/scala-2.13/votingBotExe.jar /voting_bot/votingBotExe.jar

CMD TELEGRAM_TOKEN=$token java -jar votingBotExe.jar
