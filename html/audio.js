var sound;
var radioUrl = "https://onlineradiobox.com/json/de/radioseefunk/play?platform=web";

function SoundFiles() {
    this.chat = new Audio();
    this.online = new Audio();
    this.offline = new Audio();
    this.shuffle = new Audio();
    this.deal = new Audio();
    this.finish31OnDeal = new Audio();
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
    this.askview = new Audio();
    this.finishSound = [];
    this.finishSound.length = 13;
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
        initSound(sound.finish31OnDeal);
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
        initSound(sound.askview);
        for (var i = 0; i < sound.finishSound.length; i++) {
            initSound(sound.finishSound[i]);
        }
        initSound(sound.radio);
        radioVolumeChanged();

        sound.chat.src = 'snd-chat.mp3';
        sound.online.src = 'snd-online.mp3';

        // load the sounds asynchronous in background
        setTimeout(function () {
            loadAudio(readyFunction);
        });
    }
    if (typeof readyFunction === "function") {
        readyFunction();
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
    sound.finish31OnDeal.src = 'snd-finish31OnDeal.mp3';
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
    sound.askview.src = 'snd-askview.mp3';
    for (var i = 0; i < sound.finishSound.length; i++) {
        sound.finishSound[i].src = 'finish/snd-finish-' + ((i < 10) ? "0" : "") + i + '.mp3';
    }
    console.log("Audio loaded successfully");
    if (typeof readyFunction === "function") {
        readyFunction();
    }
}

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
