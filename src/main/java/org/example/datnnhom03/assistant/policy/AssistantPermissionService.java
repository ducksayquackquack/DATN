package org.example.datnnhom03.assistant.policy;

import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AssistantPermissionService {

    @Value("${assistant.enabled:false}")
    private boolean assistantEnabled;

    @Value("${assistant.allow-admin:true}")
    private boolean allowAdmin;

    @Value("${assistant.allow-employee:true}")
    private boolean allowEmployee;

    public void validateAssistantEnabled() {
        if (!assistantEnabled) {
            throw new IllegalStateException("Assistant hiện đang tắt");
        }
    }

    public void validateRoleAllowed(AssistantRole role) {
        if (role == AssistantRole.ADMIN && !allowAdmin) {
            throw new IllegalStateException("Assistant cho ADMIN hiện đang tắt");
        }
        if (role == AssistantRole.EMPLOYEE && !allowEmployee) {
            throw new IllegalStateException("Assistant cho EMPLOYEE hiện đang tắt");
        }
    }

    public boolean canUseRevenueTools(AssistantRole role) {
        return role == AssistantRole.ADMIN;
    }

    public boolean canUseEmployeeTools(AssistantRole role) {
        return role == AssistantRole.ADMIN || role == AssistantRole.EMPLOYEE;
    }

    public boolean isIntentAllowed(AssistantRole role, AssistantIntent intent) {
        if (intent == null) {
            return true;
        }
        if (role == AssistantRole.ADMIN) {
            return true;
        }
        return !isAdminOnlyIntent(intent);
    }

    public boolean isAdminOnlyIntent(AssistantIntent intent) {
        if (intent == null) return false;
        return switch (intent) {
            case REVENUE_SUMMARY, CHAT_QUEUE_SUMMARY, CREATE_VOUCHER, PRODUCT_UPSERT, PAYMENT_ANOMALY -> true;
            default -> false;
        };
    }

    public String buildRoleRestrictionMessage(AssistantRole role, AssistantIntent intent) {
        if (role == AssistantRole.ADMIN) {
            return "Yêu cầu này đang bị chặn bởi cấu hình assistant hiện tại.";
        }
        if (isAdminOnlyIntent(intent)) {
            return "Tính năng này hiện dành cho admin. Với tài khoản nhân viên, em vẫn có thể hỗ trợ tra cứu đơn, kiểm tra tồn kho, xem khách đã mua gì và soạn câu trả lời cho khách.";
        }
        return "Tài khoản hiện tại không có quyền cho yêu cầu này.";
    }
}
