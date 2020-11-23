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
    log("Start creating SvgCards");
    var svgCards = $("<object width=1 height=1 class='cards' type='image/svg+xml'></object>");
    $("body").append(svgCards);
    svgCards[0].setAttribute("data", "svg-cards.svg");
    svgCards.on("load", function () {
        log("Svg-Datei geladen");
        svgCards.hide();
        var svgDoc = svgCards.prop("contentDocument");
        var svgTemp = $("svg", svgDoc);
        extractSvgCards(svgTemp);
        for (var color = CARO; color <= KREUZ; color++) {
            uiCards[color] = [];
            for (var value = ZWEI; value <= AS; value++) {
                uiCards[color][value] = new UICard(color, value, svgTemp.clone());
            }
        }
        uiBackCard = new UICard(undefined, undefined, svgTemp.clone());
        svgCards.remove();
        log("SvgCards created");
        if (typeof callback === "function") {
            callback();
        }
    });
}

var UICard = function (color, value, svgTemp) {
    this.svg = svgTemp;
    this.color = color;
    this.value = value;
    this.selected = false;
    var base = $("#base", this.svg);
    this.basePath = base.children("path");
    var cardName = getSvgName(color, value);
    var useBase = svgCards[cardName].find("use[xlink\\:href='#base']");
    var xOff = -1 * useBase.attr("x");
    var yOff = -1 * useBase.attr("y");
    svgCards[cardName][0].setAttribute("transform", "translate(" + xOff + " " + yOff + ")");
    var viewBox = this.svg[0].getAttribute("viewBox").split(" ");
    var vbWidth = viewBox[2] - viewBox[0];
    var vbHeight = viewBox[3] - viewBox[1];
    var cols = 13;
    var rows = 6;
    var cardWidth = vbWidth / cols; // 168
    var cardHeight = 1 + vbHeight / rows; //243; 
    // viewBox original: "-.2 -236 2178.99 1216.19"
    // ratio: 1,446428571428571
    this.svg[0].setAttribute("viewBox", "-.2 -236 " + cardWidth + " " + cardHeight);
    this.svg[0].setAttribute("id", getSvgName(color, value));
//    this.svg[0].setAttribute("width", cardWidth);
//    this.svg[0].setAttribute("height", cardHeight);
    this.svg[0].removeAttribute("width");
    this.svg[0].removeAttribute("height");
    this.svg[0].setAttribute("class", "card");
    this.svg.children("g").append(svgCards[cardName]);

    // to make fill-change work properly: use the real base object and remove the use-tag.
    base.remove();
    useBase.remove();
    this.svg.children("g").prepend(base);

    var uiCard = this;
    if (color !== undefined && value !== undefined) {
        this.svg[0].onclick = function () {
            if (typeof processCardClick === "function") {
                processCardClick(uiCard);
            }
        };
    }
};

UICard.prototype.getUI = function () {
    return this.svg;
}

UICard.prototype.setSelected = function (selected) {
    this.selected = selected;
    this.basePath.css("fill", (selected ? "#A0A0FF" : "#FFFFFF"));
};

var svgColors = ["club", "diamond", "heart", "spade"];
var svgValues = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "king", "queen", "jack"];
var svgCards = [];
function extractSvgCards(svgRoot) {
    for (var c = 0; c < svgColors.length; c++) {
        for (var v = 0; v < svgValues.length; v++) {
            var cardName = svgValues[v] + "_" + svgColors[c];
            svgCards[cardName] = $("#" + cardName, svgRoot);
            svgCards[cardName].remove();
        }
    }
    svgCards["black_joker"] = $("#black_joker", svgRoot);
    svgCards["red_joker"] = $("#red_joker", svgRoot);
    svgCards["back"] = $("#back", svgRoot);
    svgCards["black_joker"].remove();
    svgCards["red_joker"].remove();
    svgCards["back"].remove();
}

function getSvgName(color, value) {
    if (color !== undefined && value !== undefined) {
        return getSvgValue(value) + "_" + getSvgColor(color);
    }
    return "back";
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
