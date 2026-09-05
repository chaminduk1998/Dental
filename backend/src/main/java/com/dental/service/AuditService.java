package com.dental.service;

import com.dental.dao.DAOFactory;
import com.dental.model.AuditLog;

import java.util.List;

/** Read access to the audit trail (admin panel). */
public class AuditService {

    public List<AuditLog> recent(int limit, String entityFilter) {
        return DAOFactory.auditLogs().findRecent(limit <= 0 ? 200 : limit, entityFilter);
    }
}
