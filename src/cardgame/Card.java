package cardgame;

/**
 *
 */
public class Card implements Comparable {

    private final int color;
    private final int value;

    // Farben	
    public static final int CARO = 1;
    public static final int HERZ = 2;
    public static final int PIK = 3;
    public static final int KREUZ = 4;

    // Werte
    public static final int ZWEI = 2;
    public static final int DREI = 3;
    public static final int VIER = 4;
    public static final int FUENF = 5;
    public static final int SECHS = 6;
    public static final int SIEBEN = 7;
    public static final int ACHT = 8;
    public static final int NEUN = 9;
    public static final int BUBE = 10;
    public static final int DAME = 11;
    public static final int KOENIG = 12;
    public static final int ZEHN = 13;
    public static final int AS = 14;

    /**
     * Konstruktor Erstellungsdatum: (15.06.2003 12:03:36)
     *
     * @param color int
     * @param value int
     */
    public Card(int color, int value) {
        this.color = color;
        this.value = value;
        if (color < CARO || color > KREUZ) {
            throw new IllegalArgumentException("Unknown Color.");
        }
        if (value < ZWEI || value > AS) {
            throw new IllegalArgumentException("Unknown Value.");
        }
    }

    /**
     * Wandelt eine Farb-Konstante in einen String um. Erstellungsdatum:
     * (15.06.2003 16:10:44)
     *
     * @return java.lang.String
     * @param color int
     */
    public static String colorToString(int color) {
        if (color < CARO || color > KREUZ) {
            throw new IllegalArgumentException("Unknown Color.");
        }
        String colorString = null;
        switch (color) {
            case CARO:
                colorString = "Karo";
                break;
            case HERZ:
                colorString = "Herz";
                break;
            case PIK:
                colorString = "Pik";
                break;
            case KREUZ:
                colorString = "Kreuz";
                break;
        }
        return colorString;
    }

    /**
     * Interface Comparable. Wird zum sortieren der Karten benoetigt
     * Erstellungsdatum: (17.06.2003 13:11:49)
     *
     * @return int
     * @param other java.lang.Object
     */
    @Override
    public int compareTo(Object other) {
        if (other instanceof Card) {
            int otherValue = ((Card) other).getValue();
            if (otherValue < value) {
                return 1;
            }
            if (otherValue > value) {
                return -1;
            }
        }
        return 0;
    }

    /**
     * Vergleicht eine Karten mit den angegebnen Werten Erstellungsdatum:
     * (16.06.2003 16:36:20)
     *
     * @return boolean
     * @param color int
     * @param value int
     */
    public boolean equals(int color, int value) {
        return (this.color == color && this.value == value);
    }

    /**
     * Liefert die Farbe er Karte zurueck Erstellungsdatum: (15.06.2003
     * 11:56:41)
     *
     * @return int
     */
    public int getColor() {
        return color;
    }

    /**
     * Liefert den numerischen Wert der Spielkarte zurueck. Erstellungsdatum:
     * (15.06.2003 12:01:49)
     *
     * @return int
     */
    public int getValue() {
        return value;
    }

    /**
     * Liefert einen String mit der Bezeichnung der Karte zurueck.
     * Erstellungsdatum: (15.06.2003 13:58:45)
     *
     * @return java.lang.String
     */
    public String toString() {
        return colorToString(color) + " " + valueToString(value);
    }

    public static String toString(int color, int value) {
        return colorToString(color) + " " + valueToString(value);
    }

    /**
     * Wandelt einen Kartenwert in einen String um Erstellungsdatum: (15.06.2003
     * 16:11:09)
     *
     * @return java.lang.String
     * @param value int
     */
    public static String valueToString(int value) {
        if (value < ZWEI || value > AS) {
            throw new IllegalArgumentException("Unknown Value.");
        }

        String valueString = null;
        switch (value) {
            case ZWEI:
                valueString = "Zwei";
                break;
            case DREI:
                valueString = "Drei";
                break;
            case VIER:
                valueString = "Vier";
                break;
            case FUENF:
                valueString = "Fuenf";
                break;
            case SECHS:
                valueString = "Sechs";
                break;
            case SIEBEN:
                valueString = "Sieben";
                break;
            case ACHT:
                valueString = "Acht";
                break;
            case NEUN:
                valueString = "Neun";
                break;
            case ZEHN:
                valueString = "Zehn";
                break;
            case BUBE:
                valueString = "Bube";
                break;
            case DAME:
                valueString = "Dame";
                break;
            case KOENIG:
                valueString = "Koenig";
                break;
            case AS:
                valueString = "As";
                break;

        }
        return valueString;
    }
}
