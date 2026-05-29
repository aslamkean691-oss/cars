package com.example.cars.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * סוגי רכב ותתי־סוגים נפוצים בישראל (לבחירה מהירה).
 */
public final class VehicleCatalog {

    public static final List<String> MAIN_TYPES;

    private static final Map<String, List<String>> SUB_BY_TYPE = new LinkedHashMap<>();

    static {
        MAIN_TYPES = Collections.unmodifiableList(Arrays.asList(
                "פרטי",
                "משפחתי",
                "SUV",
                "מסחרי",
                "קטנוע",
                "ספורט / קופה",
                "יוקרה",
                "אחר"));

        putSubs("פרטי", "סדאן", "האצ'בק", "סטיישן", "קומפקטי", "אחר");
        putSubs("משפחתי", "מיניוואן", "חמש מושבים", "שבעה מושבים", "אחר");
        putSubs("SUV", "קומפקטי", "בינוני", "גדול / 7 מושבים", "פיקאפ", "אחר");
        putSubs("מסחרי", "טנדר", "מסחרי קל", "מסחרי כבד", "אחר");
        putSubs("קטנוע", "קטנוע", "אופנוע", "אחר");
        putSubs("ספורט / קופה", "קופה", "קבריולה", "אחר");
        putSubs("יוקרה", "סדאן", "SUV", "קופה", "אחר");
        putSubs("אחר", "אחר");
    }

    private static void putSubs(String type, String... subs) {
        SUB_BY_TYPE.put(type, Collections.unmodifiableList(Arrays.asList(subs)));
    }

    public static List<String> subTypesFor(String mainType) {
        if (mainType == null) {
            return Collections.singletonList("אחר");
        }
        List<String> list = SUB_BY_TYPE.get(mainType.trim());
        return list != null ? new ArrayList<>(list) : Collections.singletonList("אחר");
    }

    private VehicleCatalog() {}
}
