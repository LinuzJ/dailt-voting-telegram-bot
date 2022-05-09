#!/bin/bash

# FIRST argument: your personal telegram token
TELEGRAM_TOKEN=$1;
# SECOND argument: want to run on pi? either empty or pi
isPI=$2;
# THIRD argument: detatch container? empty or -d
detatch=$3;
if ! [ $detatch = -d  ]
then
    detatch=""
fi

compile () {
    sbt assembly
}
build () {
    sudo docker build --build-arg TELEGRAM_TOKEN_ARG=$TELEGRAM_TOKEN -t voting-bot .
}
build_pi () {
    sudo docker build --build-arg TELEGRAM_TOKEN_ARG=$TELEGRAM_TOKEN -t voting-bot -build-arg _IMAGE=arm32v7/adoptopenjdk:11-jre-hotspot .
}
run_docker () {
    sudo docker run $detatch --name voting-bot --network="host" voting-bot
}

run_main () {
    if [ $isPI = pi ]
    then
        echo "Compliling sbt file.."
        compile
        echo "Building docker image for pi"
        build_pi
        echo "Starting it up!"
        run_docker
    else

        echo "Compliling sbt file.."
        compile
        echo "Building docker image"
        build
        echo "Starting it up!"
        run_docker
    fi
}


if [ $( sudo docker ps | grep polls-db | wc -l ) -gt 0 ]; then
    run_main
else
    printf "\n\nNo database found, please start the database before starting the main bot\n\n"
fi

