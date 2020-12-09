var myName;
var gamePhase;
var players; // list of all players in the room
var attendees; // list of players currently in game (still alive)
var allAttendees; // list of players at start of a game (alive + dead)
var mover;
var playerStack;
var gameStack;
var changeStackAllowed;
var knockAllowed;
var attendeesStackDesks = [];
var coveredCard = {color: -1, value: -1};
var coveredStack = [coveredCard, coveredCard, coveredCard];
var swapSelection = {myStackId: -1, gameStackId: -1};
var messageInProgress;
var webradioStateLoaded = false; // get webradio state only the first time 

function onDocumentReady() {
    $("#loginConsole").text("Anmeldung vorbereiten...");
    createCards(function () {
        $("#connectBtn").prop("disabled", false);
        $("#loginConsole").html("&nbsp;");
    });
    setGameDialogVisible($("#joinInOutDialog"), false);
    setGameDialogVisible($("#dealCardsDialog"), false);
    setGameDialogVisible($("#selectDealerStackDialog"), false);
    setGameDialogVisible($("#discoverDialog"), false);
    setGameDialogVisible($("#knockDialog"), false);
    setGameDialogVisible($("#passDialog"), false);
    setGameDialogVisible($("#dealerStackSelectedDialog"), false);

    $('#pName').focus();
    var loginOnEnter = function (e) {
        if (e.keyCode === 13) {
            login();
        }
    };
    $('#pName').keyup(loginOnEnter);
    $('#pPwd').keyup(loginOnEnter);
    $('#chatMessage').keyup(function (e) {
        if (e.keyCode === 13) {
            sendChatMessage();
        }
    });
}

function onMessageBuffer() {
//    log("messageInProgress: " + messageInProgress + ", messageBuffer: " + messageBuffer.length);
    while (!messageInProgress && messageBuffer.length > 0) {
        messageInProgress = true;
        var action = messageBuffer.shift();
        try {
            action();
        } catch (e) {
            messageInProgress = false;
        }
//        log("messageInProgress: " + messageInProgress + ", messageBuffer: " + messageBuffer.length);
    }
}

function onGameState(message) {
    gamePhase = message.phase;
    players = message.playerList.players;
    attendees = message.attendeeList.attendees;
    allAttendees = message.attendeeList.allAttendees;
    mover = message.mover;
    gameStack = message.gameStack.cards;
    playerStack = message.playerStack.cards;
    changeStackAllowed = message.changeStackAllowed;
    knockAllowed = message.knockAllowed;
    updatePlayerList();
    updateAttendeeList();
    updateAttendeeStacks(message);
    var msg = mover === myName ? "" : "Warte auf " + mover;
    $("#discoverMessage").html(msg);
    $("#knockMessage").html(msg);
    $("#passMessage").html(msg);
    $("#stackSelectMessage").html(msg);
    onGamePhase(gamePhase);
    if (!webradioStateLoaded) {
        setWebRadioPlaying(message.webradioPlaying);
        webradioStateLoaded = true;
    }
}
function onAttendeeList(message) {
    mover = (message.mover !== undefined) ? message.mover : mover;
    messageInProgress = false;
    allAttendees = message.allAttendees;
    attendees = message.attendees;
    var myId = getMyAttendeeId();
    $("#addToAttendeesBtn").prop("disabled", (myId >= 0));
    $("#removeFromAttendeesBtn").prop("disabled", (myId < 0));
    updateAttendeeList();
    initDialogButtons();
}

function onPlayerStack(message) {
    messageInProgress = false;
    playerStack = message.cards;
    logStack("Player Stack", playerStack);
}

function onGamePhaseMessage(message) {
    mover = undefined;
    changeStackAllowed = message.changeStackAllowed;
    knockAllowed = message.knockAllowed;
    gamePhase = message.phase;
    mover = message.actor;
    switch (gamePhase) {
        case "shuffle":
            allAttendees = message.allAttendees;
            updateAttendeeStacks(undefined);
            onGamePhase(gamePhase);
            break;
        case "dealCards":
            updateAttendeeStacks(undefined);
            onGamePhase(gamePhase);
            break;
        case "waitForAttendees":
        case "waitForPlayerMove":
            updateAttendeeStacks(undefined);
            onGamePhase(gamePhase);
            break;
        case "moveResult":
            onMoveResult(message.moveResult);
            break;
        case "discover":
            onDiscover(message);
            break;
    }
}

function onGamePhase(phase) {
    var isDiscover = (phase === "discover");
    var isWaitForAttendees = (phase === "waitForAttendees");
    var meIsMover = (mover === myName);
    var meIsShuffling = (phase === "shuffle" && meIsMover);
    var meIsDealing = (phase === "dealCards" && meIsMover);
    var meIsMoverInGame = (phase === "waitForPlayerMove" && meIsMover);
    var isActive = isAttendee();
    var isValidSelection = swapSelection.myStackId >= 0 && swapSelection.gameStackId >= 0;
    setGameDialogVisible($("#joinInOutDialog"), isWaitForAttendees);
    setGameDialogVisible($("#dealCardsDialog"), meIsShuffling);
    setGameDialogVisible($("#selectDealerStackDialog"), meIsDealing);
    setGameDialogVisible($("#discoverDialog"), isDiscover);
    initDialogButtons();

    // Game Area
    if (isWaitForAttendees || phase === "shuffle") {
        emptyAllStackDesks();
    }
    updatePlayerList();
    updateControlPanelMessage();
    updateCardStack($("#gameStack"), gameStack);
    updateCardStack(attendeesStackDesks[getMyAllAttendeeId()], playerStack);
    updateAttendeeDeskColor();
    clearCardSelection();

    // Control Panel
    $("#logoffBtn").prop("disabled", !(isWaitForAttendees && isActive));
    $("#swapCardBtn").prop("disabled", !(meIsMoverInGame && isValidSelection));
    $("#swapAllCardsBtn").prop("disabled", !meIsMoverInGame);
    $("#passBtn").prop("disabled", !meIsMoverInGame);
    $("#knockBtn").prop("disabled", !(meIsMoverInGame && knockAllowed));
    $("#newCardsBtn").prop("disabled", !(meIsMoverInGame && changeStackAllowed));

    // Sounds
    if (phase === "shuffle") {
        sound.shuffle.loop = true;
        sound.shuffle.play();
    } else if (!sound.shuffle.paused) {
        sound.shuffle.pause();
    }
    if (phase === "dealCards") {
        sound.deal.play();
    }
    messageInProgress = false;
}

function initDialogButtons() {
    var isWaitForAttendees = (gamePhase === "waitForAttendees");
    var meIsMover = (mover === myName);
    if (isWaitForAttendees) {
        var myId = getMyAttendeeId();
        $("#addToAttendeesBtn").prop("disabled", (myId >= 0));
        $("#removeFromAttendeesBtn").prop("disabled", (myId < 0));
    }
    if (meIsMover) {
        $("#nextRoundBtn").show();
        $("#startGameBtn").prop("disabled", attendees.length < 2);
    } else {
        $("#nextRoundBtn").hide();
        $("#startGameBtn").prop("disabled", true);
    }
}

function playCoinSound(numCoin, numPlayed) {
    var time = 750;
    if (numPlayed === undefined) {
        numPlayed = 0;
    }
    if (numPlayed < numCoin) {
        var delayed = function () {
            sound.coin.pause();
            sound.coin.currentTime = 0;
            sound.coin.play();
            setTimeout(function () {
                playCoinSound(numCoin, ++numPlayed);
            }, time);
        };
        if (numPlayed === 0) {
            setTimeout(delayed, 0);
        } else {
            delayed();
        }
    }
}

function onDiscover(gamePhaseMessage) {
    updateAttendeeStacks(gamePhaseMessage);
    updateDiscoverMessageBox(gamePhaseMessage);
    $("#nextRoundBtn").prop("disabled", true);
    setGameDialogVisible($("#dealerStackSelectedDialog"), false);
    setGameDialogVisible($("#knockDialog"), false);
    setGameDialogVisible($("#passDialog"), false);
    onGamePhase(gamePhase);
    if (gamePhaseMessage.discoverMessage.finishKnocker !== undefined) {
        sound.knock2.play();
    } else if (gamePhaseMessage.discoverMessage.finisherScore === 33) {
        sound.fire.play();
    } else {
        sound.finish31.play();
    }

    // pay coins after a small time
    setTimeout(function () {
        // Update the attendees coins:
        if (gamePhaseMessage.discoverMessage.payers !== undefined) {
            // Process the number of coins to be payed 
            var numPayers = gamePhaseMessage.discoverMessage.payers !== undefined ? gamePhaseMessage.discoverMessage.payers.length : 0;
            var numLleavers = gamePhaseMessage.discoverMessage.leavers !== undefined ? gamePhaseMessage.discoverMessage.leavers.length : 0;
            var coins = numPayers - numLleavers;
            if (coins > 0) {
                playCoinSound(coins);
            } else if (gamePhaseMessage.discoverMessage.remainingAttendeesCount < 2) {
                setTimeout(function () {
                    sound.finishSound[gamePhaseMessage.discoverMessage.finishSoundId].play();
                }, 1000);
            }
            gamePhaseMessage.discoverMessage.payers.forEach(function (payer) {
                attendees[getAttendeeIdByName(payer)].gameTokens--;
            });
            updateAttendeeList();
            updateAttendeeStacks(gamePhaseMessage);
            $("#nextRoundBtn").prop("disabled", false);
        }
    }
    , 3000);
}

function onMoveResult(result) {
    if (result !== undefined) {
        var readyFunction = function () {
            gameStack = result.gameStack.cards;
            log("'" + mover + "': " + result.move);
            logStack("Game Stack", gameStack);
            logStack("Player Stack", playerStack);
            onGamePhase(gamePhase);
            onMessageBuffer();
        };
        switch (result.move) {
            case "selectStack":
                if (result.stackAction === 'keep') {
                    $("#stackSelectMessage").html((mover === myName ? "Du beh&auml;lst" : mover + " beh&auml;lt") + " die Karten");
                } else {
                    $("#stackSelectMessage").html((mover === myName ? "Du wechselst" : mover + " wechselt") + " die Karten");
                }
                sound.selectStack.play();
                animateGameDialog($("#dealerStackSelectedDialog"));
                readyFunction();
                break;
            case "swapCard":
                var takenId = result.cardSwap.stackIdTaken;
                var givenId = result.cardSwap.stackIdGiven;
                animateCardSwap(takenId, givenId, readyFunction);
                break;
            case "swapAllCards":
                animateCardSwap(0, 0);
                animateCardSwap(1, 1);
                animateCardSwap(2, 2, readyFunction);
                break;
            case "pass":
                var msg = (mover === myName ? "Du schiebst" : mover + " schiebt") + (result.count > 1 ? " mit" : "");
                if (result.count >= attendees.length) {
                    var nextMover = attendees[getNextAttendeeId(getAttendeeIdByName(mover))].name;
                    msg += "<br>" + (nextMover === myName ? "Du darfst" : nextMover + " darf") + " neue Karten nehmen";
                }
                log("MESSAGE: " + msg);
                $("#passMessage").html(msg);
                animateGameDialog($("#passDialog"));
                sound.pass.play();
                setTimeout(readyFunction, 1500);
                break;
            case "knock":
                if (result.count === 1) {
                    $("#knockMessage").html(mover === myName ? "Du klopfst" : mover + " klopft");
                    sound.knock1.play();
                    animateGameDialog($("#knockDialog"));
                }
                readyFunction();
                break;
            case "changeStack":
                animateStackChange(readyFunction);
                break;
            default:
                readyFunction();
        }
    } else {
        onGamePhase(gamePhase);
    }
}

function animateStackChange(readyFunction) {
    var speed = 2500;
    $("#swapCardBtn").prop("disabled", true);
    $("#swapAllCardsBtn").prop("disabled", true);
    $("#passBtn").prop("disabled", true);
    $("#knockBtn").prop("disabled", true);
    $("#newCardsBtn").prop("disabled", true);
    var svg1 = $($($("#gameStack").children()[0]).children(0));
    var svg2 = $($($("#gameStack").children()[1]).children(0));
    var svg3 = $($($("#gameStack").children()[2]).children(0));
    svg1.css("position", "absolute");
    svg2.css("position", "absolute");
    svg3.css("position", "absolute");
    var offX = $($($("#gameStack").children()[0]).children(0)).offset().left * 2.5;
    svg1.animate({left: "-=" + offX}, speed);
    svg2.animate({left: "-=" + offX}, speed);
    sound.newcards.play();
    svg3.animate({left: "-=" + offX}, speed, function () {
        if (typeof readyFunction === "function") {
            log("ANIMATE STACK CHANGE BEENDET");
            readyFunction();
        }
    });
}

function animateCardSwap(takenId, givenId, readyFunction) {
    var outSpeed = 2500;
    var inSpeed = 1500;
    $("#swapCardBtn").prop("disabled", true);
    $("#swapAllCardsBtn").prop("disabled", true);
    $("#passBtn").prop("disabled", true);
    $("#knockBtn").prop("disabled", true);
    $("#newCardsBtn").prop("disabled", true);
    var gameDeskWrapper = $($("#gameStack").children()[takenId]);
    var tSvg = $(gameDeskWrapper.children(0));
    var tPos = tSvg.offset();
    var attendeeDesk = attendeesStackDesks[getAllAttendeeIdByPlayerId(getPlayerIdByName(mover))];
    var attendeeDeskWrapper = $(attendeeDesk.children()[givenId]);
    var gSvg = $(attendeeDeskWrapper.children(0));
    var gPos = gSvg.offset();
    var offX = gPos.left - tPos.left;
    var offY = gPos.top - tPos.top;
    gSvg.css("position", "absolute");
    tSvg.css("position", "absolute");
    sound.click.play();
    tSvg.animate({top: "+=" + offY, left: "+=" + offX}, outSpeed, function () {
        gameDeskWrapper.append(gSvg);
        gSvg.animate({top: "+=" + offY, left: "+=" + offX}, 0, function () {
            gSvg.animate({top: "-=" + offY, left: "-=" + offX}, inSpeed, function () {
                if (typeof readyFunction === "function") {
                    sound.swap.play();
                    readyFunction();
                }
            });
        });
    });
}

function onPlayerList(message) {
    messageInProgress = false;
    players = message.players;
    updatePlayerList();
}

function onPlayerOnline(message) {
    messageInProgress = false;
    players.forEach(function (player) {
        if (player.name === message.name) {
            player.online = message.online;
        }
    });
    updatePlayerList();
    if (message.online) {
        sound.online.play();
    } else {
        sound.offline.play();
    }
}

function onChatMessage(message) {
    messageInProgress = false;
    var text = message.text;
    if (message.sender !== undefined) {
        text = message.sender + ": " + text;
    }
    var chatArea = $("#chatArea");
    if (chatArea.val()) {
        chatArea.append("\n");
    }
    chatArea.append(text);
    chatArea.scrollTop(chatArea[0].scrollHeight);
    sound.chat.play();
}

function logStack(name, stack) {
    var msg = name + ": ";
    if (stack !== undefined) {
        for (var i = 0; i < stack.length; i++) {
            msg = msg + (i + 1) + ":'" + card2String(stack[i].color, stack[i].value) + "' ";
        }
    }
    log(msg);
}

function isInAllAttendees() {
    return getMyAllAttendeeId() >= 0;
}

function getAllAttendeeIdByPlayerId(playerId) {
    if (allAttendees !== undefined) {
        for (var i = 0; i < allAttendees.length; i++) {
            if (allAttendees[i] === playerId) {
                return i;
            }
        }
    }
    return -1;
}

function getMyAllAttendeeId() {
    return getAllAttendeeIdByPlayerId(getMyPlayerId());
}

function isAttendee() {
    return getMyAttendeeId() >= 0;
}

function getMyPlayerId() {
    return  getIdByName(myName, players);
}

function getMyAttendeeId() {
    return  getAttendeeIdByName(myName);
}

function getAttendeeIdByName(name) {
    return getIdByName(name, attendees);
}

function getPlayerIdByName(name) {
    return getIdByName(name, players);
}

function getIdByName(name, aList) {
    if (aList !== undefined) {
        for (var i = 0; i < aList.length; i++) {
            if (name === aList[i].name) {
                return i;
            }
        }
    }
    return -1;
}

function getPlayerId(player) {
    return getIdByName(player.name, players);
}

function stackScoreToString(score) {
    if (score === 33)
        return "Feuer";
    if (score === 30.5)
        return "Drei&szlig;ig einhalb";
    return score;
}

function updateDiscoverMessageBox(message) {
    var discoverMsg = message.discoverMessage;
    var msgText = "";
    if (discoverMsg.finisher !== undefined) {
        msgText = (discoverMsg.finisher === myName ? ("Du hast ") : (discoverMsg.finisher + " hat ")) + stackScoreToString(discoverMsg.finisherScore);
    } else if (discoverMsg.finishKnocker !== undefined) {
        msgText = (discoverMsg.finishKnocker === myName) ? "Du machst zu" : (discoverMsg.finishKnocker + " macht zu");
    }
    if (discoverMsg.payers !== undefined) {
        msgText += "<br>";
        if (discoverMsg.payers.length === 1 && discoverMsg.payers[0] === myName) {
            msgText += "Du bezahlst";
        } else {
            for (var i = 0; i < discoverMsg.payers.length; i++) {
                msgText += (i > 0) ? (i < (discoverMsg.payers.length - 1) ? ", " : " und ") : "";
                msgText += discoverMsg.payers[i];
            }
            msgText += discoverMsg.payers.length > 1 ? " bezahlen" : " bezahlt";
        }
    }
    if (discoverMsg.leavers !== undefined) {
        msgText += "<br>";
        if (discoverMsg.leavers.length === 1 && discoverMsg.leavers[0] === myName) {
            msgText += "Du scheidest aus";
        } else {
            for (var i = 0; i < discoverMsg.leavers.length; i++) {
                msgText += (i > 0) ? (i < (discoverMsg.leavers.length - 1) ? ", " : " und ") : "";
                msgText += discoverMsg.leavers[i];
            }
            msgText += discoverMsg.leavers.length > 1 ? " scheiden aus" : " scheidet aus";
        }
    }
    $("#discoverMessage").html(msgText);
}

function updatePlayerList() {
    var panel = $("#playerListPanel");
    panel.empty();
    players.forEach(function (player) {
        panel.append("<div class='" + (player.online ? "playerOnline" : "playerOffline") + "'>" + player.name + '<br>&euro; ' + (0.5 * player.totalTokens).toFixed(2) + "</div>");
    });
}

function updateAttendeeList() {
    var panel = $("#attendeesPanel");
    panel.empty();
    attendeesStackDesks = [];
    if (attendees !== undefined) {
        var otherAttendeesCount = allAttendees.length - (isInAllAttendees() ? 1 : 0);
        var myId = getMyAllAttendeeId();
        var numPl = allAttendees.length;
        var step = (2 * Math.PI) / (numPl + ((myId < 0) ? 1 : 0));
        var angle = Math.PI / 2 + step;
        var id = myId;
        for (var i = 0; i < numPl; i++) {
            id = getNextAllAttendeeId(id);
            var name = players[ allAttendees[id] ].name;
            var attendeeId = getAttendeeIdByName(name)
            var token = attendeeId >= 0 ? attendees[attendeeId].gameTokens : -1;
            var child = $("<div class='attendeeDesk'></div>");
            var cardDesk = $("<div class='cardStack'></div>");
            child.append(cardDesk).append($("<div class='attendeeNameContainer'><div class='attendeeName'>" + name + "</div><div class='tokenImage" + token + "'></div></div>"));
            attendeesStackDesks[id] = cardDesk;
            panel.append(child);
            var isSmallSize = panel.width() < 720;
            var rx = panel.width() * (!isSmallSize ? 0.3 : 0.25);
            var ry = panel.height() * 0.25;
            var l = (panel.width() >> 1) + rx * Math.cos(angle) - (child.outerWidth() >> 1);
            var t = ((panel.height() >> 1) + ry * Math.sin(angle) - (child.outerHeight() * ((isSmallSize && otherAttendeesCount > 1) ? 1.5 : 1)));
            l = (100 / panel.width() * l) + "%";
            t = (100 / panel.height() * t) + "%";
            child.css({left: l, top: t});
            angle += step;
        }
        updateAttendeeDeskColor();
        if (getMyAttendeeId() < 0) {
            $("#addToAttendeesBtn").show();
            $("#removeFromAttendeesBtn").hide();
        } else {
            $("#addToAttendeesBtn").hide();
            $("#removeFromAttendeesBtn").show();
        }
    }
}

function updateAttendeeDeskColor() {
    if (attendees !== undefined) {
        var myId = getMyAllAttendeeId();
        var numPl = allAttendees.length;
        var id = myId;
        for (var i = 0; i < numPl; i++) {
            id = getNextAllAttendeeId(id);
            var attendee = players[allAttendees[id]];
            var className = (mover === attendee.name) ? "moverDesk" : "attendeeDesk";
            attendeesStackDesks[id].parent().prop("class", className);
        }
    }
}

function emptyAllStackDesks() {
    playerStack = undefined;
    gameStack = undefined;
    attendeesStackDesks.forEach(function (desk) {
        desk.empty();
    });
    $("#gameStack").empty();
}

function updateAttendeeStacks(message) {
    var discoverStacks = undefined;
    if (message !== undefined && gamePhase === "discover") {
        if (message.action === "gameState") {
            discoverStacks = message.discoverStacks;
        } else if (message.action === "gamePhase") {
            discoverStacks = message.discoverMessage.playerStacks;
        }
    }
    var myAllAttendeeId = getMyAllAttendeeId();
    var myDesk = myAllAttendeeId >= 0 ? attendeesStackDesks[myAllAttendeeId] : undefined;
    for (var i = 0; i < allAttendees.length; i++) {
        var desk = attendeesStackDesks[i];
        if (desk !== myDesk) {
            attendeeId = getAttendeeIdByName(players[allAttendees[i]].name);
            if (discoverStacks !== undefined) {
                updateCardStack(desk, (attendeeId >= 0) ? discoverStacks[ i ].cards : undefined);
            } else {
                updateCardStack(desk, (attendeeId >= 0) ? coveredStack : undefined);
            }
        } else {
            updateCardStack(desk, playerStack);
        }
    }
}

function updateCardStack(desk, cards) {
    if (desk !== undefined) {
        desk.empty();
        if (cards !== undefined && cards.length > 0) {
            var isCovered = (cards === coveredStack);
            for (var i = 0; i < cards.length; i++) {
                var svg = (isCovered) ? getSvgCard(cards[i]).getUI().clone() : getSvgCard(cards[i]).getUI();
                $(svg).css("position", "static");
                $(svg).css("top", "");
                $(svg).css("left", "");
                var cardWrapper = $("<div class='cardWrapper'></div>");
                cardWrapper.append(svg);
                desk.append(cardWrapper);
            }
        }
    }
}

function processCardClick(uiCard) {
    if (gamePhase === "waitForPlayerMove" && mover === myName) {

        // look in player stack:
        var id = findStackId(uiCard, playerStack);
        if (id >= 0) {
            var selected = (swapSelection.myStackId !== id);
            for (var i = 0; i < playerStack.length; i++) {
                getSvgCard(playerStack[i]).setSelected((i === id) ? selected : false);
            }
            swapSelection.myStackId = selected ? id : -1;
        } else {
            // look in game stack
            id = findStackId(uiCard, gameStack);
            if (id >= 0) {
                var selected = (swapSelection.gameStackId !== id);
                for (var i = 0; i < gameStack.length; i++) {
                    getSvgCard(gameStack[i]).setSelected((i === id) ? selected : false);
                }
                swapSelection.gameStackId = selected ? id : -1;
            }
        }
        var isValidSelection = swapSelection.myStackId >= 0 && swapSelection.gameStackId >= 0;
        $("#swapCardBtn").prop("disabled", !isValidSelection);
        log(swapSelection);
    }
}

function clearCardSelection() {
    if (playerStack !== undefined) {
        var playerCard = getSvgCard(playerStack[swapSelection.myStackId]);
        if (playerCard !== undefined) {
            playerCard.setSelected(false);
        }
    }
    if (gameStack !== undefined) {
        var gameCard = getSvgCard(gameStack[swapSelection.gameStackId]);
        if (gameCard !== undefined) {
            gameCard.setSelected(false);
        }
    }
    swapSelection.myStackId = -1;
    swapSelection.gameStackId = -1;
}


function findStackId(uiCard, stack) {
    if (stack !== undefined) {
        for (var i = 0; i < stack.length; i++) {
            if (uiCard.color === stack[i].color && uiCard.value === stack[i].value) {
                return i;
            }
        }
    }
    return -1;
}

function updateControlPanelMessage() {
    var msg = "";
    if (myName === mover) {
        msg = "Du bist dran";
    } else {
        if (mover !== undefined && mover !== "") {
            msg = mover + " ist dran";
        }
    }
    $("#controlPanelMessage").html(msg);
}

function getNextAttendeeId(currentId) {
    var id = currentId + 1;
    return id < attendees.length ? id : 0;
}

function getNextAllAttendeeId(currentId) {
    var id = currentId + 1;
    return id < allAttendees.length ? id : 0;
}

function animateGameDialog(dialog, readyFunction) {
    var time = 3000;
    setGameDialogVisible(dialog, true);
    setTimeout(function () {
        setGameDialogVisible(dialog, false);
        if (typeof readyFunction === "function") {
            readyFunction();
        }
    }, time);
}

function setGameDialogVisible(dialog, visible) {
    if (visible) {
        dialog.slideDown(fadePanelSpeed);
    } else {
        dialog.slideUp(fadePanelSpeed);
    }
}

function startGame() {
    var msg = {"action": "startGame"};
    webSocket.send(JSON.stringify(msg));
}

function startWaitForAttendees() {
    var msg = {"action": "waitForAttendees"};
    webSocket.send(JSON.stringify(msg));
}

function startDealing() {
    var msg = {"action": "dealCards"};
    webSocket.send(JSON.stringify(msg));
}

function nextRound() {
    var msg = {"action": "nextRound"};
    webSocket.send(JSON.stringify(msg));
}

function addToAttendees() {
    var msg = {"action": "addToAttendees"};
    webSocket.send(JSON.stringify(msg));
}

function removeFromAttendees() {
    var msg = {"action": "removeFromAttendees"};
    webSocket.send(JSON.stringify(msg));
}

function keepDealerStack() {
    var msg = {"action": "selectStack", "stack": "keep"};
    webSocket.send(JSON.stringify(msg));
}

function changeDealerStack() {
    var msg = {"action": "selectStack", "stack": "change"};
    webSocket.send(JSON.stringify(msg));
}

function swapCard() {
    var msg = {"action": "swapCard", "playerStack": swapSelection.myStackId, "gameStack": swapSelection.gameStackId};
    clearCardSelection();
    webSocket.send(JSON.stringify(msg));
}

function swapAllCards() {
    var msg = {"action": "swapAllCards"};
    webSocket.send(JSON.stringify(msg));
}

function pass() {
    var msg = {"action": "pass"};
    webSocket.send(JSON.stringify(msg));
}

function knock() {
    var msg = {"action": "knock"};
    webSocket.send(JSON.stringify(msg));
}

function getNewCards() {
    var msg = {"action": "changeStack"};
    webSocket.send(JSON.stringify(msg));
}

function sendChatMessage() {
    var msgField = $("#chatMessage");
    var msg = {"action": "chat", "text": msgField.val()};
    webSocket.send(JSON.stringify(msg));
    msgField.val("");
}