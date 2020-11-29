# Schwimmen Online
### Online version of the card game "Schwimmen"

![Preview Image](/ext/preview.jpg)

During the corona pandemic and the associated contact restrictions, I decided to program the card game "Schwimmen" in an online version. "Swimming Online" enables contactless gaming with friends over the Internet.

The software requires an HTML-5 capable web browser. Desktop PCs are supported as well as notebooks and mobile devices such as tablets and smartphones (only recommended for up to 3 players). The user interface language is currently german only.

Jitsi meet is integrated as the video conference service (web app in browser as well as mobile app).

Java 8 is required on the server side. The server also needs a graphical user interface to open its main window. Not all control functions are available for the clients.
The server is based on the Jetty WebServer. All runtime and test libraries used are stored in the "ext" folder. The server side is created as a Netbeans project.

The client uses the SVG cards from https://github.com/htdebeer/SVG-cards. At this point I would like to thank everyone who contributed to this excellent work.

Please note: The html folder contains media files (images, sounds) that I was able to find freely on the Internet. No research was carried out on license rights.

This project is for private use only. Each publication must clarify the license terms of the referenced libraries and media files.

## Setup:
1. Download the software
2. Enter the players with user name and password in the users.properties file.
3. Enter the name of the Jitsi conference in the settings.properties file. The server port can also be changed here. The default setting is 8080.
4. Open a command line window and start the server by executing: java -jar Schwimmen.jar

To start the client open the page in a browser (e.g. http://localhost:8080).

Set up a DynDns service to make the server accessible from the Internet.
