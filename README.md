## Telegram bot made for scheduled voting in a group chats

### Short summary of bot functionality
- Gathers poll options from users during the given time period
- Creates polls
- Stores results in PostgreSQL database
- Supports multiple chats


### Built With

  - [Scala](http://www.scala-lang.org/) 2.13.7
  - [SBT](http://www.scala-sbt.org/)
  - [bot4s Telegram wrapper](https://github.com/bot4s/telegram) 5.4.2
  - [PostgreSQL](https://www.postgresql.org/)
  - [Docker](https://www.docker.com/)



## Usage

With the bot up and running and adde to a chat you can communicate with it using the following commands:
  - /initAdmin  &ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;  -> &ensp; Initializes your user as admin for the bot in all chats
  - /init       &ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;-> &ensp; Initializes the chat. Bot starts collecting polling events from this chat
  - /help   &ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;-> &ensp;Lists all available commands in a list similar to this
  - /addOption [arg]  &ensp;&ensp;&ensp;&ensp;&ensp;&ensp;-> Adds an your argument as an option for the current poll
  - /options &ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;-> Lists all current options in the current poll
  - /removeOption [index] -> Removes option number [index] from the current poll (index can be found from /options)
  - /polls  &ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;-> Lists all past and present polls in the current chat
  - /results  &ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp; -> Lists all past and present results from the current chat 

## Prerequisites

You will need to have **Scala**, **sbt** and **Docker** installed to run the project.
You will also need to have a telegram token YOUR_TELEGRAM_TOKEN from @botfather that you will use when running run.sh.

### Installation

To install and run locally please follow these steps

1. Clone the repo
   ```sh
   git clone https://github.com/LinuzJ/scheduled-voting-telegram-bot.git
   ```
2. Initialize database
   ```sh
   cd scheduled-voting-telegram-bot
   sudo sh db/create.sh -d
   ```
3. Run the bot
   ```sh
   sudo sh run.sh YOUR_TELEGRAM_TOKEN -d
   ```
Syntax for bash scripts:

```sh
sudo sh create.sh [-d]
```

```sh
sudo sh create.sh [YOUR_TELEGRAM_TOKEN] [-d]
```

You can run both ```create.sh``` and ```run.sh``` either with the tag ```-d``` after them to detach the container logs or not.


