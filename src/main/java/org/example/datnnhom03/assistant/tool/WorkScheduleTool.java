package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Repository.LichLamViecRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.example.datnnhom03.dto.LichLamViecDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WorkScheduleTool implements AssistantTool {

    private final LichLamViecRepository lichLamViecRepository;

    public WorkScheduleTool(LichLamViecRepository lichLamViecRepository) {
        this.lichLamViecRepository = lichLamViecRepository;
    }

    @Override
    public String getName() {
        return "get_nearest_work_schedule";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        int limit = Integer.parseInt(String.valueOf(args.getOrDefault("limit", 5)));
        Integer employeeId = intValue(args.get("employeeId"));
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> items = lichLamViecRepository.getFullSchedule()
                .stream()
                .filter(dto -> employeeId == null || java.util.Objects.equals(dto.getIdNhanVien(), employeeId))
                .sorted(Comparator.comparingLong(dto -> distanceToToday(dto, today)))
                .limit(limit)
                .map(this::toMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("count", items.size());
        result.put("employeeId", employeeId);
        result.put("items", items);
        result.put("summary", buildSummary(items, employeeId));
        return result;
    }

    private String buildSummary(List<Map<String, Object>> items, Integer employeeId) {
        if (items.isEmpty()) {
            return employeeId == null
                    ? "Hiện chưa thấy lịch làm gần nhất trong dữ liệu."
                    : "Hiện chưa thấy lịch làm gần nhất của nhân viên này trong dữ liệu.";
        }
        Map<String, Object> first = items.get(0);
        return "Lịch gần nhất hiện thuộc về nhân viên " + String.valueOf(first.getOrDefault("tenNhanVien", ""))
                + ", ca " + String.valueOf(first.getOrDefault("tenCa", ""))
                + " vào ngày " + String.valueOf(first.getOrDefault("ngayLam", "")) + ".";
    }

    private long distanceToToday(LichLamViecDTO dto, LocalDate today) {
        if (dto.getNgayLam() == null) {
            return Long.MAX_VALUE;
        }
        return Math.abs(ChronoUnit.DAYS.between(today, dto.getNgayLam()));
    }

    private Integer intValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value).replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> toMap(LichLamViecDTO dto) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", dto.getId());
        row.put("idNhanVien", dto.getIdNhanVien());
        row.put("tenNhanVien", dto.getTenNhanVien());
        row.put("idCaLam", dto.getIdCaLam());
        row.put("tenCa", dto.getTenCa());
        row.put("gioBatDau", dto.getGioBatDau());
        row.put("gioKetThuc", dto.getGioKetThuc());
        row.put("ngayLam", dto.getNgayLam());
        row.put("trangThai", dto.getTrangThai());
        return row;
    }
}