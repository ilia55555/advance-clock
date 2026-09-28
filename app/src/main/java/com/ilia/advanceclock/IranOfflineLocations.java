package com.ilia.advanceclock;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class IranOfflineLocations {
    static final class Location {
        final String label;
        final String searchText;
        final double latitude;
        final double longitude;

        Location(String label, String aliases, double latitude, double longitude) {
            this.label = label;
            this.searchText = normalize(label + " " + aliases);
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static final Location[] LOCATIONS = {
            city("تهران، استان تهران، ایران", "Tehran", 35.6892, 51.3890),
            city("کرج، استان البرز، ایران", "Karaj Alborz", 35.8400, 50.9391),
            city("قم، استان قم، ایران", "Qom", 34.6416, 50.8746),
            city("اصفهان، استان اصفهان، ایران", "Isfahan Esfahan", 32.6546, 51.6680),
            city("شیراز، استان فارس، ایران", "Shiraz Fars", 29.5918, 52.5837),
            city("مشهد، خراسان رضوی، ایران", "Mashhad Razavi Khorasan", 36.2605, 59.6168),
            city("تبریز، آذربایجان شرقی، ایران", "Tabriz East Azerbaijan", 38.0800, 46.2919),
            city("ارومیه، آذربایجان غربی، ایران", "Urmia Orumiyeh West Azerbaijan", 37.5527, 45.0761),
            city("اردبیل، استان اردبیل، ایران", "Ardabil", 38.2498, 48.2933),
            city("رشت، استان گیلان، ایران", "Rasht Gilan", 37.2808, 49.5832),
            city("ساری، استان مازندران، ایران", "Sari Mazandaran", 36.5659, 53.0586),
            city("گرگان، استان گلستان، ایران", "Gorgan Golestan", 36.8427, 54.4439),
            city("سمنان، استان سمنان، ایران", "Semnan", 35.5769, 53.3921),
            city("قزوین، استان قزوین، ایران", "Qazvin Ghazvin", 36.2688, 50.0041),
            city("زنجان، استان زنجان، ایران", "Zanjan", 36.6736, 48.4787),
            city("همدان، استان همدان، ایران", "Hamadan Hamedan", 34.7989, 48.5150),
            city("کرمانشاه، استان کرمانشاه، ایران", "Kermanshah", 34.3142, 47.0650),
            city("سنندج، استان کردستان، ایران", "Sanandaj Kurdistan", 35.3219, 46.9862),
            city("ایلام، استان ایلام، ایران", "Ilam", 33.6374, 46.4227),
            city("خرم‌آباد، استان لرستان، ایران", "Khorramabad Khoramabad Lorestan خرم اباد", 33.4878, 48.3558),
            city("اهواز، استان خوزستان، ایران", "Ahvaz Ahwaz Khuzestan", 31.3183, 48.6706),
            city("شهرکرد، چهارمحال و بختیاری، ایران", "Shahrekord Shahr-e Kord", 32.3256, 50.8644),
            city("یاسوج، کهگیلویه و بویراحمد، ایران", "Yasuj", 30.6682, 51.5879),
            city("بوشهر، استان بوشهر، ایران", "Bushehr", 28.9234, 50.8203),
            city("بندرعباس، استان هرمزگان، ایران", "Bandar Abbas Hormozgan بندر عباس", 27.1832, 56.2666),
            city("کرمان، استان کرمان، ایران", "Kerman", 30.2839, 57.0834),
            city("یزد، استان یزد، ایران", "Yazd", 31.8974, 54.3569),
            city("بیرجند، خراسان جنوبی، ایران", "Birjand South Khorasan", 32.8649, 59.2262),
            city("بجنورد، خراسان شمالی، ایران", "Bojnurd North Khorasan", 37.4747, 57.3290),
            city("زاهدان، سیستان و بلوچستان، ایران", "Zahedan Sistan Baluchestan", 29.4963, 60.8629),
            city("اراک، استان مرکزی، ایران", "Arak Markazi", 34.0917, 49.6892)
    };

    private IranOfflineLocations() {}

    static List<Location> search(String query, int limit) {
        String normalized = normalize(query);
        ArrayList<Location> matches = new ArrayList<>();
        if (normalized.isEmpty()) return matches;
        for (Location location : LOCATIONS) {
            if (!location.searchText.contains(normalized)) continue;
            matches.add(location);
            if (matches.size() >= limit) break;
        }
        return matches;
    }

    private static Location city(
            String label, String aliases, double latitude, double longitude) {
        return new Location(label, aliases, latitude, longitude);
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
