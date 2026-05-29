package com.example.cars.util;

import androidx.annotation.Nullable;

import com.example.cars.model.Branch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BranchLabelMap {

    private BranchLabelMap() {}

    /** מזהה סניף → שם תצוגה (שם הסניף או כתובת). */
    public static Map<String, String> from(@Nullable List<Branch> branches) {
        Map<String, String> m = new HashMap<>();
        if (branches == null) {
            return m;
        }
        for (Branch b : branches) {
            if (b == null || b.getId() == null || b.getId().isEmpty()) {
                continue;
            }
            String label = displayLabel(b);
            if (!label.isEmpty()) {
                m.put(b.getId(), label);
            }
        }
        return m;
    }

    private static String displayLabel(Branch b) {
        String name = b.getBranchName();
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        String addr = b.getAddress();
        if (addr != null && !addr.trim().isEmpty()) {
            return addr.trim();
        }
        return "";
    }
}
