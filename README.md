# Advance

یک اپ Native اندروید با دو تب مستقل:

## ساعت پیشرفته

- آلارم عادی و آلارم تاریخ‌دار با انتخاب روز، ساعت و دقیقه.
- تکرار روزانه، هفتگی، ماهانه و سالانه.
- زمان‌بندی با AlarmManager.
- صدا + ویبره + صفحه Full-screen هنگام رسیدن زمان.
- دکمه بزرگ «قطع آلارم» داخل صفحه زنگ و Action «قطع» روی Notification.
- بازیابی آلارم‌ها بعد از Boot، تغییر ساعت، timezone و آپدیت برنامه.
- Widget ریسایزشونده؛ با افزایش ارتفاع، آلارم‌های بیشتری را نمایش می‌دهد.

## NoForget

- یادداشت متنی و توضیح آزاد.
- دست‌نویس/نقاشی با انگشت.
- درجه اهمیت کم، عادی و زیاد.
- سررسید اختیاری و Notification یادآوری.
- رنگ‌بندی بر اساس اهمیت و نزدیک‌شدن/گذشتن سررسید.
- Widget ریسایزشونده با تعداد ردیف متناسب با اندازه.
- دکمه + روی Widget برای ثبت سریع بدون باز کردن فرم کامل.

## مجوزها

Manifest فقط مجوزهای موردنیاز این قابلیت‌ها را دارد:

- POST_NOTIFICATIONS — درخواست Runtime در Android 13+.
- SCHEDULE_EXACT_ALARM — هدایت کاربر به Special Access در Android 12+.
- USE_FULL_SCREEN_INTENT — بررسی و هدایت کاربر به Special Access در Android 14+.
- RECEIVE_BOOT_COMPLETED — بازیابی زمان‌بندی‌ها پس از روشن شدن گوشی.
- VIBRATE و WAKE_LOCK — ویبره و بیدار نگه داشتن CPU هنگام زنگ.
- FOREGROUND_SERVICE و FOREGROUND_SERVICE_MEDIA_PLAYBACK — سرویس صدای آلارم.

در اجرای اول، Flow مجوزها به ترتیب اعلان‌ها → آلارم دقیق → صفحه Full-screen اجرا می‌شود. وضعیت هر سه مورد همیشه در صفحه اصلی دیده می‌شود و دکمه «بررسی و دریافت مجوزهای لازم» امکان اجرای دوباره Flow را می‌دهد.

## Build

- Android SDK Platform: Android 16 / API 36.1
- compileSdk: 36.1 (API 36 + minor API level 1)
- targetSdk: 36
- minSdk: 26
- Android SDK Platform-Tools: 36.0.2
- Android Gradle Plugin: 8.13.2
- Gradle: 8.13
- Java: 17

> Platform-Tools بخشی از نصب Android SDK است و داخل Gradle پین نمی‌شود؛ برای این پروژه نسخه 36.0.2 در SDK Manager نصب و استفاده شود.
