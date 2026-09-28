package com.ilia.advanceclock;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Small offline IANA timezone database for prayer locations.
 *
 * Most countries are single-zone and are resolved directly from ISO-3166 country code.
 * Large multi-zone countries use province/state names plus coordinates. Online lookup is only
 * needed when a location cannot be resolved confidently from this table.
 */
final class OfflineTimeZoneResolver {
    private static final Map<String, String> SINGLE = new HashMap<>();

    static {
        put("AD","Europe/Andorra"); put("AE","Asia/Dubai"); put("AF","Asia/Kabul");
        put("AL","Europe/Tirane"); put("AM","Asia/Yerevan"); put("AO","Africa/Luanda");
        put("AR","America/Argentina/Buenos_Aires"); put("AT","Europe/Vienna");
        put("AZ","Asia/Baku"); put("BA","Europe/Sarajevo"); put("BB","America/Barbados");
        put("BD","Asia/Dhaka"); put("BE","Europe/Brussels"); put("BF","Africa/Ouagadougou");
        put("BG","Europe/Sofia"); put("BH","Asia/Bahrain"); put("BI","Africa/Bujumbura");
        put("BJ","Africa/Porto-Novo"); put("BN","Asia/Brunei"); put("BO","America/La_Paz");
        put("BT","Asia/Thimphu"); put("BW","Africa/Gaborone"); put("BY","Europe/Minsk");
        put("BZ","America/Belize"); put("CH","Europe/Zurich"); put("CI","Africa/Abidjan");
        put("CM","Africa/Douala"); put("CN","Asia/Shanghai"); put("CO","America/Bogota");
        put("CR","America/Costa_Rica"); put("CU","America/Havana"); put("CV","Atlantic/Cape_Verde");
        put("CZ","Europe/Prague"); put("DE","Europe/Berlin"); put("DJ","Africa/Djibouti");
        put("DK","Europe/Copenhagen"); put("DO","America/Santo_Domingo"); put("DZ","Africa/Algiers");
        put("EE","Europe/Tallinn"); put("EG","Africa/Cairo"); put("ER","Africa/Asmara");
        put("ET","Africa/Addis_Ababa"); put("FI","Europe/Helsinki"); put("FJ","Pacific/Fiji");
        put("FR","Europe/Paris"); put("GA","Africa/Libreville"); put("GB","Europe/London");
        put("GE","Asia/Tbilisi"); put("GH","Africa/Accra"); put("GR","Europe/Athens");
        put("GT","America/Guatemala"); put("GY","America/Guyana"); put("HN","America/Tegucigalpa");
        put("HR","Europe/Zagreb"); put("HT","America/Port-au-Prince"); put("HU","Europe/Budapest");
        put("IE","Europe/Dublin"); put("IL","Asia/Jerusalem"); put("IN","Asia/Kolkata");
        put("IQ","Asia/Baghdad"); put("IR","Asia/Tehran"); put("IS","Atlantic/Reykjavik");
        put("IT","Europe/Rome"); put("JM","America/Jamaica"); put("JO","Asia/Amman");
        put("JP","Asia/Tokyo"); put("KE","Africa/Nairobi"); put("KG","Asia/Bishkek");
        put("KH","Asia/Phnom_Penh"); put("KP","Asia/Pyongyang"); put("KR","Asia/Seoul");
        put("KW","Asia/Kuwait"); put("KZ","Asia/Almaty"); put("LA","Asia/Vientiane");
        put("LB","Asia/Beirut"); put("LK","Asia/Colombo"); put("LR","Africa/Monrovia");
        put("LS","Africa/Maseru"); put("LT","Europe/Vilnius"); put("LU","Europe/Luxembourg");
        put("LV","Europe/Riga"); put("LY","Africa/Tripoli"); put("MA","Africa/Casablanca");
        put("MD","Europe/Chisinau"); put("ME","Europe/Podgorica"); put("MG","Indian/Antananarivo");
        put("MK","Europe/Skopje"); put("ML","Africa/Bamako"); put("MM","Asia/Yangon");
        put("MT","Europe/Malta"); put("MU","Indian/Mauritius"); put("MV","Indian/Maldives");
        put("MW","Africa/Blantyre"); put("MY","Asia/Kuala_Lumpur"); put("MZ","Africa/Maputo");
        put("NA","Africa/Windhoek"); put("NE","Africa/Niamey"); put("NG","Africa/Lagos");
        put("NI","America/Managua"); put("NL","Europe/Amsterdam"); put("NO","Europe/Oslo");
        put("NP","Asia/Kathmandu"); put("OM","Asia/Muscat"); put("PA","America/Panama");
        put("PE","America/Lima"); put("PH","Asia/Manila"); put("PK","Asia/Karachi");
        put("PL","Europe/Warsaw"); put("PR","America/Puerto_Rico"); put("PS","Asia/Gaza");
        put("PY","America/Asuncion"); put("QA","Asia/Qatar"); put("RO","Europe/Bucharest");
        put("RS","Europe/Belgrade"); put("RW","Africa/Kigali"); put("SA","Asia/Riyadh");
        put("SE","Europe/Stockholm"); put("SG","Asia/Singapore"); put("SI","Europe/Ljubljana");
        put("SK","Europe/Bratislava"); put("SL","Africa/Freetown"); put("SN","Africa/Dakar");
        put("SO","Africa/Mogadishu"); put("SR","America/Paramaribo"); put("SS","Africa/Juba");
        put("SV","America/El_Salvador"); put("SY","Asia/Damascus"); put("SZ","Africa/Mbabane");
        put("TD","Africa/Ndjamena"); put("TG","Africa/Lome"); put("TH","Asia/Bangkok");
        put("TJ","Asia/Dushanbe"); put("TL","Asia/Dili"); put("TM","Asia/Ashgabat");
        put("TN","Africa/Tunis"); put("TR","Europe/Istanbul"); put("TT","America/Port_of_Spain");
        put("TW","Asia/Taipei"); put("TZ","Africa/Dar_es_Salaam"); put("UA","Europe/Kyiv");
        put("UG","Africa/Kampala"); put("UY","America/Montevideo"); put("UZ","Asia/Tashkent");
        put("VA","Europe/Rome"); put("VE","America/Caracas"); put("VN","Asia/Ho_Chi_Minh");
        put("YE","Asia/Aden"); put("ZA","Africa/Johannesburg"); put("ZM","Africa/Lusaka");
        put("ZW","Africa/Harare");

        // Common territories with their own ISO country codes.
        put("AW","America/Aruba"); put("BM","Atlantic/Bermuda"); put("BQ","America/Kralendijk");
        put("BS","America/Nassau"); put("CW","America/Curacao"); put("FO","Atlantic/Faroe");
        put("GG","Europe/Guernsey"); put("GI","Europe/Gibraltar"); put("GP","America/Guadeloupe");
        put("HK","Asia/Hong_Kong"); put("IM","Europe/Isle_of_Man"); put("JE","Europe/Jersey");
        put("KY","America/Cayman"); put("MO","Asia/Macau"); put("MQ","America/Martinique");
        put("NC","Pacific/Noumea"); put("RE","Indian/Reunion"); put("SX","America/Lower_Princes");
        put("TC","America/Grand_Turk"); put("TO","Pacific/Tongatapu"); put("TV","Pacific/Funafuti");
        put("VG","America/Tortola"); put("VI","America/St_Thomas"); put("VU","Pacific/Efate");
        put("WS","Pacific/Apia"); put("YT","Indian/Mayotte");
    }

    private OfflineTimeZoneResolver() {}

    private static void put(String code, String zone) { SINGLE.put(code, zone); }

    static String resolve(String countryCode, String region, double latitude, double longitude) {
        String code = countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.US);
        String area = normalize(region);
        String multi = resolveMultiZone(code, area, latitude, longitude);
        if (!multi.isEmpty()) return valid(multi) ? multi : "";
        String single = SINGLE.get(code);
        return valid(single) ? single : "";
    }

    static String resolveFromLabel(String label, double latitude, double longitude) {
        String text = normalize(label);
        if (containsAny(text, "canada", "کانادا")) return resolve("CA", text, latitude, longitude);
        if (containsAny(text, "united states", "usa", "u.s.", "ایالات متحده", "آمریکا"))
            return resolve("US", text, latitude, longitude);
        if (containsAny(text, "australia", "استرالیا")) return resolve("AU", text, latitude, longitude);
        if (containsAny(text, "russia", "россия", "روسیه")) return resolve("RU", text, latitude, longitude);
        if (containsAny(text, "brazil", "brasil", "برزیل")) return resolve("BR", text, latitude, longitude);
        if (containsAny(text, "mexico", "méxico", "مکزیک")) return resolve("MX", text, latitude, longitude);
        if (containsAny(text, "indonesia", "اندونزی")) return resolve("ID", text, latitude, longitude);
        if (containsAny(text, "iran", "ایران")) return "Asia/Tehran";
        return "";
    }

    private static String resolveMultiZone(
            String code, String area, double latitude, double longitude) {
        switch (code) {
            case "CA": return canada(area, longitude);
            case "US": return unitedStates(area, latitude, longitude);
            case "AU": return australia(area, longitude);
            case "RU": return russia(longitude);
            case "BR": return brazil(area, longitude);
            case "MX": return mexico(area, longitude);
            case "ID": return indonesia(longitude);
            case "ES": return containsAny(area, "canary", "canarias")
                    ? "Atlantic/Canary" : "Europe/Madrid";
            case "PT":
                if (containsAny(area, "azores", "açores")) return "Atlantic/Azores";
                if (containsAny(area, "madeira")) return "Atlantic/Madeira";
                return "Europe/Lisbon";
            case "CL": return longitude < -90.0 ? "Pacific/Easter" : "America/Santiago";
            case "EC": return longitude < -88.0 ? "Pacific/Galapagos" : "America/Guayaquil";
            case "NZ": return longitude < -175.5 ? "Pacific/Chatham" : "Pacific/Auckland";
            case "CD": return longitude < 23.0 ? "Africa/Kinshasa" : "Africa/Lubumbashi";
            case "PG": return longitude > 153.0 ? "Pacific/Bougainville" : "Pacific/Port_Moresby";
            case "GL": return "America/Nuuk";
            default: return "";
        }
    }

    private static String canada(String a, double lon) {
        if (containsAny(a, "newfoundland", "terre-neuve")) return "America/St_Johns";
        if (containsAny(a, "nova scotia", "nouvelle-écosse", "new brunswick", "nouveau-brunswick",
                "prince edward", "île-du-prince")) return "America/Halifax";
        if (containsAny(a, "quebec", "québec", "ontario")) return "America/Toronto";
        if (containsAny(a, "manitoba")) return "America/Winnipeg";
        if (containsAny(a, "saskatchewan")) return "America/Regina";
        if (containsAny(a, "alberta", "northwest territories", "territoires du nord-ouest"))
            return "America/Edmonton";
        if (containsAny(a, "british columbia", "colombie-britannique")) return "America/Vancouver";
        if (containsAny(a, "yukon")) return "America/Whitehorse";
        if (containsAny(a, "nunavut")) return lon < -95.0 ? "America/Rankin_Inlet" : "America/Iqaluit";
        // Coordinate fallback is useful when Nominatim omits the province.
        if (lon < -120) return "America/Vancouver";
        if (lon < -110) return "America/Edmonton";
        if (lon < -100) return "America/Regina";
        if (lon < -90) return "America/Winnipeg";
        if (lon < -60) return "America/Toronto";
        return "America/Halifax";
    }

    private static String unitedStates(String a, double lat, double lon) {
        if (containsAny(a, "hawaii")) return "Pacific/Honolulu";
        if (containsAny(a, "alaska")) return lon < -169 ? "America/Adak" : "America/Anchorage";
        if (containsAny(a, "arizona")) return "America/Phoenix";
        if (containsAny(a, "california", "washington", "oregon", "nevada")) return "America/Los_Angeles";
        if (containsAny(a, "colorado", "utah", "montana", "wyoming", "new mexico", "idaho"))
            return "America/Denver";
        if (containsAny(a, "texas", "oklahoma", "kansas", "nebraska", "south dakota", "north dakota",
                "minnesota", "iowa", "missouri", "arkansas", "louisiana", "wisconsin", "illinois",
                "mississippi", "alabama")) return "America/Chicago";
        if (containsAny(a, "new york", "new jersey", "pennsylvania", "ohio", "michigan", "indiana",
                "kentucky", "tennessee", "georgia", "florida", "virginia", "west virginia",
                "north carolina", "south carolina", "maryland", "delaware", "connecticut",
                "massachusetts", "vermont", "new hampshire", "maine", "rhode island"))
            return "America/New_York";
        if (lon < -115) return "America/Los_Angeles";
        if (lon < -103) return "America/Denver";
        if (lon < -86) return "America/Chicago";
        return "America/New_York";
    }

    private static String australia(String a, double lon) {
        if (containsAny(a, "western australia")) return "Australia/Perth";
        if (containsAny(a, "northern territory")) return "Australia/Darwin";
        if (containsAny(a, "south australia")) return "Australia/Adelaide";
        if (containsAny(a, "queensland")) return "Australia/Brisbane";
        if (containsAny(a, "tasmania")) return "Australia/Hobart";
        if (containsAny(a, "victoria")) return "Australia/Melbourne";
        if (containsAny(a, "new south wales", "australian capital territory")) return "Australia/Sydney";
        if (lon < 129) return "Australia/Perth";
        if (lon < 138) return "Australia/Darwin";
        return "Australia/Sydney";
    }

    private static String russia(double lon) {
        if (lon < 43) return "Europe/Moscow";
        if (lon < 55) return "Europe/Samara";
        if (lon < 69) return "Asia/Yekaterinburg";
        if (lon < 82) return "Asia/Omsk";
        if (lon < 94) return "Asia/Novosibirsk";
        if (lon < 110) return "Asia/Krasnoyarsk";
        if (lon < 121) return "Asia/Irkutsk";
        if (lon < 136) return "Asia/Yakutsk";
        if (lon < 151) return "Asia/Vladivostok";
        if (lon < 163) return "Asia/Magadan";
        return "Asia/Kamchatka";
    }

    private static String brazil(String a, double lon) {
        if (containsAny(a, "acre")) return "America/Rio_Branco";
        if (containsAny(a, "amazonas", "rondônia", "rondonia", "roraima", "mato grosso"))
            return "America/Manaus";
        if (lon < -59) return "America/Rio_Branco";
        if (lon < -50) return "America/Manaus";
        return "America/Sao_Paulo";
    }

    private static String mexico(String a, double lon) {
        if (containsAny(a, "quintana roo")) return "America/Cancun";
        if (containsAny(a, "baja california")) return "America/Tijuana";
        if (containsAny(a, "sonora")) return "America/Hermosillo";
        if (containsAny(a, "chihuahua")) return "America/Chihuahua";
        if (lon < -112) return "America/Tijuana";
        if (lon < -104) return "America/Chihuahua";
        return "America/Mexico_City";
    }

    private static String indonesia(double lon) {
        if (lon < 120) return "Asia/Jakarta";
        if (lon < 135) return "Asia/Makassar";
        return "Asia/Jayapura";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    private static boolean valid(String id) {
        if (id == null || id.isEmpty()) return false;
        TimeZone zone = TimeZone.getTimeZone(id);
        return !"GMT".equals(zone.getID()) || "GMT".equalsIgnoreCase(id);
    }
}
