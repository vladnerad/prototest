package com.dst.server;

import com.dst.observer.EventListener;

import java.io.IOException;
import java.net.SocketException;

public interface Exchanger {
    void initListFromCache() throws IOException;   // Инициализация листа задач при подключении
    void exchange() throws IOException ;           // Метод для циклического обмена сообщениями
    EventListener getEventListener();              // Получить слушателя
    void close() throws IOException;               // Завершение при закрытии сокета
    void sendCheckMsg() throws IOException;
    void stopCheckingConn();
    void startCheckingConn();
}
