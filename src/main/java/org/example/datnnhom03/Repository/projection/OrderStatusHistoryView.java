package org.example.datnnhom03.Repository.projection;

import java.time.LocalDateTime;

public interface OrderStatusHistoryView {
    LocalDateTime getChangedAt();
    String getFromStatus();
    String getToStatus();
    String getNote();
}
