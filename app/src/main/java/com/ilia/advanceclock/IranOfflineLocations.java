package com.ilia.advanceclock;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class IranOfflineLocations {
    static final class Location {
        final int labelResId;
        final String aliases;
        final double latitude;
        final double longitude;

        Location(int labelResId, String aliases, double latitude, double longitude) {
            this.labelResId = labelResId;
            this.aliases = aliases;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        String label() {
            return AppString.get(labelResId);
        }

        String searchText() {
            return normalize(label() + " " + aliases);
        }
    }

    private static final Location[] LOCATIONS = {
            city(R.string.iran_location_text_001, "Tehran", 35.6892, 51.3890),
            city(R.string.iran_location_text_002, "Karaj Alborz", 35.8400, 50.9391),
            city(R.string.iran_location_text_003, "Qom", 34.6416, 50.8746),
            city(R.string.iran_location_text_004, "Isfahan Esfahan", 32.6546, 51.6680),
            city(R.string.iran_location_text_005, "Shiraz Fars", 29.5918, 52.5837),
            city(R.string.iran_location_text_006, "Mashhad Razavi Khorasan", 36.2605, 59.6168),
            city(R.string.iran_location_text_007, "Tabriz East Azerbaijan", 38.0800, 46.2919),
            city(R.string.iran_location_text_008, "Urmia Orumiyeh West Azerbaijan", 37.5527, 45.0761),
            city(R.string.iran_location_text_009, "Ardabil", 38.2498, 48.2933),
            city(R.string.iran_location_text_010, "Rasht Gilan", 37.2808, 49.5832),
            city(R.string.iran_location_text_011, "Sari Mazandaran", 36.5659, 53.0586),
            city(R.string.iran_location_text_012, "Gorgan Golestan", 36.8427, 54.4439),
            city(R.string.iran_location_text_013, "Semnan", 35.5769, 53.3921),
            city(R.string.iran_location_text_014, "Qazvin Ghazvin", 36.2688, 50.0041),
            city(R.string.iran_location_text_015, "Zanjan", 36.6736, 48.4787),
            city(R.string.iran_location_text_016, "Hamadan Hamedan", 34.7989, 48.5150),
            city(R.string.iran_location_text_017, "Kermanshah", 34.3142, 47.0650),
            city(R.string.iran_location_text_018, "Sanandaj Kurdistan", 35.3219, 46.9862),
            city(R.string.iran_location_text_019, "Ilam", 33.6374, 46.4227),
            city(R.string.iran_location_text_020, "Khorramabad Khoramabad Lorestan خرم اباد", 33.4878, 48.3558),
            city(R.string.iran_location_text_021, "Ahvaz Ahwaz Khuzestan", 31.3183, 48.6706),
            city(R.string.iran_location_text_022, "Shahrekord Shahr-e Kord", 32.3256, 50.8644),
            city(R.string.iran_location_text_023, "Yasuj", 30.6682, 51.5879),
            city(R.string.iran_location_text_024, "Bushehr", 28.9234, 50.8203),
            city(R.string.iran_location_text_025, "Bandar Abbas Hormozgan بندر عباس", 27.1832, 56.2666),
            city(R.string.iran_location_text_026, "Kerman", 30.2839, 57.0834),
            city(R.string.iran_location_text_027, "Yazd", 31.8974, 54.3569),
            city(R.string.iran_location_text_028, "Birjand South Khorasan", 32.8649, 59.2262),
            city(R.string.iran_location_text_029, "Bojnurd North Khorasan", 37.4747, 57.3290),
            city(R.string.iran_location_text_030, "Zahedan Sistan Baluchestan", 29.4963, 60.8629),
            city(R.string.iran_location_text_031, "Arak Markazi", 34.0917, 49.6892)
    };

    private IranOfflineLocations() {}

    static List<Location> search(String query, int limit) {
        String normalized = normalize(query);
        ArrayList<Location> matches = new ArrayList<>();
        if (normalized.isEmpty()) return matches;
        for (Location location : LOCATIONS) {
            if (!location.searchText().contains(normalized)) continue;
            matches.add(location);
            if (matches.size() >= limit) break;
        }
        return matches;
    }

    static String localizedLabel(
            double latitude, double longitude, String fallback) {
        for (Location location : LOCATIONS) {
            if (Math.abs(location.latitude - latitude) < 0.0002
                    && Math.abs(location.longitude - longitude) < 0.0002) {
                return location.label();
            }
        }
        return fallback;
    }

    private static Location city(
            int labelResId, String aliases, double latitude, double longitude) {
        return new Location(labelResId, aliases, latitude, longitude);
    }

    private static String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT)
                .replace('ي', 'ی')
                .replace('ك', 'ک')
                .replace('ة', 'ه')
                .replace("‌", " ");
        return normalized.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}
