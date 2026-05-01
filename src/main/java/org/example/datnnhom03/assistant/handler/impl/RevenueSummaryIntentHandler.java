
package org.example.datnnhom03.assistant.handler.impl;

import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.handler.AssistantRequestContext;
import org.example.datnnhom03.assistant.handler.IntentHandler;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.service.ToolDispatcher;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RevenueSummaryIntentHandler extends AbstractToolIntentHandler implements IntentHandler {

    private static final Pattern YEAR_PATTERN = Pattern.compile("(20\\d{2})");
    private static final Pattern MONTH_YEAR_PATTERN = Pattern.compile("th[aá]ng\\s*(1[0-2]|0?[1-9])(?:\\s*n[aă]m\\s*(20\\d{2}))?");
    private static final Pattern QUARTER_YEAR_PATTERN = Pattern.compile("qu[yý]\\s*([1-4])(?:\\s*n[aă]m\\s*(20\\d{2}))?");
    private static final Pattern DAY_MONTH_YEAR_PATTERN = Pattern.compile("(?:ng[aà]y\\s*)?(\\d{1,2})[\\/\\-](\\d{1,2})[\\/\\-](20\\d{2})");

    public RevenueSummaryIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }

    @Override public AssistantIntent supportedIntent() { return AssistantIntent.REVENUE_SUMMARY; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        String normalized = normalize(context.getMessage());
        Map<String, Object> range = resolveRange(normalized);

        Map<String, Object> data = call(context, "get_revenue_summary", range, result);
        result.getPayload().put("revenueSummary", data);
        result.getPayload().put("revenueChart", new LinkedHashMap<>(Map.of(
                "from", range.get("from"),
                "to", range.get("to"),
                "period", range.get("period"),
                "label", range.get("label"),
                "granularity", range.get("chartGranularity")
        )));

        result.setGrounded(data.containsKey("totalRevenue"));
        String label = stringValue(range.getOrDefault("label", "tháng này"));
        String from = stringValue(range.get("from"));
        String to = stringValue(range.get("to"));
        result.setReply("Doanh thu " + label + " (" + from + " đến " + to + "): "
                + money(data.get("totalRevenue"))
                + ", số đơn hoàn thành " + stringValue(data.getOrDefault("totalOrders", data.get("orderCount")))
                + ", giá trị trung bình/đơn " + money(data.get("averageOrderValue")) + ".");
        return result;
    }

    private Map<String, Object> resolveRange(String normalized) {
        LocalDate now = LocalDate.now();
        Map<String, Object> args = new LinkedHashMap<>();

        LocalDate from;
        LocalDate to;
        String label;
        String period;
        String granularity;

        Matcher fullDateMatcher = DAY_MONTH_YEAR_PATTERN.matcher(normalized);
        if (fullDateMatcher.find()) {
            int day = Integer.parseInt(fullDateMatcher.group(1));
            int month = Integer.parseInt(fullDateMatcher.group(2));
            int year = Integer.parseInt(fullDateMatcher.group(3));
            LocalDate exact = LocalDate.of(year, month, day);
            from = exact;
            to = exact;
            label = "ngày " + exact;
            period = "day";
            granularity = "hour";
            return fillArgs(args, from, to, label, period, granularity);
        }

        if (containsAny(normalized, "hôm qua", "hom qua")) {
            LocalDate target = now.minusDays(1);
            from = target;
            to = target;
            label = "ngày hôm qua";
            period = "day";
            granularity = "hour";
            return fillArgs(args, from, to, label, period, granularity);
        }

        if (containsAny(normalized, "hôm nay", "hom nay", "ngày hôm nay", "ngay hom nay")) {
            from = now;
            to = now;
            label = "ngày hôm nay";
            period = "day";
            granularity = "hour";
            return fillArgs(args, from, to, label, period, granularity);
        }

        if (containsAny(normalized, "tuần trước", "tuan truoc")) {
            LocalDate base = now.minusWeeks(1);
            from = base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            to = from.plusDays(6);
            label = "tuần trước";
            period = "week";
            granularity = "day";
            return fillArgs(args, from, to, label, period, granularity);
        }

        if (containsAny(normalized, "tuần này", "tuan nay")) {
            from = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            to = from.plusDays(6);
            label = "tuần này";
            period = "week";
            granularity = "day";
            return fillArgs(args, from, to, label, period, granularity);
        }

        Matcher quarterMatcher = QUARTER_YEAR_PATTERN.matcher(normalized);
        if (quarterMatcher.find()) {
            int quarter = Integer.parseInt(quarterMatcher.group(1));
            int year = quarterMatcher.group(2) != null ? Integer.parseInt(quarterMatcher.group(2)) : now.getYear();
            int startMonth = (quarter - 1) * 3 + 1;
            from = LocalDate.of(year, startMonth, 1);
            to = from.plusMonths(3).minusDays(1);
            label = "quý " + quarter + " năm " + year;
            period = "quarter";
            granularity = "month";
            return fillArgs(args, from, to, label, period, granularity);
        }

        Matcher monthYearMatcher = MONTH_YEAR_PATTERN.matcher(normalized);
        if (monthYearMatcher.find()) {
            int month = Integer.parseInt(monthYearMatcher.group(1));
            int year = monthYearMatcher.group(2) != null ? Integer.parseInt(monthYearMatcher.group(2)) : now.getYear();
            from = LocalDate.of(year, month, 1);
            to = from.withDayOfMonth(from.lengthOfMonth());
            label = "tháng " + month + " năm " + year;
            period = "month";
            granularity = "day";
            return fillArgs(args, from, to, label, period, granularity);
        }

        if (containsAny(normalized, "tháng trước", "thang truoc")) {
            LocalDate base = now.minusMonths(1);
            from = base.withDayOfMonth(1);
            to = from.withDayOfMonth(from.lengthOfMonth());
            label = "tháng trước";
            period = "month";
            granularity = "day";
            return fillArgs(args, from, to, label, period, granularity);
        }

        if (containsAny(normalized, "tháng này", "thang nay")) {
            from = now.withDayOfMonth(1);
            to = now.withDayOfMonth(now.lengthOfMonth());
            label = "tháng này";
            period = "month";
            granularity = "day";
            return fillArgs(args, from, to, label, period, granularity);
        }

        if (containsAny(normalized, "năm trước", "nam truoc")) {
            int year = now.getYear() - 1;
            from = LocalDate.of(year, Month.JANUARY, 1);
            to = LocalDate.of(year, Month.DECEMBER, 31);
            label = "năm trước";
            period = "year";
            granularity = "month";
            return fillArgs(args, from, to, label, period, granularity);
        }

        Matcher yearMatcher = YEAR_PATTERN.matcher(normalized);
        if (containsAny(normalized, "năm", "nam") && yearMatcher.find()) {
            int year = Integer.parseInt(yearMatcher.group(1));
            from = LocalDate.of(year, Month.JANUARY, 1);
            to = LocalDate.of(year, Month.DECEMBER, 31);
            label = "năm " + year;
            period = "year";
            granularity = "month";
            return fillArgs(args, from, to, label, period, granularity);
        }

        from = now.withDayOfMonth(1);
        to = now.withDayOfMonth(now.lengthOfMonth());
        label = "tháng này";
        period = "month";
        granularity = "day";
        return fillArgs(args, from, to, label, period, granularity);
    }

    private Map<String, Object> fillArgs(Map<String, Object> args, LocalDate from, LocalDate to,
                                         String label, String period, String granularity) {
        args.put("from", from.toString());
        args.put("to", to.toString());
        args.put("label", label);
        args.put("period", period);
        args.put("chartGranularity", granularity);
        return args;
    }
}
