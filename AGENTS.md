# دستور قطعی مالک پروژه: آیکون اپ ممنوع‌التغییر است

آیکون اپ و خروجی Android Studio Image Asset فقط توسط مالک پروژه مدیریت می‌شوند.
هیچ ایجنتی حق تغییر، حذف، جایگزینی، بازسازی، تغییر نام یا جابه‌جایی تصاویر آیکون،
فایل‌های launcher، لایه‌های foreground/background، منابع mipmap مرتبط، رنگ‌ها و
تنظیمات Image Asset یا اتصال‌های android:icon و android:roundIcon را ندارد.
هیچ اسکریپت یا وظیفهٔ Gradle برای تولید یا تحمیل لوگوی دیگر اضافه نکنید.
این ممنوعیت شامل رفع خطای بیلد، پاک‌سازی منابع و تغییر طراحی نیز هست.
اگر مشکل مربوط به آیکون بود، فقط گزارش دهید و به فایل‌ها دست نزنید.
صرفاً دستور صریح و جدید مالک برای تغییر همین بخش می‌تواند مجوز تغییر باشد؛
درخواست‌های کلی رفع باگ، بیلد، مرتب‌سازی یا بررسی پروژه مجوز نیستند.

# Owner-managed launcher icon — mandatory

Do not edit, delete, replace, regenerate, rename, move, or override the app icon,
its Image Asset output, associated launcher/mipmap resources, foreground/background
layers or colors, or Manifest icon/roundIcon wiring. Do not add icon generation tasks.
This restriction also applies during build fixes, cleanup, and design changes.
Report icon problems without modifying them. Only a new, explicit owner instruction
specifically authorizing an icon change can override this rule.
