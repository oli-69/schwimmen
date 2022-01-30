var webSocket;
var fadePanelSpeed = 500;
var messageBuffer = [];
var questionMessageBuffer = [];
var videoWindow;
var videoRoomName = "";

$(document).ready(function () {
    $("#loginOptionsSwitch").change(onLoginOptionsSwitchChange);
    $("#loginOptionsSwitch").prop("checked", false);
    onLoginOptionsSwitchChange();
});
function onLoginOptionsSwitchChange() {
    var selected = $(loginOptionsSwitch).is(":checked");
    $("#loginImageGame").attr("src", (selected ? "login-game-bw.png" : "login-game.png"));
    $("#loginImageVideo").attr("src", (selected ? "login-video.png" : "login-video-bw.png"));
    $("#loginOptionGame").css("opacity", (selected ? 0.3 : 1.0));
    $("#loginOptionVideo").css("opacity", (selected ? 1.0 : 0.3));
}

function selectLoginOptionsSwitch(checked) {
    $("#loginOptionsSwitch").prop("checked", checked);
    onLoginOptionsSwitchChange();
}

function openVideo() {
    if (videoWindow === undefined || videoWindow.closed === true) {
        videoWindow = window.open(getVideoUrl(), "Video", width = 320, height = 320);
    }
}

function getVideoUrl() {
    var params = "userInfo.displayName=\"" + myName + "\"&config.prejoinPageEnabled=false";
//    return "https://meet.jit.si/" + videoRoomName + "#" + params;
//    changed Jitsi Server Name for German Vodafone customer
    return "https://meet.ffmuc.net/" + videoRoomName + "#" + params;
}

function logoff() {
    if (webSocket.readyState === WebSocket.OPEN) {
        msg = {
            "action": "logoff"
        };
        console.log("Logoff");
        webSocket.send(JSON.stringify(msg));
        webSocket.close();
        webSocket = undefined;
    }
}

function login() {
    initAudio(); // do this within user interaction scope
    time = 1000;
    tries = 6;
    count = 0;
    logf = function () {
        count++;
        if (count > tries) {
            $("#loginConsole").text("Fehler: Server antwortet nicht.");
            $("#connectBtn").prop("disabled", false);
            webSocket = undefined;
            return;
        }
        $("#connectBtn").prop("disabled", true);
        if (webSocket === undefined) {
            if (count > 1) {
                $("#loginConsole").text("Starte Anmeldung...(" + count + "/" + tries + ")");
            } else {
                $("#loginConsole").text("Starte Anmeldung...");
            }
            connect();
            setTimeout(logf, time);
        } else {
            if (webSocket.readyState === WebSocket.OPEN) {
                msg = {
                    "action": "login",
                    "name": $("#pName").val(),
                    "pwd": $("#pPwd").val()
                };
                myName = msg.name;
                console.log("Login with name '" + msg.name + "'");
                webSocket.send(JSON.stringify(msg));
            } else {
                log("Waiting for server connect...");
                setTimeout(logf, time);
            }
        }
    };
    setTimeout(logf, 0);
}

function onLoginSuccess(message) {
    $("#loginConsole").text("Anmeldung erfolgreich");
    videoRoomName = message.videoRoomName;
    if ($(loginOptionsSwitch).is(":checked")) {
        // open video room
        closeSocket();
        window.location = getVideoUrl();
    } else {
        // start gaming
        onRadioListChanged(message.radioList)
        setLoginPageVisible(false);
    }
}

function setLoginPageVisible(visible) {
    $("#loginConsole").text("").append("&nbsp;");
    if (visible) {
        $("#loginPage").show(fadePanelSpeed);
        setWebRadioPlaying(false);
        setShuffling(false);
    } else {
        $("#loginPage").hide(fadePanelSpeed);
    }
}

function onLoginError(error) {
    messageInProgress = false;
    var text;
    switch (error) {
        case "badPwd":
            text = "Fehler: falsches Passwort";
            break;
        case "badUser":
            text = "Fehler: Benutzer unbekannt";
            break;
        case "internalServerError":
        default:
            text = "Fehler: interner Serverfehler";
            break;
    }
    log("Login Error: " + error);
    log(text);
    $("#connectBtn").prop("disabled", false);
    $("#loginConsole").text(text);
}

function onServerMessage(data) {
    log("Message: " + (data));
    var message = JSON.parse(data);
    switch (message.action) {
        case "playWebradio":
            setWebRadioPlaying(message.play);
            break;
        case "radioUrl":
            setWebRadioUrl(message.url);
            updateRadioList(message.url);
            break;
        case "loginSuccess":
            onLoginSuccess(message);
            break;
        case "playerList":
            messageBuffer.push(function () {
                onPlayerList(message);
            });
            break;
        case "playerOnline":
            messageBuffer.push(function () {
                onPlayerOnline(message);
            });
            break;
        case "chatMessage":
            messageBuffer.push(function () {
                onChatMessage(message);
            });
            break;
        case "loginError":
            messageBuffer.push(function () {
                onLoginError(message.text);
            });
            break;
        case "gameState":
            messageBuffer.push(function () {
                onGameState(message);
            });
            break;
        case "attendeeList":
            messageBuffer.push(function () {
                onAttendeeList(message);
            });
            break;
        case "gamePhase":
            messageBuffer.push(function () {
                onGamePhaseMessage(message);
            });
            break;
        case "gameRules":
            messageBuffer.push(function () {
                onGameRules(message);
            });
            break;
        case "playerStack":
            messageBuffer.push(function () {
                onPlayerStack(message);
            });
            break;
        case "askForCardView":
            questionMessageBuffer.push(function () {
                onAskForCardView(message);
            });
            onQuestionMessageBuffer();
            break;
        case "askForCardShow":
            questionMessageBuffer.push(function () {
                onAskForCardShow(message);
            });
            onQuestionMessageBuffer();
            break;
        case "viewerMap":
            messageBuffer.push(function () {
                onViewerMap(message);
            });
            break;
        case "ping":
            // ignore
            break;
        default:
            log("Unprocessed Message: " + data);
    }
    onMessageBuffer();
}

function connect() {
    // open the connection if one does not exist
    if (webSocket !== undefined && webSocket.readyState !== WebSocket.CLOSED) {
        return;
    }
    // Create a websocket
    var loc = window.location, uri;
    if (loc.protocol === "https:") {
        uri = "wss:";
    } else {
        uri = "ws:";
    }
    uri += "//" + loc.host;
    uri += loc.pathname + "schwimmen/ws";
    webSocket = new WebSocket(uri);

    webSocket.onopen = function (event) {
        log("Connected!");
    };

    webSocket.onmessage = function (event) {
        onServerMessage(event.data);
    };

    webSocket.onclose = function (event) {
        log("Connection Closed");
        webSocket = undefined;
        $("#connectBtn").prop("disabled", false);
        $("#logoffBtn").prop("disabled", true);
        setLoginPageVisible(true);
    };
}

function closeSocket() {
    webSocket.close();
}


function log(text) {
    if (!true) {
        console.log(text);
    }
}

function warn(text) {
    console.warn(text);
}

function error(text) {
    console.error(text);
}
