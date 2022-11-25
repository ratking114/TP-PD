package shared;

import java.io.Serializable;
import java.util.List;

public class EventFilterViewModel implements Serializable {
    public List<EventInfo> eventInfoList;
    public Error_Messages error_message;

    public EventFilterViewModel(List<EventInfo> eventInfoList, Error_Messages error_message) {
        this.eventInfoList = eventInfoList;
        this.error_message = error_message;
    }
}

