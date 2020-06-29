## Home Keeper
**Home Keeper** is an IoT Framework for your home with a Multi-Agent Smart Alarm System on top of it.

### Prerequisites
The prerequisite are:
 * [Java jdk-8u221](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)
 * [MQTT broker](https://mosquitto.org/download/)
 * [Node.js](https://nodejs.org/en/)

### Installation
1. Clone the repository. ```git clone https://github.com/bmp-git/home-keeper```

#### mirror-home
1. Move into project directory.  ```cd mirror-home/```
2. Build and run application. ```gradlew run```

By default, the mirror-home service is available at http://localhost:8090.

Api available at: http://localhost:8090/api/home

#### home-viewer
1. Move into project directory.  ```cd home-viewer/```
2. Install npm dependencies. ```npm install```
3. Launch the application. ```npm run serve```

#### home-agents
1. Move into project directory.  ```cd home-agents/```
2. Build and run application. ```gradlew run```

By default, home-agents connects to the mirror-home service at http://localhost:8090.

#### user-localizer
1. Move into project directory.  ```cd user-localizer/```
2. Install dependencies. ```pip install -r requirements.txt```
3. Launch the application. ```python main.py [PORT] [MAIL] [COOKIE_FILE]```

By default, the user-localizer is started by mirror-home on http://localhost:PORT.

#### scanner_esp32
1. Follow the instructions. https://docs.micropython.org/en/latest/esp32/tutorial/intro.html
2. Copy ```scanner_esp32/micropython/``` content to esp32 using [Thonny](https://thonny.org/)

#### scanner_esp8266
1. Install ESP8266 Add-on in Arduino IDE.
2. Open ```scanner_esp8266/scanner_esp8266.ino``` with Arduino IDE, compile and upload.

### Authors
Edoardo Barbieri, Lorenzo Mondani, Emanuele Pancisi
 
Developed as final project for [81615 - Pervasive Computing](https://www.unibo.it/it/didattica/insegnamenti/insegnamento/2019/412647) & [72529 - Smart City e Tecnologie Mobili](https://www.unibo.it/it/didattica/insegnamenti/insegnamento/2019/412645) courses (academic year 2019/2020).
