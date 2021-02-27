package cardgame;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public class Player {

    private final String name;
    private final PropertyChangeSupport propChangeSupport;
    private final List<Card> stack;

    public Player(String name) {
        this.name = name;
        this.stack = new ArrayList<>();
        propChangeSupport = new PropertyChangeSupport(this);
    }

    public void addPropertyChangeListener(PropertyChangeListener pl) {
        propChangeSupport.addPropertyChangeListener(pl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pl) {
        propChangeSupport.removePropertyChangeListener(pl);
    }

    public String getName() {
        return name;
    }

    public void clearStack() {
        stack.clear();
    }

    public List<Card> getStack() {
        return stack;
    }

    @Override
    public String toString() {
        return name;
    }

    protected void firePropertyChange(String popertyName, Object oldValue, Object newValue) {
        propChangeSupport.firePropertyChange(popertyName, oldValue, newValue);
    }
}
