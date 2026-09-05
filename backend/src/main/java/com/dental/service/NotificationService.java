package com.dental.service;

import com.dental.dao.DAOFactory;
import com.dental.model.Notification;

import java.util.List;

/** Read access to the queued reminder / confirmation messages (Reminders screen). */
public class NotificationService {

    public List<Notification> recent() {
        return DAOFactory.notifications().findAll();
    }
}
