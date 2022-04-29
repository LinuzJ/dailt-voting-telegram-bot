#!/bin/bash

sudo docker build -t polls-db .

result=$(sudo docker ps -a | grep polls-db | wc -l  )

if [ $( sudo docker ps -a | grep polls-db | wc -l ) -gt 0 ]; then
  printf "\n\nContainer exists\nMake sure you want to build a new container. This means you will delete the existing database\n   To delete use: sudo docker rm polls-db"
else
  printf "\n\nNo such container, running new\n\n"
  sudo docker run -d --name polls-db -p 5432:5432 -e POSTGRES_HOST_AUTH_METHOD=trust polls-db
fi


