# Offline calendar dates and recurring occasions

The application calculates dates and occasions without a network request. Persian
and lunar date arithmetic is bundled with the application, so Android's installed
ICU calendar tables cannot change their results. ICU still supplies civil time-zone
and week arithmetic. Gregorian date validation uses `java.time.LocalDate`.

## Supported interval

The date picker and month navigation allow complete calendar years within
**1900-01-01 through 2170-12-31 Gregorian**. This covers more than 100 future years
from the 2026 release. The underlying lunar table covers **1300–1600 AH**; dates
outside that table are rejected rather than silently switched to a tabular calendar.
The picker clamps an old day to the new month's length before conversion, so
selecting June from 31 May selects 30 June rather than 1 July.

This is a guarantee of a defined calculation and supported range, **not a guarantee
that a predicted lunar date equals every country's future moon-sighting announcement**.
Future announcements are not available in advance. The application explicitly
shows its lunar reference and the end of the Iranian reference coverage in Settings.
The occasion rules describe recurring observances; future statutory holidays may
also change independently of those rules.

## Lunar references

* **Iran (default):** historical Iranian month starts through 1447/10 from the
  CC0 `roozbehp/qamari` compilation, followed by month starts extracted from the
  University of Tehran Calendar Centre's published 1405 calendar. Iranian reference
  coverage ends on **2027-03-20 inclusive (29 Esfand 1405)**. The compilation includes
  corrections to some originally published historical dates; it is a reference
  compilation, not an assertion that every historical entry is independently verified.
* **Saudi Arabia / Umm al-Qura:** pinned Unicode ICU month starts for 1300–1600 AH.
* After the last Iranian reference month, Iran mode uses the bundled Umm al-Qura
  calculation for future month starts. The connection has a valid 29-day month;
  neither calendar skips a date or creates an invalid 28/31-day lunar month.
  `hasIranReference` and `datasetNotice` distinguish reference and calculated dates.

For example, 2026-10-08 is **1448/04/26** in Iran mode and **1448/04/27** in
Umm al-Qura mode. That reference difference must not be fixed with a permanent
one-day offset, since different months do not always have the same difference.
Changing the selected reference updates dates and lunar occasions together in
the application, widgets, and persistent date notification.

Lunar dates here are civil date labels, changing at local midnight. Evening
observance and the religious sunset boundary are not separate date-picker days.
Country-specific moon-sighting announcements must be incorporated in a future
version's bundled reference data if exact official observance is required.

## Occasions

Religious occasions are recurring **lunar month/day rules**, including births and
martyrdoms of the Imams, Fatima, the Prophet, Ghadir, Ashura, Arbaeen, and the shared
Eids. Both Iranian and Arabic collections obtain them from the same selected lunar
calendar; enabling both collections does not duplicate shared titles. These rules
are not limited to 2026. Fixed Iranian civil occasions remain Persian dates, and
international occasions remain Gregorian dates.

Imam Reza's martyrdom is the **last day of Safar**, and Imam Jawad's martyrdom is
the **last day of Dhu al-Qadah**. Both work for 29- and 30-day months. Sunni Mawlid
on 12 Rabi al-Awwal and the Shia Prophet/Imam Sadiq birth observance on 17 Rabi
al-Awwal remain separate. Alternative traditions are identified in event titles.
Displaying an occasion in both collections does not make it a statutory holiday
in every Arab country; source-specific holiday flags are retained.

## Persian leap years

The bundled Persian algorithm includes ICU's leap-year corrections, including
**1502/1503 AP (2123/2124)**. Using only the uncorrected 33-year cycle would be
incorrect inside the requested future century on devices with older ICU versions.

## Sources and licenses

* Iranian month starts: [roozbehp/qamari consolidated.txt](https://github.com/roozbehp/qamari/blob/575d275ef169c0a18012506d473ba1008a1c10d0/consolidated.txt),
  commit `575d275ef169c0a18012506d473ba1008a1c10d0`, CC0 1.0, included in
  `third_party/qamari-CC0.txt`. Month starts from 1300/01 onward are bundled.
* Iranian 1405 reference and religious dates:
  [University of Tehran Calendar Centre, Calendar-1405.pdf](https://calendar.ut.ac.ir/documents/2139738/7092644/Calendar-1405.pdf/64228cbb-f4de-dc32-4d2b-57db3c8e322f?t=1761972997587).
  Month starts added: 1447/11=2026-04-19, 1447/12=2026-05-18,
  1448/01=2026-06-16, /02=2026-07-16, /03=2026-08-14, /04=2026-09-13,
  /05=2026-10-12, /06=2026-11-11, /07=2026-12-11, /08=2027-01-10,
  /09=2027-02-08, /10=2027-03-10.
* Umm al-Qura data:
  [Unicode ICU IslamicCalendar.java](https://github.com/unicode-org/icu/blob/9b16ae8169886cfbe9f4d2a25b87718024071cca/icu4j/main/core/src/main/java/com/ibm/icu/util/IslamicCalendar.java),
  file revision `9b16ae8169886cfbe9f4d2a25b87718024071cca`.
  Expanded from 301 year masks starting at Julian day 2408762; all month starts
  were checked against ICU's independent year-start estimate and corrections.
* Persian algorithm and corrections:
  [Unicode ICU PersianCalendar.java](https://github.com/unicode-org/icu/blob/9407fad66d51a399941556d0fb6161fbdf0a4707/icu4j/main/core/src/main/java/com/ibm/icu/util/PersianCalendar.java),
  file revision `9407fad66d51a399941556d0fb6161fbdf0a4707`.
  ICU notices and Unicode License v3 are included in `third_party/unicode-LICENSE.txt`.

## Automated validation

Run `gradle :app:testDebugUnitTest :app:assembleDebug`. GitHub Actions runs both
tasks on Linux and Windows. ICU4J and JUnit are **test-only dependencies** and are
not packaged with the application.

Tests cover:

* Every day from 1900 through 2170, round-tripping all three calendars under both
  lunar references, including month and leap-year boundaries.
* All Persian and Umm al-Qura dates in that interval against ICU4J 78.1.
* Published Iranian 1405 holiday/date fixtures, including the reported one-day
  difference, and recurring lunar occasions over 151 lunar years.
* The production calendar adapter's date/time conversion and month lengths on
  every day from 2026 through 2129, and local dates in UTC, Tehran, Riyadh, and
  New York, including daylight-saving boundaries.
* Strict invalid-date rejection, supported picker year bounds, both lengths of
  end-of-month religious occasions, and the Persian 1502/1503 correction.

For adapter tests, Gradle generates a copy of the production source replacing
only `android.icu.util` imports with `com.ibm.icu.util` and the class name. This
exercises its actual logic on JVM ICU rather than calling non-executable Android
SDK stub methods. Real-device UI checks remain in `TEST_CHECKLIST.md`.
