# قانون مالک پروژه: لوگوی اپ فقط محلی است (GitHub هیچ تصویری از لوگو ندارد)

- لوگوی لانچر را فقط مالک، به‌صورت دستی در Android Studio با `res > New > Image Asset`
  و نام `ic_launcher` ایجاد یا جایگزین می‌کند.
- تمام خروجی‌های Image Asset (از جمله mipmap، foreground/background، نسخه گرد،
  تصاویر Google Play و تصویر اصلی لوگو) **فقط روی دستگاه مالک** باقی می‌مانند.
  هرگز آن‌ها را به Git، Commit، Push، GitHub یا خروجی کدنویسی ایجنت‌ها اضافه نکنید.
- هیچ ایجنتی مجاز نیست تصاویر لانچر را بسازد، ویرایش کند، حذف کند، بازسازی کند،
  نامشان را تغییر دهد یا فایل‌های محلی آیکون را جابه‌جا کند. در صورت مشاهده خطا
  فقط گزارش دهد؛ تغییر منوط به درخواست جدید و صریح مالک است.
- `.gitignore` باید خروجی Image Asset را نادیده بگیرد؛ آن را برای آپلود تصاویر
  آیکون دور نزنید. فایل‌های غیرمرتبط مانند تصاویر ویجت و قبله‌نما مشمول این قانون نیستند.
- ارجاع آیکون در Manifest از طریق placeholderهای Gradle انجام می‌شود: اگر آیکون
  محلی وجود داشته باشد همان استفاده می‌شود، و در کلون تمیز GitHub آیکون پیش‌فرض
  اندروید استفاده می‌شود تا بیلد بدون تصویر اختصاصی ممکن باشد.
- هیچ کار تولید آیکون یا جایگزینی لوگوی کاربر را به Gradle اضافه نکنید.

# Owner-managed launcher icon — local only, never in GitHub

Only the project owner manages app launcher artwork manually via Android Studio
Image Asset with the resource name `ic_launcher`. Do not commit, push, recreate,
replace, edit, rename, move, delete, or auto-generate any launcher assets,
mipmap resources, foreground/background art, round icons, or Play Store artwork.
Keep all generated icon resources local and ignored by Git. Never bypass the
icon-related .gitignore entries. Unrelated application images are unaffected.

Gradle/Manifest may select the owner's existing, local `@mipmap/ic_launcher`
and `@mipmap/ic_launcher_round` resources when present and use a built-in Android
fallback on clean checkouts. No app icon imagery is stored on GitHub. If an icon
problem arises, report it; only a new explicit instruction from the owner can
authorize changes to locally managed icon resources.
