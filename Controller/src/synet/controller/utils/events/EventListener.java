package synet.controller.utils.events;

public interface EventListener<M> {
    void onNotify(Object source, M message);
}
