var myName;
var gamePhase;
var players; // list of all players in the room
var attendees; // list of players currently in game (still alive)
var allAttendees; // list of players at start of a game (alive + dead)
var viewerMap = [0][0];
var mover;
var admin;
var playerStack;
var gameStack;
var gameStackOffsets = [];
var gameStackRotations = [];
var dealer2ndStackProps;
var cardFlips = [3];
var gameDesk;
var viewerStacks;
var controlPanel;
var changeStackAllowed;
var knockAllowed;
var passAllowed;
var attendeesCardStacks = [];
var dealer2ndStack;
var coveredCard = {color: -1, value: -1};
var coveredStack = [coveredCard, coveredCard, coveredCard];
var shufflingCard;
var swapSelection = {myStackId: -1, gameStackId: -1};
var messageInProgress;
var questionMessageInProgress;
var webradioStateLoaded = false; // get webradio state only the first time 
var askForViewerHashCode;
var adminWindow;
var radioList = [];
var playerPopup;

function onDocumentReady() {
    $("#loginConsole").text("Anmeldung vorbereiten...");
    createCards(function () {
        $("#connectBtn").prop("disabled", false);
        $("#loginConsole").html("&nbsp;");
    });
    gameDesk = $("#gameDesk");
    controlPanel = $("#controlPanel");
    shufflingCard = $(getSvgCard(coveredCard).getUI()).clone();
    adminWindow = $("#adminWindow");
    adminWindow.hide();
    setGameDialogVisible($("#joinInOutDialog"), false);
    setGameDialogVisible($("#dealCardsDialog"), false);
    setGameDialogVisible($("#selectDealerStackDialog"), false);
    setGameDialogVisible($("#discoverDialog"), false);
    setGameDialogVisible($("#knockDialog"), false);
    setGameDialogVisible($("#passDialog"), false);
    setGameDialogVisible($("#dealerStackSelectedDialog"), false);
    setGameDialogVisible($("#askForViewCardsDialog"), false);
    setGameDialogVisible($("#askForShowCardsDialog"), false);

    playerPopup = $("#playerPopupMenu");
    playerPopup.hide();
    $(document).click(function (e) {
        if (playerPopup.is(":visible")) {
            var target = $(e.target);
            var isplayerPopUp = target.parents().is(playerPopup) || target.is(playerPopup);
            if (!isplayerPopUp) {
                setPlayerPopupVisible(false);
            }
        }
    });

    // connect the enter key to the input fields.
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

    var layoutFunction = function () {
        updateAttendeeList();
        updateAttendeeStacks();
        updateCardStack($("#gameStack"), gameStack);
        setShuffling(false);
        onGamePhase(gamePhase);
    };
    $(window).on("orientationchange", function () {
        setTimeout(layoutFunction(), 1000);
    });
    $(window).resize(layoutFunction);
}

function onMessageBuffer() {
//    log("messageInProgress: " + messageInProgress + ", messageBuffer: " + messageBuffer.length);
    while (!messageInProgress && messageBuffer.length > 0) {
        messageInProgress = true;
        var action = messageBuffer.shift();
        try {
            action();
        } catch (e) {
            error("Fehler in onMessageBuffer -> " + action + " '" + e + "'");
            messageInProgress = false;
        }
//        log("messageInProgress: " + messageInProgress + ", messageBuffer: " + messageBuffer.length);
    }
}

function onQuestionMessageBuffer() {
    while (!questionMessageInProgress && questionMessageBuffer.length > 0) {
        questionMessageInProgress = true;
        var action = questionMessageBuffer.shift();
        try {
            action();
        } catch (e) {
            questionMessageInProgress = false;
        }
    }
}

function onGameState(message) {
    gamePhase = message.phase;
    players = message.playerList.players;
    attendees = message.attendeeList.attendees;
    allAttendees = message.attendeeList.allAttendees;
    mover = message.attendeeList.mover;
    admin = message.playerList.admin;
    viewerMap = message.viewerMap.table;
    gameStack = message.gameStack.cards;
    gameStackOffsets = message.gameStack.offset;
    gameStackRotations = message.gameStack.rotation;
    playerStack = message.playerStack.cards;
    viewerStacks = message.viewerStackList.viewerStacks;
    changeStackAllowed = message.changeStackAllowed;
    knockAllowed = message.knockAllowed;
    passAllowed = message.passAllowed;
    updatePlayerList();
    updateAttendeeList();
    updateAttendeeStacks(message);
    var msg = mover === myName ? "" : "Warte auf " + mover;
    $("#discoverMessage").html(msg);
    $("#knockMessage").html(msg);
    $("#passMessage").html(msg);
    $("#stackSelectMessage").html(msg);
    onGamePhase(gamePhase);
    setWebRadioUrl(message.radioUrl.url);
    updateRadioList(message.radioUrl.url);
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
    updateAdminWindow();
}

function onPlayerStack(message) {
    messageInProgress = false;
    playerStack = message.cards;
    logStack("Player Stack", playerStack);
}

function onAskForCardView(message) {
    askForViewerHashCode = message.hashCode;
    var msg = message.source + " m&ouml;chte bei Dir in die Karten schauen.";
    $("#askForViewCardsMessage").html(msg);
    sound.askview.play();
    setGameDialogVisible($("#askForViewCardsDialog"), true);
}

function onAskForCardShow(message) {
    askForViewerHashCode = message.hashCode;
    var msg = message.source + " m&ouml;chte Dir die Karten zeigen.";
    $("#askForShowCardsMessage").html(msg);
    sound.askview.play();
    setGameDialogVisible($("#askForShowCardsDialog"), true);
}

function onViewerMap(message) {
    messageInProgress = false;
    viewerMap = message.table;
    updateAttendeeList();
    updateAttendeeStacks();
}

function onGamePhaseMessage(message) {
    mover = undefined;
    changeStackAllowed = message.changeStackAllowed;
    knockAllowed = message.knockAllowed;
    passAllowed = message.passAllowed;
    gamePhase = message.phase;
    mover = message.actor;
    switch (gamePhase) {
        case "shuffle":
            allAttendees = message.allAttendees;
            updateAttendeeStacks(undefined);
            onGamePhase(gamePhase);
            break;
        case "dealCards":
            viewerStacks = message.viewerStackList.viewerStacks;
            updateAttendeeList();
            updateAttendeeStacks(undefined);
            sound.deal.play();
            animateDealCards(function () {
                onGamePhase(gamePhase);
                onMessageBuffer();
            });
            break;
        case "finish31OnDeal":
            updateAttendeeStacks(message);
            sound.finish31OnDeal.play();
            onGamePhase(gamePhase);
            break;
        case "waitForAttendees":
            viewerStacks = [0];
            updateAttendeeList();
            onGamePhase(gamePhase);
            break;
        case "waitForPlayerMove":
            updateAttendeeStacks(undefined);
            onGamePhase(gamePhase);
            break;
        case "moveResult":
            viewerStacks = message.viewerStackList.viewerStacks;
            onMoveResult(message.moveResult);
            break;
        case "discover":
            onDiscover(message);
            break;
    }
}

function onGamePhase(phase) {
    log("onGamePhase: " + phase);
    var isDiscover = (phase === "discover");
    var isWaitForAttendees = (phase === "waitForAttendees");
    var meIsMover = (mover === myName);
    var meIsShuffling = (phase === "shuffle" && meIsMover);
    var meIsDealing = ((phase === "dealCards" || phase === "finish31OnDeal") && meIsMover);
    var meIsMoverInGame = (phase === "waitForPlayerMove" && meIsMover);
    var isActive = isAttendee();
    var isValidSelection = swapSelection.myStackId >= 0 && swapSelection.gameStackId >= 0;
    setGameDialogVisible($("#joinInOutDialog"), isWaitForAttendees);
    setGameDialogVisible($("#dealCardsDialog"), meIsShuffling);
    setGameDialogVisible($("#selectDealerStackDialog"), meIsDealing);
    setGameDialogVisible($("#discoverDialog"), isDiscover);
    initDialogButtons();

    // reset all card selections and hovers
    resetUICards();

    // Game Area
    if (isWaitForAttendees || phase === "shuffle") {
        emptyAllStackDesks();
    }
    updatePlayerList();
    updateControlPanelMessage();
    updateCardStack($("#gameStack"), gameStack);
    updateCardStack(attendeesCardStacks[getMyAllAttendeeId()], playerStack);
    updateAttendeeDeskColor();
    updateAdminWindow();

    // Control Panel
    if (meIsMoverInGame) {
        controlPanel.show();
    } else {
        controlPanel.hide();
    }
    $("#logoffBtn").prop("disabled", !(isWaitForAttendees && isActive));
    $("#swapCardBtn").prop("disabled", !(meIsMoverInGame && isValidSelection));
    $("#swapAllCardsBtn").prop("disabled", !meIsMoverInGame);
    $("#passBtn").prop("disabled", !(meIsMoverInGame && passAllowed));
    $("#knockBtn").prop("disabled", !(meIsMoverInGame && knockAllowed));
    $("#newCardsBtn").prop("disabled", !(meIsMoverInGame && changeStackAllowed));

    setShuffling(phase === "shuffle");

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
        $("#startGameBtn").prop("disabled", attendees === null || attendees === undefined || attendees.length < 2);
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
        setTimeout(animateKnock, 100);
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
            // disabled here to cover empty payers/leavers when knock rules are disabled
            // empty payers/leavers happens, when all attendees have equal card values when swimming
            //    $("#nextRoundBtn").prop("disabled", false);
        }
        // Button is activated here, outside of if-clause
        $("#nextRoundBtn").prop("disabled", false);
    }
    , 3000);
}

function onMoveResult(result) {
    if (result !== undefined) {
        gameStackOffsets = result.gameStack.offset;
        gameStackRotations = result.gameStack.rotation;
        cardFlips = result.gameStack.cardFlips;
        var readyFunction = function () {
            try {
                gameStack = result.gameStack.cards;
                log("'" + mover + "': " + result.move);
                logStack("Game Stack", gameStack);
                logStack("Player Stack", playerStack);
                onGamePhase(gamePhase);
            } catch (e) {
                log("Exception in onMoveResult::readyFunction: '" + e + "'");
            }
            onMessageBuffer();
        };
        switch (result.move) {
            case "selectStack":
                var isKeepStack = result.stackAction === 'keep';
                if (isKeepStack) {
                    $("#stackSelectMessage").html((mover === myName ? "Du beh&auml;ltst" : mover + " beh&auml;lt") + " die Karten");
                } else {
                    $("#stackSelectMessage").html((mover === myName ? "Du wechselst" : mover + " wechselt") + " die Karten");
                }
                sound.selectStack.play();
                setGameDialogVisible($("#selectDealerStackDialog"), false);
                animateGameDialog($("#dealerStackSelectedDialog"));
                animateSelectStack(isKeepStack, function () {
                    dealer2ndStackProps = undefined;
                    dealer2ndStack = undefined;
                    updateAttendeeList();
                    readyFunction();
                });
                break;
            case "swapCard":
                var takenId = result.cardSwap.stackIdTaken;
                var givenId = result.cardSwap.stackIdGiven;
                animateCardSwap(takenId, givenId, readyFunction);
                break;
            case "swapAllCards":
                animateAllCardsSwap(readyFunction);
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
                    sound.knock1.play();
                    $("#knockMessage").html(mover === myName ? "Du klopfst" : mover + " klopft");
                    animateGameDialog($("#knockDialog"));
                    setTimeout(function () {
                        animateKnock(readyFunction);
                    }, 100);
                } else {
                    readyFunction();
                }
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

function animateKnock(readyFunction) {
    var gameStack = $("#gameStack");
    var level = Math.random();
    var bounceProps = {times: level * 3 + 4, distance: level * 8 + 2};
    var bounceSpeed = 200;
    $(gameStack.children()[0]).effect("bounce", bounceProps, bounceSpeed);
    $(gameStack.children()[1]).effect("bounce", bounceProps, bounceSpeed);
    $(gameStack.children()[2]).effect("bounce", bounceProps, bounceSpeed);
    var lastProps = bounceProps;
    if (typeof readyFunction === "function") {
        lastProps = {
            times: bounceProps.times,
            distance: bounceProps.distance,
            complete: readyFunction};
    }
    $("#attendeesPanel").effect("bounce", lastProps, bounceSpeed);

}

function setShuffling(isShuffling) {
    if (isShuffling) {
        sound.shuffle.loop = true;
        sound.shuffle.play();
        $("body").prepend(shufflingCard);
        var pos = getShuffleCardsPosition(shufflingCard);
        shufflingCard.css({position: "fixed", top: pos.top, left: pos.left});
        pos.stack.append(shufflingCard);
        var shakeTime = 1000;
        var shakes = 6;
        var shakeAmount = 2;
        function loopShake() {
            shufflingCard.effect("shake", {distance: shakeAmount, times: shakes}, shakeTime, loopShake);
        }
        loopShake();
    } else if (sound !== undefined && !(sound.shuffle.paused)) {
        sound.shuffle.pause();
        shufflingCard.stop();
        shufflingCard.remove();
    }
}

function getShuffleCardsPosition(card) {
    var id = getAllAttendeeIdByPlayerId(getPlayerIdByName(mover));
    var dealerStack = attendeesCardStacks[id];
    var off = dealerStack.offset();
    return {
        top: off.top + (dealerStack.height() - card.height()),
        left: off.left + (dealerStack.width() - card.width()) * 0.5,
        stack: dealerStack
    };
}

function animateDealCards(readyFunction) {
    setGameDialogVisible($("#dealCardsDialog"), false);
    var cards = [];
    var props = [];
    var dealerId = getAllAttendeeIdByPlayerId(getPlayerIdByName(mover));
    var id = dealerId;
    var numPlayers = allAttendees.length;
    var c = 0;
    var childs;
    var card = $(dealer2ndStack.children()[0]);
    var pos = getShuffleCardsPosition(card);
    for (var y = 0; y < 3; y++) {
        for (var i = 0; i < numPlayers; i++) {
            id = getNextAllAttendeeId(id);
            childs = attendeesCardStacks[id].children();
            if (id === dealerId) {
                card = $(dealer2ndStack.children()[y]);
                props[c] = {x: card.css("left"), y: card.css("top"), r: getRotationDegrees(card)};
                card.css({top: pos.top, left: pos.left});
                card.hide();
                cards[c++] = card;
            }
            if (childs.length > 0) {
                card = $(childs[y]);
                props[c] = {x: card.css("left"), y: card.css("top"), r: getRotationDegrees(card)};
                card.css({top: pos.top, left: pos.left});
                card.hide();
                cards[c++] = card;
            }
        }
    }
    var delay = 400;
    for (var i = 0; i < cards.length; i++) {
        if (i === cards.length - 1) {
            animateDealSingleCard(cards[i], pos.top, pos.left, props[i], i * delay, readyFunction);
        } else {
            animateDealSingleCard(cards[i], pos.top, pos.left, props[i], i * delay);
        }
    }
}

function animateDealSingleCard(card, top, left, props, delay, readyFunction) {
    var distance = Math.abs(calculateDistanceBetweenPoints(left, top, parseFloat(props.x), parseFloat(props.y)));
    var animTime = 1.75 * distance;
    card.prop("rot", 0);
    var animProps = {
        duration: animTime,
        step: function (now, tween) {
            if (tween.prop === "rot") {
                card.css("transform", "rotate(" + now + "deg)");
            }
        }
    };
    if (typeof readyFunction === "function") {
        animProps.complete = readyFunction;
    }
    setTimeout(function () {
        card.show();
        card.animate({top: props.y, left: props.x, rot: props.r}, animProps);
    }, delay);
}

function calculateDistanceBetweenPoints(x1, y1, x2, y2) {
    return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
}

function animateSelectStack(isKeepStack, readyFunction) {
    var gameStack = $("#gameStack");
    var attendeeStack = attendeesCardStacks[getAllAttendeeIdByPlayerId(getPlayerIdByName(mover))];
    var playerCards = [];
    var dealer2ndCards = [];
    var trashCards = [];
    var trashCardProps = [];
    var playerCardProps = [];
    for (var i = 0; i < 3; i++) {
        playerCards[i] = $(attendeeStack.children()[i]);
        dealer2ndCards[i] = $(dealer2ndStack.children()[i]);
        playerCardProps[i] = {top: playerCards[i].css("top"), left: playerCards[i].css("left"), rot: getRotationDegrees(playerCards[i])};
        trashCardProps[i] = getGameStackProperties(i, playerCards[i], gameStack);
    }

    var finishFunction = isKeepStack ? readyFunction : function () {
        gameStack.append(trashCards[0]);
        gameStack.append(trashCards[1]);
        gameStack.append(trashCards[2]);
        attendeeStack.prepend(dealer2ndStack[2]);
        attendeeStack.prepend(dealer2ndStack[1]);
        attendeeStack.prepend(dealer2ndStack[0]);
        var takeSpeed = 500;
        animateTakeCard(takeSpeed, dealer2ndCards[0], playerCardProps[0]);
        animateTakeCard(takeSpeed, dealer2ndCards[1], playerCardProps[1]);
        animateTakeCard(takeSpeed, dealer2ndCards[2], playerCardProps[2], readyFunction);
    };

    var giveSpeed = 1500;
    var trashCards = isKeepStack ? dealer2ndCards : playerCards;
    animateGiveCard(giveSpeed, trashCards[0], trashCardProps[0], 0);
    animateGiveCard(giveSpeed, trashCards[1], trashCardProps[1], 0);
    animateGiveCard(giveSpeed, trashCards[2], trashCardProps[2], 0, finishFunction);
}

/* New Cards */
function animateStackChange(readyFunction) {
    var speed = 2500;
    $("#swapCardBtn").prop("disabled", true);
    $("#swapAllCardsBtn").prop("disabled", true);
    $("#passBtn").prop("disabled", true);
    $("#knockBtn").prop("disabled", true);
    $("#newCardsBtn").prop("disabled", true);
    var gameStack = $("#gameStack");
    var svg1 = $(gameStack.children()[0]);
    var svg2 = $(gameStack.children()[1]);
    var svg3 = $(gameStack.children()[2]);
    var cardStack = $("#gameStack");
    var offX = cardStack.offset().left + cardStack.width() + 25;
    sound.newcards.play();
    svg1.animate({left: "-=" + offX}, speed);
    svg2.animate({left: "-=" + offX}, speed);
    svg3.animate({left: "-=" + offX}, speed, function () {
        if (typeof readyFunction === "function") {
            svg1.remove();
            svg2.remove();
            svg3.remove();
            readyFunction();
        }
    });
}

/* Swap all three cards */
function animateAllCardsSwap(readyFunction) {
    $("#swapCardBtn").prop("disabled", true);
    $("#swapAllCardsBtn").prop("disabled", true);
    $("#passBtn").prop("disabled", true);
    $("#knockBtn").prop("disabled", true);
    $("#newCardsBtn").prop("disabled", true);
    var gameStack = $("#gameStack");
    var attendeeStack = attendeesCardStacks[getAllAttendeeIdByPlayerId(getPlayerIdByName(mover))];
    var gameCards = [];
    var playerCardProps = [];
    var playerCards = [];
    var reverseTargetProps = [];
    for (var i = 0; i < 3; i++) {
        gameCards[i] = $(gameStack.children()[i]);
        playerCards[i] = $(attendeeStack.children()[i]);
        playerCardProps[i] = {top: playerCards[i].css("top"), left: playerCards[i].css("left"), rot: getRotationDegrees(playerCards[i])};
        reverseTargetProps[i] = getGameStackProperties(i, gameCards[i], gameStack);
    }

    var finish = function () {
        sound.swap.play();
        gameStack.append(playerCards[0]);
        gameStack.append(playerCards[1]);
        gameStack.append(playerCards[2]);
        if (typeof readyFunction === "function") {
            readyFunction();
        }
    };

    var reverse = function () {
        var giveSpeed = 1500;
        animateGiveCard(giveSpeed, playerCards[0], reverseTargetProps[0], 0);
        animateGiveCard(giveSpeed, playerCards[1], reverseTargetProps[1], 0);
        animateGiveCard(giveSpeed, playerCards[2], reverseTargetProps[2], 0, finish);
    };

    sound.click.play();
    attendeeStack.prepend(gameCards[2]);
    attendeeStack.prepend(gameCards[1]);
    attendeeStack.prepend(gameCards[0]);
    var takeSpeed = 2500;
    animateTakeCard(takeSpeed, gameCards[0], playerCardProps[0]);
    animateTakeCard(takeSpeed, gameCards[1], playerCardProps[1]);
    animateTakeCard(takeSpeed, gameCards[2], playerCardProps[2], reverse);
}

/* Swap a single card */
function animateCardSwap(takenId, givenId, readyFunction) {
    $("#swapCardBtn").prop("disabled", true);
    $("#swapAllCardsBtn").prop("disabled", true);
    $("#passBtn").prop("disabled", true);
    $("#knockBtn").prop("disabled", true);
    $("#newCardsBtn").prop("disabled", true);
    var gameStack = $("#gameStack");
    var attendeeStack = attendeesCardStacks[getAllAttendeeIdByPlayerId(getPlayerIdByName(mover))];
    var gameStackAnchor = gameStack.children()[takenId + 1];
    var tSvg = $(gameStack.children()[takenId]);
    var gSvg = $(attendeeStack.children()[givenId]);
    var gProps = {top: gSvg.css("top"), left: gSvg.css("left"), rot: getRotationDegrees(gSvg)};

    var finish = function () {
        sound.swap.play();
        if (gameStackAnchor !== undefined) {
            gSvg.insertBefore(gameStackAnchor);
        } else {
            gameStack.append(gSvg);
        }
        if (typeof readyFunction === "function") {
            readyFunction();
        }
    };
    var targetProps = getGameStackProperties(takenId, tSvg, gameStack);
    var reverse = function () {
        animateGiveCard(1500, gSvg, targetProps, cardFlips[takenId], finish);
    };
    sound.click.play();
    tSvg.insertBefore(gSvg);
    animateTakeCard(2500, tSvg, gProps, reverse);
}

function animateGiveCard(speed, card, targetProps, cardFlips, finish) {
    var startRot = getRotationDegrees(card);
    var targetRot = targetProps.r;
    targetRot += cardFlips * 360;
    card.prop("rot", startRot);
    var animProps = {
        duration: speed,
        step: function (now, tween) {
            if (tween.prop === "rot") {
                card.css("transform", "rotate(" + now + "deg)");
            }
        }
    };
    if (typeof finish === "function") {
        animProps.complete = finish;
    }
    card.animate({top: targetProps.y, left: targetProps.x, rot: targetRot}, animProps);
}

function animateTakeCard(speed, card, targetProps, reverse) {
    card.prop("rot", getRotationDegrees(card));
    var animProps = {
        duration: speed,
        step: function (now, tween) {
            if (tween.prop === "rot") {
                card.css("transform", "rotate(" + now + "deg)");
            }
        }
    };
    if (typeof reverse === "function") {
        animProps.complete = reverse;
    }
    card.animate({top: targetProps.top, left: targetProps.left, rot: targetProps.rot}, animProps);
}

function getRotationDegrees(obj) {
    var angle = 0;
    var matrix = obj.css("-webkit-transform") ||
            obj.css("-moz-transform") ||
            obj.css("-ms-transform") ||
            obj.css("-o-transform") ||
            obj.css("transform");
    if (matrix !== 'none') {
        var values = matrix.split('(')[1].split(')')[0].split(',');
        var a = values[0];
        var b = values[1];
        angle = Math.round(Math.atan2(b, a) * (180 / Math.PI));
    }
    return angle;
}

function onPlayerList(message) {
    messageInProgress = false;
    players = message.players;
    admin = message.admin;
    updatePlayerList();
    updateAdminWindow();
}

function onPlayerOnline(message) {
    messageInProgress = false;
    players.forEach(function (player) {
        if (player.name === message.name) {
            player.online = message.online;
        }
    });
    admin = message.admin;
    updatePlayerList();
    updateAdminWindow();
    if (message.online) {
        sound.online.play();
    } else {
        sound.offline.play();
    }
}

function onRadioListChanged(list) {
    log("process onRadioListChanged");
    radioList = list;
    var adminRadioBox = $("#cfgRadioList");
    var id = 0;
    adminRadioBox.empty();
    radioList.forEach(function (radioUrl) {
        $("<option/>").val(id++).text(radioUrl.name).appendTo(adminRadioBox);
    });
    updateRadioList(radioUrl)
}

function onChatMessage(message) {
    messageInProgress = false;
    var text = "<span class='chatMsgTxt'>" + message.text + "</span>";
    if (message.sender !== undefined) {
        text = "<span class='chatMsgSender'>" + message.sender + ": " + "</span>" + text;
    }
    var chatArea = $("#chatArea");
    chatArea.append("&gt; " + text + "<br>");
    chatArea.scrollTop(chatArea[0].scrollHeight);
    if (message.sender !== undefined || message.beep) {
        sound.chat.play();
    }
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
    if (discoverMsg.payers == undefined && discoverMsg.leavers == undefined) {
        msgText += "<br>Unentschieden, es folgt noch eine Runde!";
    }
    $("#discoverMessage").html(msgText);
}

function updatePlayerList() {
    var panel = $("#playerListPanel");
    panel.empty();
    players.forEach(function (player) {
        var className = player.online ? "playerOnline" : "playerOffline";
        var tokens = (0.5 * player.totalTokens).toFixed(2);
        var admnImg = (admin === player.name) ? "dot-blue-8.png" : "dot-empty-8.png";
        var name = $("<span class='playerPopupEnabled'>" + player.name + "</span>");
        var container = $("<div class='" + className + "'></div>");
        container.append($("<img src='" + admnImg + "' border=0>"));
        container.append(name);
        container.append("<br>&euro; " + tokens);
        panel.append(container);
        name.click(createPlayerClickFunction(player));
    });
}

function showPlayerPopup(evt, player) {
    setTimeout(function () { // asynchron because the document handler will fire a close event
        $("#playerPopupMenuTitle").html(player.name);
        var meIsAttendee = isAttendee();
        var targetIsAttendee = getAttendeeIdByName(player.name) >= 0;
        var targetIsMe = player.name === myName;
        var enabled = "playerPopupMenuItem";
        var disabled = "playerPopupMenuItemDisabled";
        var miAskForCardView = $("#miAskForViewCards");
        var miAskForCardShow = $("#miAskForShowCards");
        var miStopCardViewing = $("#miStopViewCards");
        var miStopCardShowing = $("#miStopShowCards");
        miAskForCardView.hide();
        miAskForCardShow.hide();
        miStopCardViewing.hide();
        miStopCardShowing.hide();
        miAskForCardView.off("click");
        miAskForCardShow.off("click");
        miStopCardViewing.off("click");
        miStopCardShowing.off("click");
        if (meIsAttendee) {
            miAskForCardShow.show();
            miStopCardShowing.show();
            var isMyViewer = isViewerOf(player.name, myName);
            var askEnabled = !(targetIsAttendee || targetIsMe || isMyViewer || !player.online);
            var stopEnabled = !(targetIsAttendee || targetIsMe || !isMyViewer);
            miAskForCardShow.prop("class", askEnabled ? enabled : disabled);
            miStopCardShowing.prop("class", stopEnabled ? enabled : disabled);
            if (askEnabled) {
                miAskForCardShow.click(function () {
                    askForCardShow(player.name);
                    setPlayerPopupVisible(false);
                });
            }
            if (stopEnabled) {
                miStopCardShowing.click(function () {
                    stopCardShowing(player.name);
                    setPlayerPopupVisible(false);
                });
            }
        } else {
            miAskForCardView.show();
            miStopCardViewing.show();
            var meIsViewer = isViewerOf(myName, player.name);
            var askEnabled = targetIsAttendee && !targetIsMe && !meIsViewer && player.online;
            var stopEnabled = targetIsAttendee && !targetIsMe && meIsViewer;
            miAskForCardView.prop("class", askEnabled ? enabled : disabled);
            miStopCardViewing.prop("class", stopEnabled ? enabled : disabled);
            if (askEnabled) {
                miAskForCardView.click(function () {
                    askForCardView(player.name);
                    setPlayerPopupVisible(false);
                });
            }
            if (stopEnabled) {
                miStopCardViewing.click(function () {
                    stopCardViewing(player.name);
                    setPlayerPopupVisible(false);
                });
            }
        }
        var popup = $("#playerPopupMenu");
        popup.css("top", evt.pageY);
        popup.css("left", evt.pageX);
        popup.fadeIn("fast");
    });
}

function isViewerOf(viewer, shower) {
    var list = getViewerList(shower);
    if (list !== undefined) {
        for (i = 1; i < list.length; i++) {
            if (list[i] === viewer) {
                return true;
            }
        }
    }
    return false;
}

function getViewerList(name) {
    if (viewerMap !== undefined) {
        for (i = 0; i < viewerMap.length; i++) {
            var list = viewerMap[i];
            if (list !== undefined && list[0] === name) {
                return list;
            }
        }
    }
    return undefined;
}

function createPlayerClickFunction(player) {
    return function (evt) {
        showPlayerPopup(evt, player);
    };
}

function updateAttendeeList() {
    var panel = $("#attendeesPanel");
    gameDesk.remove();
    panel.empty();
    attendeesCardStacks = [];
    if (attendees !== undefined) {
        var otherAttendeesCount = allAttendees.length - (isInAllAttendees() ? 1 : 0);
        var myId = getMyAllAttendeeId();
        var numPl = allAttendees.length;
        var step = (2 * Math.PI) / (numPl + ((myId < 0) ? 1 : 0));
        var angle = Math.PI / 2 + step;
        var id = myId;
        var isSmallSize = panel.width() < 800;
        var rx = panel.width() * (!isSmallSize ? 0.35 : 0.25);
        var ry = panel.height() * 0.26;
        for (var i = 0; i < numPl; i++) {
            id = getNextAllAttendeeId(id);
            attendeesCardStacks[id] = createCardStack();
            var player = players[ allAttendees[id] ];
            var attendeeDesk = createAttendeeDesk(player, attendeesCardStacks[id]);
            if (id === myId) { // add the game desk here
                panel.append(gameDesk);
            }
            panel.append(attendeeDesk);
            var l = (panel.width() * 0.5) + rx * Math.cos(angle) - (attendeeDesk.outerWidth() >> 1);
            var t = ((panel.height() * 0.5) + ry * Math.sin(angle)
                    - (1.1 * attendeesCardStacks[id].outerHeight() * ((isSmallSize && otherAttendeesCount > 1) ? 1.4 : 1)));
            attendeeDesk.css({left: l + "px", top: t + "px"});
            if (gamePhase === "dealCards" && player.name === mover) { // add the dealer 2nd stack here
                createDealer2ndStack(attendeeDesk, panel);
            }
            if (id === myId) { // set the control panel position here
                controlPanel.css({left: l + attendeeDesk.outerWidth() + 4 + "px", top: t + "px"});
            }
            angle += step;
        }
        if (!isAttendee()) {
            panel.append(gameDesk);
        }
        gameDesk.css({top: (panel.height() - gameDesk.height()) * 0.5 - ((isSmallSize) ? 0.075 : 0.085) * panel.height() + "px", left: (panel.width() - gameDesk.width()) * 0.5 + "px"});

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

function createDealer2ndStack(attendeeDesk, panel) {
    dealer2ndStack = createCardStack();
    dealer2ndStack.insertBefore(attendeeDesk);
    var off = attendeeDesk.offset();
    var isNorth = (off.top + 0.5 * attendeeDesk.outerHeight()) > (0.5 * panel.height()) ? false : true;
    var card = $(getSvgCard(coveredCard).getUI()).clone();
    var cardSize;
    $("body").prepend(card);
    cardSize = {w: card.width(), h: card.height()};
    card.remove();
    dealer2ndStack.css({
        position: "absolute",
        left: (off.left + 30) + "px",
        top: (off.top + (isNorth ? (cardSize.h * 0.6) : -(cardSize.h * 0.15))) + "px"
    });
    return dealer2ndStack;
}

function createCardStack() {
    var cardDesk = $("<div class='cardStack'></div>");
    return cardDesk;
}

function createAttendeeDesk(player, stackDesk) {
    var attendeeId = getAttendeeIdByName(player.name);
    var token = attendeeId >= 0 ? attendees[attendeeId].gameTokens : -1;
    var playerDesk = $("<div class='attendeeDesk'></div>");
    var nameContainer = $("<div class='attendeeNameContainer'></div>");
    var nameDiv = $("<div class='attendeeName'></div>");
    var nameSpan = $("<span class='playerPopupEnabled'>" + player.name + "</span>");
    nameSpan.click(createPlayerClickFunction(player));
    nameDiv.append(nameSpan);
    var viewerList = getViewerList(player.name);
    if (viewerList !== undefined && viewerList.length > 1) { // first entry is the own name
        var viewerDiv = $("<div class='cardViewerName'></div>");
        var viewerHtml = viewerList[1];
        for (var n = 2; n < viewerList.length; n++) {
            viewerHtml += ("<br>" + viewerList[n]);
        }
        viewerDiv.html(viewerHtml);
        nameDiv.append($("<br>")).append(viewerDiv);
    }
    nameContainer.append(nameDiv);
    nameContainer.append($("<div class='tokenImage" + token + "'></div>"));
    playerDesk.append(stackDesk).append(nameContainer);
    return playerDesk;
}

function getPlayerNameClickFunction(player) {
    return function (evt) {
        showPlayerPopup(evt, player);
    };
}

function updateAttendeeDeskColor() {
    if (attendees !== undefined) {
        var numPl = allAttendees.length;
        var id = getMyAllAttendeeId();
        for (var i = 0; i < numPl; i++) {
            id = getNextAllAttendeeId(id);
            var attendee = players[allAttendees[id]];
            var className = (mover === attendee.name) ? "moverDesk" : "attendeeDesk";
            attendeesCardStacks[id].parent().prop("class", className);
        }
    }
}

function emptyAllStackDesks() {
    playerStack = undefined;
    gameStack = undefined;
    attendeesCardStacks.forEach(function (desk) {
        desk.empty();
    });
    $("#gameStack").empty();
}

function updateAttendeeStacks(message) {
    try {
        var discoverStacks = undefined;
        var finish31OnDealMessage = undefined;
        if (message !== undefined) {
            if (gamePhase === "discover") {
                discoverStacks = (message.action === "gameState") ? message.discoverStacks : message.discoverMessage.playerStacks;
            }
            if (gamePhase === "finish31OnDeal") {
                finish31OnDealMessage = message.finish31OnDealMessage; //(message.action === "gameState") ? message.finish31OnDealMessage : message.discoverMessage.finish31OnDealMessage;
            }
        }
        var id = getMyAllAttendeeId();
        var deskId = getMyAllAttendeeId();
        var myDesk = deskId >= 0 ? attendeesCardStacks[deskId] : undefined;
        for (var i = 0; i < allAttendees.length; i++) {
            id = getNextAllAttendeeId(id);
            var desk = attendeesCardStacks[id];
            if (desk !== myDesk) {
                var playerName = players[allAttendees[id]].name;
                var isAttendee = getAttendeeIdByName(playerName) >= 0;
                if (discoverStacks !== undefined) {
                    updateCardStack(desk, isAttendee ? discoverStacks[getAttendeeIdByName(playerName)].cards : undefined);
                } else {
                    var viewerStack = getViewerStack(playerName);
                    if (finish31OnDealMessage !== undefined && finish31OnDealMessage.finisher === playerName) {
                        viewerStack = finish31OnDealMessage.finishStack.cards;
                    }
                    updateCardStack(desk, isAttendee
                            ? (viewerStack !== undefined
                                    ? viewerStack
                                    : ((gamePhase !== "waitForAttendees" && gamePhase != "shuffle")
                                            ? coveredStack
                                            : undefined))
                            : undefined);
                }
            } else {
                updateCardStack(desk, playerStack);
            }
        }
        if (dealer2ndStack !== undefined) {
            updateCardStack(dealer2ndStack, coveredStack);
        }
    } catch (e) {
        log("Fehler in updateAttendeeStacks(): '" + e + "'");
    }
}

function getGameStackProperties(id, card, desk) {
    return {
        y: desk.offset().top + ((desk.height() - card.height()) >> 1) + gameStackOffsets[id].y + "px",
        x: desk.offset().left + ((desk.width() - card.width()) >> 1) - card.width() + id * card.width() + gameStackOffsets[id].x + "px",
        r: gameStackRotations[id]
    };
}

function getDealer2ndStackProperties() {
    if (dealer2ndStackProps === undefined) {
        dealer2ndStackProps = [];
        for (var i = 0; i < 3; i++) {
            dealer2ndStackProps[i] = {
                x: (Math.random() * (2 * 16)) - 16,
                y: (Math.random() * (2 * 8)) - 8,
                r: (Math.random() * (2 * 4)) - 4
            };
        }
    }
    return dealer2ndStackProps;
}

function updateCardStack(desk, cards) {
    try {
        if (desk !== undefined) {
            var isGameStack = cards === gameStack;
            var isDealer2ndStack = desk === dealer2ndStack;
            desk.empty();
            if (cards !== undefined && cards.length > 0) {
                var isCovered = (cards === coveredStack);
                var rotStepX = 7.5; // in degrees
                for (var i = 0; i < cards.length; i++) {
                    var svg = (isCovered) ? getSvgCard(cards[i]).getUI().clone() : getSvgCard(cards[i]).getUI();
                    var card = $(svg);
                    card.css("position", "fixed");
                    desk.append(card);
                    if (isGameStack) {
                        var props = getGameStackProperties(i, card, desk);
                        card.css({top: props.y, left: props.x, transform: "rotate(" + props.r + "deg)"});
                    } else if (isDealer2ndStack) {
                        var props = getDealer2ndStackProperties()[i];
                        var off = dealer2ndStack.offset();
                        card.css({top: off.top + props.y + "px", left: off.left + props.x + "px", transform: "rotate(" + props.r + "deg)"});
                    } else {
                        var shiftX = 0.5 * card.width();
                        var y = ((desk.height() - card.height()) >> 1);
                        var x = ((desk.width() - card.width()) >> 1) - shiftX;
                        card.css("top", desk.offset().top + y + "px");
                        card.css("left", desk.offset().left + x + i * shiftX + "px");
                        card.css("transform", "rotate(" + (-rotStepX + i * rotStepX) + "deg)");
                    }
                    card.css("transform-origin", "50% 50%");
                }
            }
        }
    } catch (e) {
        log("Fehler in updateAttendeeStacks(): '" + e + "'");
    }
}

function getViewerStack(name) {
    if (viewerStacks !== undefined) {
        for (var i = 0; i < viewerStacks.length; i++) {
            if (viewerStacks[i].name === name) {
                return viewerStacks[i].cards;
            }
        }
    }
    return undefined;
}

function processCardHover(uiCard, isHover) {
    if (gamePhase === "waitForPlayerMove" && mover === myName) {
        uiCard.setHover(isHover);
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
    $("#bottomPanelMessage").html(msg);
}

function updateRadioList(radioUrl) {
    var adminRadioBox = $("#cfgRadioList");
    adminRadioBox.removeAttr('selected');
    for (var id = 0; id < radioList.length; id++) {
        if (radioUrl === radioList[id].url) {
            adminRadioBox.val(id);
            break;
        }
    }
}

function updateAdminWindow() {
    var isAdmin = (myName === admin);
    var isRunning = (gamePhase != "waitForAttendees");
    var isMinAttendees = (attendees.length > 1);
    $("#cfgStartGameBtn").prop("disabled", !(isAdmin && !isRunning && isMinAttendees));
    $("#cfgStopGameBtn").prop("disabled", !(isAdmin && isRunning));
    $("#cfgShufflePlayersBtn").prop("disabled", !(isAdmin && !isRunning && isMinAttendees));
    $("#cfgRadioList").prop("disabled", !(isAdmin));
    if (!isAdmin) {
        $("#adminButton").hide();
        if (adminWindow.is(':visible')) {
            onCloseAdminWindow();
        }
    } else {
        $("#adminButton").show();
    }
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

function onOpenAdminWindow() {
    if (myName === admin) {
        adminWindow.slideDown(500);
    }
}

function onCloseAdminWindow() {
    adminWindow.slideUp(500);
}

function setPlayerPopupVisible(visible) {
    if (visible) {
        playerPopup.fadeIn("fast");
    } else {
        playerPopup.fadeOut("fast");
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
    resetUICards();
    swapSelection.myStackId = -1;
    swapSelection.gameStackId = -1;
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

function askForCardView(target) {
    var msg = {"action": "askForCardView", "target": target};
    webSocket.send(JSON.stringify(msg));
}

function stopCardViewing(target) {
    var msg = {"action": "stopCardViewing", "target": target};
    webSocket.send(JSON.stringify(msg));
}

function askForCardShow(target) {
    var msg = {"action": "askForCardShow", "target": target};
    webSocket.send(JSON.stringify(msg));
}

function stopCardShowing(target) {
    var msg = {"action": "stopCardShowing", "target": target};
    webSocket.send(JSON.stringify(msg));
}

function viewCardsResponse(allowed) {
    var msg = {"action": "askForCardViewResponse", "hashCode": askForViewerHashCode, "value": allowed};
    webSocket.send(JSON.stringify(msg));
    setGameDialogVisible($("#askForViewCardsDialog"), false);
    questionMessageInProgress = false;
    onQuestionMessageBuffer();
}

function showCardsResponse(allowed) {
    var msg = {"action": "askForCardShowResponse", "hashCode": askForViewerHashCode, "value": allowed};
    webSocket.send(JSON.stringify(msg));
    setGameDialogVisible($("#askForShowCardsDialog"), false);
    questionMessageInProgress = false;
    onQuestionMessageBuffer();
}


function onCfgChangeWebRadio() {
    if (myName === admin) {
        var msg = {"action": "command", "command": "changeWebradio", "id": $("#cfgRadioList").val()};
        webSocket.send(JSON.stringify(msg));
    }
}

function onCfgStartGameBtn() {
    if (myName === admin) {
        var msg = {"action": "command", "command": "start"};
        webSocket.send(JSON.stringify(msg));
    }
}

function onCfgStopGameBtn() {
    if (myName === admin) {
        if (window.confirm("Soll das Spiel abgebrochen werden?")) {
            var msg = {"action": "command", "command": "stop"};
        }
        webSocket.send(JSON.stringify(msg));
    }
}

function onCfgShufflePlayersBtn() {
    if (myName === admin) {
        var msg = {"action": "command", "command": "shufflePlayers"};
        webSocket.send(JSON.stringify(msg));
    }
}