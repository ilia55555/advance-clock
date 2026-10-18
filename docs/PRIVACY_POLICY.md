# InfiniTime — Privacy policy

[English](#english) · [فارسی](#persian) · [العربية](#arabic)

Developer / سازنده / المطوّر: **ایلیا بکتاش**

Last updated: **2026-10-18**

<a id="english"></a>

## English

### App and developer

This policy applies to InfiniTime (com.ilia.advanceclock), developed by ایلیا بکتاش. The current app has no sign-in, developer-operated cloud account, advertising SDK or analytics SDK. Core clock, calendar, reminder and time-tool features work offline.

### Information stored on your device

Alarms, notes, drawings, timer/stopwatch presets and history, settings, chosen cities, saved prayer locations, and attachment references are stored on your device. Selected or captured photos may be copied into the app’s private storage. The app does not automatically upload note text, drawings, photos, file contents or your app-shortcut list to the developer.

### Location and online requests

Location is optional and is used for prayer times, Qibla direction and location selection when you use those tools. You may select a location manually instead. The app does not request background-location permission or continuously track your movements. Saved coordinates can remain in settings.

Online city searches send the search text to OpenStreetMap/Nominatim. To determine a time zone, selected or saved coordinates may be sent to TimeAPI (timeapi.io) and, as a fallback, Open-Meteo (api.open-meteo.com). Repairing an older saved prayer location can also perform this lookup on app startup if offline resolution fails. Android’s Geocoder may send place names or coordinates to the location service configured by your device. These requests can reveal precise location when exact coordinates are used.

### Third parties and sharing

Network requests use HTTPS. The receiving service can see your IP address, the search or coordinates included in the request, and ordinary request metadata. Those providers control their own processing and retention; their policy links are listed under Sources and licenses. The developer does not operate a server that receives these requests and does not sell your local app content.

Opening an attached app, link or file, using a camera app, selecting cloud-hosted files, or visiting a support/policy website involves the app or provider you choose and its own terms. An external file viewer receives access to the file you choose to open.

### Photos, files and app shortcuts

Android’s system picker grants access to the photos and files you select. Taking a photo uses your installed camera app with a temporary grant to save the captured image. Captured originals and cropped images can be kept locally for reframing. To offer app shortcuts, the app reads names, icons and identifiers of launchable apps and stores only your chosen shortcuts locally; it does not upload an installed-app inventory.

### Storage and security

App-managed files and preferences use Android’s app-private storage. Alarm and note records use encryption backed by Android Keystore; photos, caches and ordinary preferences are not all separately encrypted. Android cloud backup and app-data transfer are disabled in this build. Keep your device protected. The app cannot control the security of your device, external file providers or services you open. Notifications, widgets and lock-screen alerts may show your chosen content. Use notification privacy and alert settings to control what others can see on your device.

### Retention and deletion

Local information is retained until you remove it or clear app storage. Delete alarms and notes using their delete controls. Removing an attachment or entry may leave copied photos, drafts or search caches in app storage. To remove all app-managed data, use Android Settings → Apps → InfiniTime → Storage → Clear storage, or uninstall the app. This also removes saved reminders and settings. Original files in your gallery or cloud provider and data held by external services are not deleted by clearing this app. Manage those with the relevant provider.

You can revoke location, notification and file access in Android settings, turn off full-screen alerts or exact alarms, and avoid optional online searches. Some functions will then be unavailable or delayed. No account-deletion request is needed because the app does not create user accounts.

### Questions and policy changes

Privacy questions and app issues can be submitted through the project’s support page. GitHub issues are public: do not include private notes, precise coordinates, personal files or account details. There is no private support email configured in this version. The date above identifies this policy version. Future material data-handling changes must be reflected here and require any permissions or consent applicable to the new feature.

[Project support / پشتیبانی / الدعم](https://github.com/ilia55555/advance-clock/issues)

- [OpenStreetMap privacy](https://osmfoundation.org/wiki/Privacy_Policy)
- [OpenStreetMap attribution / ODbL](https://www.openstreetmap.org/copyright)
- [TimeAPI privacy](https://www.timeapi.io/Home/Privacy)
- [Open-Meteo terms and privacy](https://open-meteo.com/en/terms)

<a id="persian"></a>

## فارسی

### برنامه و سازنده

این سیاست مربوط به InfiniTime با شناسهٔ com.ilia.advanceclock و سازندگی ایلیا بکتاش است. نسخهٔ فعلی ورود کاربر، حساب ابری نزد سازنده، کیت تبلیغات یا ابزار تحلیل رفتار کاربر ندارد. قابلیت‌های اصلی ساعت، تقویم، یادآوری و ابزارهای زمان به‌صورت آفلاین کار می‌کنند.

### اطلاعات ذخیره‌شده روی دستگاه

هشدارها، یادداشت‌ها، نقاشی‌ها، تنظیمات و سوابق تایمر و کرنومتر، ترجیحات، شهرهای انتخابی، موقعیت‌های ذخیره‌شدهٔ اوقات شرعی و نشانی پیوست‌ها روی دستگاه شما نگهداری می‌شوند. عکس‌های انتخابی یا گرفته‌شده ممکن است در فضای خصوصی برنامه کپی شوند. برنامه متن یادداشت، نقاشی، عکس، محتوای فایل یا فهرست میان‌بر برنامه‌های شما را به‌صورت خودکار برای سازنده ارسال نمی‌کند.

### موقعیت و درخواست‌های آنلاین

دسترسی به موقعیت اختیاری است و هنگام استفاده از ابزارهای مربوط، برای اوقات شرعی، جهت قبله و انتخاب مکان به کار می‌رود. می‌توانید مکان را دستی انتخاب کنید. برنامه مجوز موقعیت پس‌زمینه درخواست نمی‌کند و حرکت شما را به‌طور پیوسته ردیابی نمی‌کند. مختصات انتخابی می‌تواند در تنظیمات باقی بماند.

جست‌وجوی آنلاین شهر، عبارت جست‌وجو را به OpenStreetMap/Nominatim می‌فرستد. برای تشخیص منطقهٔ زمانی، مختصات انتخابی یا ذخیره‌شده ممکن است به TimeAPI در timeapi.io و در صورت نیاز به Open-Meteo در api.open-meteo.com ارسال شود. اصلاح منطقهٔ زمانیِ یک موقعیت قدیمی نیز ممکن است هنگام شروع برنامه، در صورت ناموفق بودن تشخیص آفلاین، همین درخواست را انجام دهد. سرویس Geocoder اندروید ممکن است نام مکان یا مختصات را برای ارائه‌دهندهٔ مکان دستگاه بفرستد. اگر مختصات دقیق استفاده شود، این درخواست‌ها می‌توانند موقعیت دقیق را آشکار کنند.

### سرویس‌های بیرونی و اشتراک‌گذاری

درخواست‌های اینترنتی با HTTPS ارسال می‌شوند. سرویس دریافت‌کننده می‌تواند نشانی IP، عبارت جست‌وجو یا مختصات موجود در درخواست و اطلاعات معمول درخواست را ببیند. پردازش و مدت نگهداری داده نزد این سرویس‌ها تابع سیاست خودشان است؛ پیوند سیاست‌هایشان در بخش منابع و مجوزها آمده است. سازنده سروری برای دریافت این درخواست‌ها ندارد و محتوای محلی شما را نمی‌فروشد.

باز کردن برنامه، لینک یا فایل پیوست‌شده، استفاده از برنامهٔ دوربین، انتخاب فایل ابری یا رفتن به سایت پشتیبانی و سیاست‌ها، شامل برنامه یا ارائه‌دهندهٔ انتخابی شما و شرایط خودش است. نمایشگر فایل بیرونی، برای فایلی که خودتان باز می‌کنید اجازهٔ دسترسی می‌گیرد.

### عکس، فایل و میان‌بر برنامه‌ها

انتخاب‌گر اندروید اجازهٔ دسترسی به عکس‌ها و فایل‌هایی را می‌دهد که انتخاب می‌کنید. گرفتن عکس از برنامهٔ دوربین نصب‌شده، با اجازهٔ موقت برای ذخیرهٔ عکس، استفاده می‌کند. اصل عکس گرفته‌شده و نسخهٔ برش‌خورده ممکن است برای تنظیم دوبارهٔ قاب، محلی نگهداری شوند. برای ساخت میان‌بر، نام و آیکون و شناسهٔ برنامه‌های قابل اجرا خوانده می‌شود و فقط میان‌برهای انتخابی شما محلی ذخیره می‌شوند؛ فهرست برنامه‌های نصب‌شده ارسال نمی‌شود.

### نگهداری و امنیت

فایل‌ها و تنظیمات مدیریت‌شدهٔ برنامه در فضای خصوصی اندروید نگهداری می‌شوند. داده‌های هشدار و یادداشت با رمزنگاری متکی بر Android Keystore ذخیره می‌شوند؛ همهٔ عکس‌ها، حافظه‌های موقت و تنظیمات عادی جداگانه رمزنگاری نمی‌شوند. پشتیبان‌گیری ابری اندروید و انتقال دادهٔ برنامه در این نسخه غیرفعال است. از دستگاهتان محافظت کنید. برنامه امنیت دستگاه، ارائه‌دهندگان فایل یا سرویس‌هایی را که باز می‌کنید کنترل نمی‌کند. اعلان‌ها، ویجت‌ها و هشدار صفحهٔ قفل ممکن است محتوای انتخابی شما را نشان دهند. با تنظیم حریم اعلان و گزینه‌های هشدار، آنچه دیگران روی دستگاه می‌بینند را مدیریت کنید.

### مدت نگهداری و حذف اطلاعات

اطلاعات محلی تا زمان حذف آن‌ها یا پاک کردن فضای برنامه باقی می‌ماند. هشدار و یادداشت را با گزینهٔ حذف همان مورد پاک کنید. حذف یک مورد یا پیوست ممکن است عکس‌های کپی‌شده، پیش‌نویس‌ها یا حافظهٔ جست‌وجو را در فضای برنامه باقی بگذارد. برای حذف همهٔ داده‌های مدیریت‌شدهٔ برنامه، در تنظیمات اندروید به برنامه‌ها ← InfiniTime ← فضای ذخیره‌سازی ← پاک کردن داده‌ها بروید یا برنامه را حذف نصب کنید. با این کار یادآوری‌ها و تنظیمات ذخیره‌شده نیز حذف می‌شوند. فایل اصلی گالری یا ارائه‌دهندهٔ ابری و داده‌های سرویس‌های بیرونی با پاک کردن این برنامه حذف نمی‌شوند؛ آن‌ها را نزد همان ارائه‌دهنده مدیریت کنید.

می‌توانید دسترسی موقعیت، اعلان و فایل را در تنظیمات اندروید لغو کنید، هشدار تمام‌صفحه یا هشدار دقیق را خاموش کنید و از جست‌وجوی آنلاین اختیاری استفاده نکنید. در این صورت بعضی قابلیت‌ها در دسترس نیستند یا با تأخیر اجرا می‌شوند. چون برنامه حساب کاربری ایجاد نمی‌کند، درخواست حذف حساب لازم نیست.

### پرسش‌ها و تغییر سیاست

پرسش‌های حریم خصوصی و مشکلات برنامه را می‌توانید از صفحهٔ پشتیبانی پروژه مطرح کنید. گزارش‌های گیت‌هاب عمومی‌اند؛ یادداشت خصوصی، مختصات دقیق، فایل شخصی یا اطلاعات حساب را در آن‌ها قرار ندهید. در این نسخه ایمیل خصوصی پشتیبانی تنظیم نشده است. تاریخ بالای صفحه نسخهٔ این سیاست را مشخص می‌کند. تغییر مهم در پردازش داده‌ها در نسخه‌های آینده باید در این صفحه منعکس شود و هر مجوز یا رضایت لازم برای قابلیت جدید دریافت شود.

[Project support / پشتیبانی / الدعم](https://github.com/ilia55555/advance-clock/issues)

- [OpenStreetMap privacy](https://osmfoundation.org/wiki/Privacy_Policy)
- [OpenStreetMap attribution / ODbL](https://www.openstreetmap.org/copyright)
- [TimeAPI privacy](https://www.timeapi.io/Home/Privacy)
- [Open-Meteo terms and privacy](https://open-meteo.com/en/terms)

<a id="arabic"></a>

## العربية

### التطبيق والمطوّر

تنطبق هذه السياسة على InfiniTime بالمعرّف com.ilia.advanceclock، من تطوير ایلیا بکتاش. لا يتضمن الإصدار الحالي تسجيل دخول أو حسابًا سحابيًا لدى المطوّر أو حزمة إعلانات أو أدوات لتحليل سلوك المستخدم. تعمل وظائف الساعة والتقويم والتذكيرات وأدوات الوقت الأساسية دون إنترنت.

### المعلومات المخزنة على جهازك

تُحفظ المنبّهات والملاحظات والرسومات وإعدادات وسجلات المؤقّت وساعة الإيقاف والتفضيلات والمدن المختارة ومواقع الصلاة المحفوظة ومراجع المرفقات على جهازك. قد تُنسخ الصور المختارة أو الملتقطة إلى مساحة التطبيق الخاصة. لا يرفع التطبيق تلقائيًا نصوص ملاحظاتك أو رسوماتك أو صورك أو محتوى ملفاتك أو قائمة اختصارات تطبيقاتك إلى المطوّر.

### الموقع والطلبات عبر الإنترنت

الوصول إلى الموقع اختياري ويُستخدم لمواقيت الصلاة واتجاه القبلة واختيار المكان عند استخدام الأدوات ذات الصلة. يمكنك اختيار الموقع يدويًا. لا يطلب التطبيق إذن الموقع في الخلفية ولا يتتبع تحركاتك باستمرار. قد تبقى الإحداثيات المحفوظة في الإعدادات.

يرسل البحث عبر الإنترنت عن المدن عبارة البحث إلى OpenStreetMap/Nominatim. لتحديد المنطقة الزمنية، قد تُرسل الإحداثيات المختارة أو المحفوظة إلى TimeAPI على timeapi.io، ثم إلى Open-Meteo على api.open-meteo.com عند الحاجة. قد يجري إصلاح منطقة زمنية لموقع قديم هذا الطلب عند بدء التطبيق إذا تعذّر الحل دون اتصال. قد يرسل Geocoder في أندرويد أسماء الأماكن أو الإحداثيات إلى مزوّد الموقع في جهازك. يمكن أن تكشف هذه الطلبات الموقع الدقيق عند استخدام إحداثيات دقيقة.

### الجهات الخارجية والمشاركة

تُرسل طلبات الشبكة عبر HTTPS. يمكن للخدمة المستقبلة رؤية عنوان IP وعبارة البحث أو الإحداثيات المرسلة وبيانات الطلب المعتادة. تتحكم هذه الجهات بمعالجتها وفترة الاحتفاظ بها وفق سياساتها؛ روابطها في قسم المصادر والتراخيص. لا يشغّل المطوّر خادمًا يستقبل هذه الطلبات ولا يبيع محتواك المحلي.

فتح تطبيق أو رابط أو ملف مرفق، واستخدام تطبيق الكاميرا، واختيار ملفات سحابية، وزيارة مواقع الدعم والسياسات، يتضمن التطبيق أو المزوّد الذي تختاره وشروطه الخاصة. يحصل عارض الملفات الخارجي على إذن الوصول إلى الملف الذي تختار فتحه.

### الصور والملفات واختصارات التطبيقات

يمنح منتقي أندرويد الوصول إلى الصور والملفات التي تختارها. يُستخدم تطبيق الكاميرا المثبّت لديك لالتقاط الصور، مع إذن مؤقت لحفظ الصورة. قد تُحفظ الصور الأصلية والنسخ المقصوصة محليًا لإعادة ضبط الإطار. لإنشاء اختصار، تُقرأ أسماء وأيقونات ومعرّفات التطبيقات القابلة للتشغيل، وتُحفظ اختصاراتك المختارة محليًا فقط؛ لا تُرفع قائمة التطبيقات المثبّتة.

### التخزين والأمان

تُحفظ الملفات والتفضيلات التي يديرها التطبيق في مساحة أندرويد الخاصة بالتطبيق. تستخدم سجلات المنبّهات والملاحظات تشفيرًا يعتمد على Android Keystore؛ لا تُشفّر جميع الصور وذاكرات التخزين المؤقت والتفضيلات العادية بشكل منفصل. النسخ الاحتياطي السحابي ونقل بيانات التطبيق في أندرويد معطّلان في هذا الإصدار. احمِ جهازك؛ لا يتحكم التطبيق بأمان جهازك أو مزوّدي الملفات أو الخدمات التي تفتحها. قد تُظهر الإشعارات والودجات وتنبيهات شاشة القفل المحتوى الذي تختاره. استخدم إعدادات خصوصية الإشعارات والتنبيهات للتحكم بما يراه الآخرون على جهازك.

### الاحتفاظ بالمعلومات وحذفها

تبقى المعلومات المحلية حتى تحذفها أو تمسح مساحة التطبيق. احذف المنبّهات والملاحظات من عناصر التحكم الخاصة بها. قد تبقى صور منسوخة ومسودات وذاكرة البحث بعد إزالة عنصر أو مرفق. لإزالة جميع البيانات التي يديرها التطبيق، انتقل إلى إعدادات أندرويد ← التطبيقات ← InfiniTime ← التخزين ← مسح مساحة التخزين، أو أزل التطبيق. تُحذف أيضًا التذكيرات والإعدادات المحفوظة. لا تُحذف الملفات الأصلية في المعرض أو السحابة ولا بيانات الخدمات الخارجية؛ تُدار هذه لدى مزوّدها.

يمكنك سحب أذونات الموقع والإشعارات والملفات من إعدادات أندرويد، وإيقاف تنبيهات ملء الشاشة أو المنبّهات الدقيقة وتجنّب البحث الاختياري عبر الإنترنت. قد تتعطل بعض الوظائف أو تتأخر حينئذٍ. لا يلزم طلب حذف حساب لأن التطبيق لا ينشئ حسابات مستخدمين.

### الأسئلة وتغييرات السياسة

يمكن تقديم أسئلة الخصوصية ومشاكل التطبيق عبر صفحة دعم المشروع. بلاغات GitHub عامة؛ لا تنشر ملاحظات خاصة أو إحداثيات دقيقة أو ملفات شخصية أو تفاصيل حسابات. لم يُضبط بريد دعم خاص في هذا الإصدار. يحدد التاريخ أعلاه نسخة السياسة. يجب أن تنعكس تغييرات معالجة البيانات المهمة هنا وأن تُطلب الأذونات أو الموافقة اللازمة للميزات الجديدة.

[Project support / پشتیبانی / الدعم](https://github.com/ilia55555/advance-clock/issues)

- [OpenStreetMap privacy](https://osmfoundation.org/wiki/Privacy_Policy)
- [OpenStreetMap attribution / ODbL](https://www.openstreetmap.org/copyright)
- [TimeAPI privacy](https://www.timeapi.io/Home/Privacy)
- [Open-Meteo terms and privacy](https://open-meteo.com/en/terms)

