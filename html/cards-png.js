// Farben	
var CARO = 1;
var HERZ = 2;
var PIK = 3;
var KREUZ = 4;

// Werte
var ZWEI = 2;
var DREI = 3;
var VIER = 4;
var FUENF = 5;
var SECHS = 6;
var SIEBEN = 7;
var ACHT = 8;
var NEUN = 9;
var BUBE = 10;
var DAME = 11;
var KOENIG = 12;
var ZEHN = 13;
var AS = 14;


/**
 * Wandelt eine Farb-Konstante in einen String um.
 * Erstellungsdatum: (15.06.2003 16:10:44)
 * @return java.lang.String
 * @param color int
 */
function cardColorToString(color) {
    switch (color)
    {
        case CARO :
            return "Karo";
        case HERZ :
            return "Herz";
        case PIK  :
            return "Pik";
        case KREUZ:
            return "Kreuz";
    }
}
/**
 * Liefert einen String mit der Bezeichnung der Karte zurueck.
 * Erstellungsdatum: (15.06.2003 13:58:45)
 * @return java.lang.String
 */
function card2String(color, value)
{
    if (color < 0 || value < 0) {
        return "verdeckt";
    }
    return cardColorToString(color) + " " + cardValueToString(value);
}
/**
 * Wandelt einen Kartenwert in einen String um
 * Erstellungsdatum: (15.06.2003 16:11:09)
 * @return java.lang.String
 * @param value int
 */
function cardValueToString(value)
{
    switch (value)
    {
        case ZWEI:
            return "Zwei";
        case DREI:
            return "Drei";
        case VIER:
            return "Vier";
        case FUENF:
            return "Fuenf";
        case SECHS:
            return "Sechs";
        case SIEBEN:
            return "Sieben";
        case ACHT:
            return "Acht";
        case NEUN:
            return "Neun";
        case ZEHN:
            return "Zehn";
        case BUBE:
            return "Bube";
        case DAME:
            return "Dame";
        case KOENIG:
            return  "Koenig";
        case AS:
            return "As";
    }
}

var uiCards = [];
var uiBackCard;
var svgColors = ["club", "diamond", "heart", "spade"];
var svgValues = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "king", "queen", "jack"];

function getSvgCard(card) {
    if (card !== undefined) {
        return getUICard(card.color, card.value);
    }
}

function getUICard(color, value) {
    if (color < 0 || value < 0) {
        return uiBackCard;
    }
    return uiCards[color][value];
}

function createCards(callback) {
    log("Start creating Cards");

    var fetchCount = 0;
    var imgCount = 33; // ( 32 + back)
    var fetchFunction = function () {
        fetchCount++;
        if (fetchCount === imgCount) {
            log("Cards created");
            if (typeof callback === "function") {
                callback();
            }
        }
    }
    for (var color = CARO; color <= KREUZ; color++) {
        uiCards[color] = [];
        for (var value = SIEBEN; value <= AS; value++) {
            uiCards[color][value] = new UICard(color, value, fetchFunction);
        }
    }
    uiBackCard = new UICard(undefined, undefined, fetchFunction);
}

var UICard = function (color, value, fetchFunction) {
    this.color = color;
    this.value = value;
    this.selected = false;
    var svgName = getSvgName(color, value);
    this.img = $("<img class='card' src='cards/" + svgName + ".png'>");
    this.ui = $("<div class='card' id='" + svgName + "'></div>");
    this.img.on("load", fetchFunction);
    this.img.on("error", function (e) {
        console.log("Error loading image " + e);
    });
    this.ui.append(this.img);

    var activeCard = this;
    if (color !== undefined && value !== undefined) {
        this.ui[0].onclick = function () {
            if (typeof processCardClick === "function") {
                processCardClick(activeCard);
            }
        };
        this.ui[0].onmouseover = function () {
            if (typeof processCardHover === "function") {
                processCardHover(activeCard, true);
            }
        };
        this.ui[0].onmouseout = function () {
            if (typeof processCardHover === "function") {
                processCardHover(activeCard, false);
            }
        };
    }
};

UICard.prototype.getUI = function () {
    return this.ui;
};

UICard.prototype.setSelected = function (selected) {
    this.selected = selected;
    this.img.css("opacity", (selected ? "0.7" : "1"));
    this.ui.css("background-color", (selected ? "#0000FF" : "rgba(0,0,0,0)"));
};

UICard.prototype.setHover = function (isHover) {
    var cursor = isHover ? "pointer" : "auto";
    this.ui.css("cursor", cursor);
    if (!this.selected) {
        var imgOpacity = isHover ? "0.875" : "1";
        var bgColor = isHover ? "#0000FF" : "rgba(0,0,0,0)";
        this.img.css("opacity", imgOpacity);
        this.ui.css("background-color", bgColor);
    }
};

UICard.prototype.reset = function () {
    this.setHover(false);
    this.setSelected(false);
};

function resetUICards() {
    uiCards.forEach(function (color) {
        color.forEach(function (card) {
            card.reset();
        });
    });
}

function getSvgName(color, value) {
    if (color !== undefined && value !== undefined) {
        return getSvgColor(color) + "_" + getSvgValue(value);
    }
    return "back-navy";
}

function getSvgValue(value) {
    switch (value) {
        case ZWEI:
        case DREI:
        case VIER:
        case FUENF:
        case SECHS:
        case SIEBEN:
        case ACHT:
        case NEUN:
            return "" + value;
        case ZEHN:
            return "10";
        case BUBE:
            return "jack";
        case DAME:
            return "queen";
        case KOENIG:
            return  "king";
        case AS:
            return "1";
    }
}

function getSvgColor(color) {
    switch (color) {
        case CARO:
            return "diamond";
        case HERZ:
            return "heart";
        case PIK:
            return "spade";
        case KREUZ:
            return "club";
    }
}
