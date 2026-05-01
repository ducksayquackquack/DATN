package org.example.datnnhom03.Controller.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.*;

@RestController
@RequestMapping("/api/shipping")
@CrossOrigin("*")
public class ShippingApiController {

    private static final String PROVIDER_GHN = "GHN";
    private static final String PROVIDER_GHTK = "GHTK";

    private static final Map<String, ProviderConfig> PROVIDER_CONFIGS = Map.of(
            PROVIDER_GHN, new ProviderConfig(22000, 5500, 150),
            PROVIDER_GHTK, new ProviderConfig(15500, 3800, 110)
    );

    @Value("${shipping.origin.province:Ha Noi}")
    private String originProvince;

    @Value("${shipping.origin.district:Tay Ho}")
    private String originDistrict;

    @Value("${shipping.origin.ward:Thuy Khue}")
    private String originWard;

    @PostMapping("/quote")
    public Map<String, Object> quoteSingle(@RequestBody ShippingQuoteRequest request) {
        String provider = normalizeProvider(request.provider);
        QuoteResult quote = calculate(provider, request);
        return toMap(quote);
    }

    @PostMapping("/quote/all")
    public Map<String, Object> quoteAll(@RequestBody ShippingQuoteRequest request) {
        List<Map<String, Object>> quotes = new ArrayList<>();
        quotes.add(toMap(calculate(PROVIDER_GHN, request)));
        quotes.add(toMap(calculate(PROVIDER_GHTK, request)));
        quotes.sort(Comparator.comparingInt(item -> ((Number) item.get("fee")).intValue()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("from", buildAddressMap(originProvince, originDistrict, originWard));
        response.put("to", buildAddressMap(request.toProvince, request.toDistrict, request.toWard));
        response.put("quotes", quotes);
        return response;
    }

    private QuoteResult calculate(String provider, ShippingQuoteRequest request) {
        ProviderConfig config = PROVIDER_CONFIGS.getOrDefault(provider, PROVIDER_CONFIGS.get(PROVIDER_GHN));

        int weightGram = clampInt(request.weightGram, 100, 100000);
        int lengthCm = clampInt(request.lengthCm, 10, 200);
        int widthCm = clampInt(request.widthCm, 10, 200);
        int heightCm = clampInt(request.heightCm, 5, 200);

        double actualKg = weightGram / 1000.0;
        double volumetricKg = (lengthCm * widthCm * heightCm) / 5000.0;
        double chargeableKg = Math.max(actualKg, volumetricKg);

        int distanceKm = estimateDistanceKm(
                originProvince,
                originDistrict,
                request.toProvince,
                request.toDistrict
        );

        int baseFee = config.baseFee;
        int weightFee = (int) Math.ceil(chargeableKg * config.weightFeePerKg);
        int distanceFee = distanceKm * config.distanceFeePerKm;
        int remoteSurcharge = isRemoteDestination(request.toProvince, request.toDistrict, request.toWard) ? 9000 : 0;

        int fee = Math.max(baseFee + weightFee + distanceFee + remoteSurcharge, 12000);

        return new QuoteResult(
                provider,
                fee,
                distanceKm,
                roundTo2(actualKg),
                roundTo2(volumetricKg),
                roundTo2(chargeableKg),
                baseFee,
                weightFee,
                distanceFee,
                remoteSurcharge
        );
    }

    private int estimateDistanceKm(String fromProvince, String fromDistrict, String toProvince, String toDistrict) {
        String fromP = normalizeKey(fromProvince);
        String fromD = normalizeKey(fromDistrict);
        String toP = normalizeKey(toProvince);
        String toD = normalizeKey(toDistrict);

        if (fromP.isBlank() || toP.isBlank()) {
            return 30;
        }

        if (fromP.equals(toP) && !fromD.isBlank() && fromD.equals(toD)) {
            return 5;
        }

        if (fromP.equals(toP)) {
            return estimateIntraCityKm(fromP, fromD, toD);
        }

        Map<String, Integer> knownRoutes = new HashMap<>();
        knownRoutes.put(routeKey("ha noi", "hai phong"), 120);
        knownRoutes.put(routeKey("ha noi", "quang ninh"), 160);
        knownRoutes.put(routeKey("ha noi", "bac ninh"), 32);
        knownRoutes.put(routeKey("ha noi", "bac giang"), 55);
        knownRoutes.put(routeKey("ha noi", "thai nguyen"), 80);
        knownRoutes.put(routeKey("ha noi", "nam dinh"), 90);
        knownRoutes.put(routeKey("ha noi", "hai duong"), 60);
        knownRoutes.put(routeKey("ha noi", "vinh phuc"), 55);
        knownRoutes.put(routeKey("ha noi", "hung yen"), 65);
        knownRoutes.put(routeKey("ha noi", "ha nam"), 60);
        knownRoutes.put(routeKey("ha noi", "ninh binh"), 95);
        knownRoutes.put(routeKey("ha noi", "phu tho"), 85);
        knownRoutes.put(routeKey("ha noi", "hoa binh"), 75);
        knownRoutes.put(routeKey("ha noi", "thanh hoa"), 155);
        knownRoutes.put(routeKey("ha noi", "nghe an"), 300);
        knownRoutes.put(routeKey("ha noi", "da nang"), 770);
        knownRoutes.put(routeKey("ha noi", "ho chi minh"), 1700);
        knownRoutes.put(routeKey("da nang", "ho chi minh"), 960);
        knownRoutes.put(routeKey("ho chi minh", "binh duong"), 30);
        knownRoutes.put(routeKey("ho chi minh", "dong nai"), 35);
        knownRoutes.put(routeKey("ho chi minh", "long an"), 50);
        knownRoutes.put(routeKey("ho chi minh", "vung tau"), 95);
        knownRoutes.put(routeKey("ho chi minh", "can tho"), 170);

        String directKey = routeKey(fromP, toP);
        if (knownRoutes.containsKey(directKey)) {
            return knownRoutes.get(directKey);
        }

        String fromRegion = regionOf(fromP);
        String toRegion = regionOf(toP);

        if (fromRegion.equals(toRegion)) {
            return 140;
        }

        if (("north".equals(fromRegion) && "central".equals(toRegion))
                || ("central".equals(fromRegion) && "north".equals(toRegion))) {
            return 650;
        }

        if (("south".equals(fromRegion) && "central".equals(toRegion))
                || ("central".equals(fromRegion) && "south".equals(toRegion))) {
            return 780;
        }

        return 1650;
    }

    private int estimateIntraCityKm(String provinceKey, String fromDistrict, String toDistrict) {
        if (fromDistrict.isBlank() || toDistrict.isBlank()) {
            return 15;
        }
        boolean fromInner = isInnerDistrict(provinceKey, fromDistrict);
        boolean toInner = isInnerDistrict(provinceKey, toDistrict);
        if (fromInner && toInner) return 8;
        if (fromInner || toInner) return 15;
        return 22;
    }

    private boolean isInnerDistrict(String province, String district) {
        if (province.contains("ha noi")) {
            return district.contains("hoan kiem") || district.contains("ba dinh")
                    || district.contains("dong da") || district.contains("hai ba trung")
                    || district.contains("tay ho") || district.contains("cau giay")
                    || district.contains("thanh xuan") || district.contains("hoang mai")
                    || district.contains("long bien") || district.contains("nam tu liem")
                    || district.contains("bac tu liem") || district.contains("ha dong");
        }
        if (province.contains("ho chi minh")) {
            return district.contains("quan 1") || district.contains("quan 3")
                    || district.contains("quan 5") || district.contains("quan 10")
                    || district.contains("phu nhuan") || district.contains("binh thanh")
                    || district.contains("tan binh") || district.contains("go vap")
                    || district.contains("thu duc");
        }
        return false;
    }

    private boolean isRemoteDestination(String province, String district, String ward) {
        String all = String.join(" ",
                normalizeKey(province),
                normalizeKey(district),
                normalizeKey(ward)
        );
        return all.contains("dao")
                || all.contains("con dao")
                || all.contains("ly son")
                || all.contains("phu quy")
                || all.contains("bac ai")
                || all.contains("muong te")
                || all.contains("dong van");
    }

    private String normalizeProvider(String provider) {
        String normalized = String.valueOf(provider == null ? "" : provider).trim().toUpperCase(Locale.ROOT);
        if (PROVIDER_GHTK.equals(normalized)) return PROVIDER_GHTK;
        return PROVIDER_GHN;
    }

    private int clampInt(Integer value, int min, int max) {
        int v = value == null ? min : value;
        return Math.max(min, Math.min(max, v));
    }

    private String normalizeKey(String value) {
        String raw = String.valueOf(value == null ? "" : value).trim().toLowerCase(Locale.ROOT);
        if (raw.isBlank()) return "";
        String noAccent = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccent.replace("đ", "d").replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private String regionOf(String provinceKey) {
        if (provinceKey.contains("ha noi") || provinceKey.contains("hai phong") || provinceKey.contains("quang ninh")
                || provinceKey.contains("bac ninh") || provinceKey.contains("thai nguyen") || provinceKey.contains("nam dinh")
                || provinceKey.contains("hai duong") || provinceKey.contains("vinh phuc") || provinceKey.contains("hung yen")
                || provinceKey.contains("ha nam") || provinceKey.contains("ninh binh") || provinceKey.contains("bac giang")
                || provinceKey.contains("phu tho") || provinceKey.contains("hoa binh") || provinceKey.contains("lang son")
                || provinceKey.contains("yen bai") || provinceKey.contains("lao cai") || provinceKey.contains("son la")
                || provinceKey.contains("dien bien") || provinceKey.contains("lai chau") || provinceKey.contains("ha giang")
                || provinceKey.contains("cao bang") || provinceKey.contains("bac kan") || provinceKey.contains("tuyen quang")
                || provinceKey.contains("thai binh")) {
            return "north";
        }

        if (provinceKey.contains("da nang") || provinceKey.contains("hue") || provinceKey.contains("quang nam")
                || provinceKey.contains("quang ngai") || provinceKey.contains("khanh hoa") || provinceKey.contains("nghe an")
                || provinceKey.contains("ha tinh") || provinceKey.contains("ninh thuan") || provinceKey.contains("binh dinh")
                || provinceKey.contains("thanh hoa") || provinceKey.contains("quang binh") || provinceKey.contains("quang tri")
                || provinceKey.contains("phu yen") || provinceKey.contains("binh thuan") || provinceKey.contains("gia lai")
                || provinceKey.contains("kon tum") || provinceKey.contains("dak lak") || provinceKey.contains("dak nong")
                || provinceKey.contains("lam dong")) {
            return "central";
        }

        return "south";
    }

    private String routeKey(String a, String b) {
        String left = normalizeKey(a);
        String right = normalizeKey(b);
        if (left.compareTo(right) <= 0) {
            return left + "::" + right;
        }
        return right + "::" + left;
    }

    private double roundTo2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Map<String, Object> toMap(QuoteResult quote) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("provider", quote.provider);
        map.put("fee", quote.fee);
        map.put("distanceKm", quote.distanceKm);

        Map<String, Object> packageMap = new LinkedHashMap<>();
        packageMap.put("actualWeightKg", quote.actualWeightKg);
        packageMap.put("volumetricWeightKg", quote.volumetricWeightKg);
        packageMap.put("chargeableWeightKg", quote.chargeableWeightKg);
        map.put("packageInfo", packageMap);

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("baseFee", quote.baseFee);
        breakdown.put("weightFee", quote.weightFee);
        breakdown.put("distanceFee", quote.distanceFee);
        breakdown.put("remoteSurcharge", quote.remoteSurcharge);
        map.put("breakdown", breakdown);
        return map;
    }

    private Map<String, Object> buildAddressMap(String province, String district, String ward) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("province", String.valueOf(province == null ? "" : province).trim());
        map.put("district", String.valueOf(district == null ? "" : district).trim());
        map.put("ward", String.valueOf(ward == null ? "" : ward).trim());
        return map;
    }

    private static class ProviderConfig {
        final int baseFee;
        final int weightFeePerKg;
        final int distanceFeePerKm;

        ProviderConfig(int baseFee, int weightFeePerKg, int distanceFeePerKm) {
            this.baseFee = baseFee;
            this.weightFeePerKg = weightFeePerKg;
            this.distanceFeePerKm = distanceFeePerKm;
        }
    }

    private static class QuoteResult {
        final String provider;
        final int fee;
        final int distanceKm;
        final double actualWeightKg;
        final double volumetricWeightKg;
        final double chargeableWeightKg;
        final int baseFee;
        final int weightFee;
        final int distanceFee;
        final int remoteSurcharge;

        QuoteResult(String provider,
                    int fee,
                    int distanceKm,
                    double actualWeightKg,
                    double volumetricWeightKg,
                    double chargeableWeightKg,
                    int baseFee,
                    int weightFee,
                    int distanceFee,
                    int remoteSurcharge) {
            this.provider = provider;
            this.fee = fee;
            this.distanceKm = distanceKm;
            this.actualWeightKg = actualWeightKg;
            this.volumetricWeightKg = volumetricWeightKg;
            this.chargeableWeightKg = chargeableWeightKg;
            this.baseFee = baseFee;
            this.weightFee = weightFee;
            this.distanceFee = distanceFee;
            this.remoteSurcharge = remoteSurcharge;
        }
    }

    public static class ShippingQuoteRequest {
        public String provider;
        public String toProvince;
        public String toDistrict;
        public String toWard;
        public Integer weightGram;
        public Integer lengthCm;
        public Integer widthCm;
        public Integer heightCm;
    }
}
