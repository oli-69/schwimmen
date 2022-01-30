package schwimmen.messages;

public class ViewerStackList {

    public final String action = "viewerStackList";

    public ViewerStack[] viewerStacks;

    public ViewerStackList() {
        this(new ViewerStack[0]);
    }

    public ViewerStackList(ViewerStack[] viewerStacks) {
        this.viewerStacks = viewerStacks;
    }

}
