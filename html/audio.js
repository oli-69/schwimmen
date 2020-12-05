var sound;
var radioUrl = "https://onlineradiobox.com/json/de/radioseefunk/play?platform=web";

function SoundFiles() {
    this.chat = new Audio();
    this.online = new Audio();
    this.offline = new Audio();
    this.shuffle = new Audio();
    this.deal = new Audio();
    this.selectStack = new Audio();
    this.click = new Audio();
    this.swap = new Audio();
    this.pass = new Audio();
    this.knock1 = new Audio();
    this.knock2 = new Audio();
    this.newcards = new Audio();
    this.finish31 = new Audio();
    this.fire = new Audio();
    this.coin = new Audio();
    this.finishSound = [];
    this.finishSound.length = 12;
    for (var i = 0; i < this.finishSound.length; i++) {
        this.finishSound[i] = new Audio();
    }
    this.radio = new Audio();
}

function initAudio(readyFunction) {
    if (sound === undefined) {
        sound = new SoundFiles();

        initSound(sound.chat);
        initSound(sound.online);
        initSound(sound.offline);
        initSound(sound.shuffle);
        initSound(sound.deal);
        initSound(sound.selectStack);
        initSound(sound.click);
        initSound(sound.swap);
        initSound(sound.pass);
        initSound(sound.knock1);
        initSound(sound.knock2);
        initSound(sound.newcards);
        initSound(sound.finish31);
        initSound(sound.fire);
        initSound(sound.coin);
        for (var i = 0; i < sound.finishSound.length; i++) {
            initSound(sound.finishSound[i]);
        }
        initSound(sound.radio);
        radioVolumeChanged();

        sound.chat.src = 'snd-chat.mp3';
        sound.online.src = 'snd-online.mp3';
        setTimeout(function () {
            loadAudio(readyFunction);
        });
    } else {
        if (typeof readyFunction === "function") {
            readyFunction();
        }
    }
}

function initSound(snd) {
    snd.play();
    snd.pause();
}

function loadAudio(readyFunction) {
    console.log("Start loading Audio");
    sound.offline.src = 'snd-offline.mp3';
    sound.shuffle.src = 'snd-shuffle.mp3';
    sound.deal.src = 'snd-deal.mp3';
    sound.selectStack.src = 'snd-selectStack.mp3';
    sound.click.src = 'snd-click.mp3';
    sound.swap.src = 'snd-swap.mp3';
    sound.pass.src = 'snd-pass.mp3';
    sound.knock1.src = 'snd-knock1.mp3';
    sound.knock2.src = 'snd-knock2.mp3';
    sound.newcards.src = 'snd-newcards.mp3';
    sound.finish31.src = 'snd-finish31.mp3';
    sound.fire.src = 'snd-fire.mp3';
    sound.coin.src = 'snd-coin.mp3';
    for (var i = 0; i < sound.finishSound.length; i++) {
        sound.finishSound[i].src = 'finish/snd-finish-' + (( i<10) ? "0" : "")+i + '.mp3';
    }
//    sound.radio.src = radioUrl;
//    sound.radio.play();
//    sound.radio.volume = 0.15;

    console.log("Audio loaded successfully");
    if (typeof readyFunction === "function") {
        readyFunction();
    }
}

//function GameAudio(url) {
//    window.AudioContext = window.AudioContext || window.webkitAudioContext; //fix up prefixing
//    this.context = new AudioContext(); //context
//    this.buffer;
//    this.source;
//    var ref = this;
//    var request = new XMLHttpRequest();
//    request.open('GET', url, true);
//    request.responseType = 'arraybuffer'; //the  response is an array of bits
//    request.onload = function () {
//        ref.context.decodeAudioData(request.response, function (response) {
//            ref.buffer = response;
////            console.log(url + " loaded successfully");
//        }, function () {
//            console.error('The request failed.');
//        });
//    }
//    request.send();
//}
//
//GameAudio.prototype.stop = function (looped) {
//    this.source.stop(0);
//}
//
//GameAudio.prototype.play = function (looped) {
//    this.source = this.context.createBufferSource(); //source node
//    this.source.connect(this.context.destination); //connect source to speakers so we can hear it
//    this.source.buffer = this.buffer;
//    this.source.start(0);
//    this.source.loop = (looped !== undefined) ? looped : false;
//};


// Webradio
function  toogleWebRadio() {
    setWebRadioPlaying(sound.radio.paused);
}

function setWebRadioPlaying(playing) {
    console.log("setWebRadioPlaying: " + playing);
    if (playing && sound.radio.paused) {
        sound.radio.src = radioUrl;
        sound.radio.play();
        $("#webRadioBtn").css("background-image", "url('wr-pause-24.png')");
    } else if (!sound.radio.paused) {
        sound.radio.pause();
        sound.radio.src = "";
        $("#webRadioBtn").css("background-image", "url('wr-start-24.png')");
    }
}

function radioVolumeChanged() {
    sound.radio.volume = $("#webRadioSlider").val() / 100.0;
}