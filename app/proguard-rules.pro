# MainNoteTabEnhancer accesses only these MainActivity members by string name.
# Keep their names and implementations; the rest of the application can shrink,
# optimize and obfuscate. Do not add a package-wide -keep or blanket -dontwarn.
-keepclassmembers class com.ilia.advanceclock.MainActivity {
    java.util.Calendar quickNoteDue;
    int noteRecurrenceMode;
    int noteIntervalDays;
    java.lang.String noteCustomDates;
    void updateQuickNoteLabels();
    void renderNoForget();
}

# Retain useful retrace information without exposing original source filenames.
# mapping.txt stays in the ignored build directory and must be stored privately.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
