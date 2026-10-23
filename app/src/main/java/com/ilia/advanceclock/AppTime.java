package com.ilia.advanceclock;

import java.util.*;

/** App-local reference zone. Never changes Android's or Java's system time zone. */
final class AppTime {
    private static final Set<String> ZONES=new HashSet<>(Arrays.asList(TimeZone.getAvailableIDs()));
    private static volatile String reference="";
    static boolean valid(String id){return id!=null && ZONES.contains(id);}
    static void configure(String id){reference=valid(id)?id:"";}
    static String referenceId(){return reference;}
    static TimeZone zone(){return reference.isEmpty()?TimeZone.getDefault():TimeZone.getTimeZone(reference);}
}
