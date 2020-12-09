package schwimmen.messages;

import java.util.List;
import schwimmen.SchwimmenPlayer;

public class AttendeeList {

    public final String action = "attendeeList";

    public Player[] attendees;
    public int[] allAttendees;
    public String mover;

    public AttendeeList(List<SchwimmenPlayer> anttendeeList, int[] allAttendees, SchwimmenPlayer mover) {
        attendees = new Player[anttendeeList.size()];
        for (int i = 0; i < attendees.length; i++) {
            attendees[i] = new Player(anttendeeList.get(i));
        }
        this.allAttendees = allAttendees;
        if (mover != null) {
            this.mover = mover.getName();
        }
    }

}
