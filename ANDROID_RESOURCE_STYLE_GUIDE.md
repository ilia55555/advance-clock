# راهنمای پایدار منابع Style اندروید

## قانون قطعی انیمیشن‌ها

- انیمیشن Activity و صفحه تنظیمات فقط با فایل‌های `res/anim` و ارجاع مستقیم `R.anim` اجرا می‌شود.
- برای انیمیشن‌ها در `themes.xml` هیچ `style` سفارشی و هیچ `windowAnimationStyle` تعریف نمی‌شود.
- نام‌هایی با الگوی `Animation.AdvanceClock...` ممنوع‌اند؛ نقطه در نام Style باعث ایجاد والد ضمنی مانند `Animation.AdvanceClock` می‌شود و در صورت نبود آن AAPT با خطای `resource style/Animation.AdvanceClock not found` متوقف می‌شود.
- انیمیشن ورود و خروج Activity باید فقط از helperهای `AppSettings.playFullscreenEnter` و `AppSettings.playFullscreenExit` استفاده کند.
- انیمیشن View یا Dialog باید مستقیماً با `View.animate()` یا منابع `R.anim` انجام شود و نباید Style انیمیشن تازه‌ای به `themes.xml` اضافه کند.

## قانون ویرایش `themes.xml`

- هر Style باید فقط یک بار در کل پوشه‌های `res/values*` تعریف شود.
- Styleهای نقطه‌دار فقط زمانی مجازند که `parent` معتبر و صریح داشته باشند؛ مانند Themeهای برنامه.
- قبل از commit، همه ارجاع‌های `windowAnimationStyle` و نام‌های `Animation.AdvanceClock` باید جست‌وجو شوند و نتیجه‌ای نداشته باشند.

## بررسی الزامی قبل از تحویل

```bash
! rg -n 'windowAnimationStyle|Animation\.AdvanceClock' app/src/main/res
python3 - <<'PY'
from collections import Counter
from pathlib import Path
import xml.etree.ElementTree as ET

names = []
for path in Path('app/src/main/res').glob('values*/*.xml'):
    for style in ET.parse(path).getroot().findall('style'):
        if style.get('name'):
            names.append(style.get('name'))

duplicates = {name: count for name, count in Counter(names).items() if count > 1}
assert not duplicates, duplicates
print('No duplicate styles')
PY
```

پس از تغییر یا حذف Style در محیطی که قبلاً خطای merge داشته است، یک بار `gradle clean assembleDebug` اجرا شود تا خروجی incremental قدیمی Gradle نیز پاک شود.
