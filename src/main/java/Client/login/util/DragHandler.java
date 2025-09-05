package Client.login.util;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class DragHandler implements EventHandler<MouseEvent> {
    private final Stage stage;
    private double ox; private double oy;
    public DragHandler(Stage stage){ this.stage = stage; }
    @Override
    public void handle(MouseEvent e){
        if(e.getEventType()==MouseEvent.MOUSE_PRESSED){ ox = e.getX(); oy = e.getY(); }
        else if(e.getEventType()==MouseEvent.MOUSE_DRAGGED){ stage.setX(e.getScreenX()-ox); stage.setY(e.getScreenY()-oy); }
    }
}

