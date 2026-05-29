package com.example.cars.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cars.model.CarListing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * סינון ומיון רשימת רכבים בצד הלקוח (מקומי, אחרי טעינה מהשרת).
 */
public final class CarListingFilterSort {

    private CarListingFilterSort() {}

    public enum SortKind implements Serializable {
        DEFAULT,
        SALE_PRICE_ASC,
        SALE_PRICE_DESC,
        CATALOG_PRICE_ASC,
        CATALOG_PRICE_DESC,
        YEAR_DESC,
        YEAR_ASC,
        KM_ASC,
        KM_DESC,
        BRAND_ASC,
        MODEL_ASC,
        PLATE_ASC
    }

    public static final class FilterState implements Serializable, Cloneable {
        private static final long serialVersionUID = 1L;

        @NonNull public SortKind sortKind = SortKind.DEFAULT;
        /** ריק או null — כל הסניפים */
        @Nullable public String branchId;
        @NonNull public String brandContains = "";
        @NonNull public String modelContains = "";
        @NonNull public String vehicleTypeContains = "";
        @NonNull public String subTypeContains = "";
        @NonNull public String colorContains = "";
        @NonNull public String previousOwnershipContains = "";
        @NonNull public String plateContains = "";
        @Nullable public Integer yearMin;
        @Nullable public Integer yearMax;
        @Nullable public Double priceMin;
        @Nullable public Double priceMax;
        @Nullable public Integer kmMax;

        public boolean hasActiveFilters() {
            return (branchId != null && !branchId.trim().isEmpty())
                    || !brandContains.trim().isEmpty()
                    || !modelContains.trim().isEmpty()
                    || !vehicleTypeContains.trim().isEmpty()
                    || !subTypeContains.trim().isEmpty()
                    || !colorContains.trim().isEmpty()
                    || !previousOwnershipContains.trim().isEmpty()
                    || !plateContains.trim().isEmpty()
                    || yearMin != null
                    || yearMax != null
                    || priceMin != null
                    || priceMax != null
                    || kmMax != null;
        }

        public boolean hasActiveSortOrFilters() {
            return sortKind != SortKind.DEFAULT || hasActiveFilters();
        }

        @NonNull
        @Override
        public FilterState clone() {
            try {
                return (FilterState) super.clone();
            } catch (CloneNotSupportedException e) {
                FilterState c = new FilterState();
                c.sortKind = sortKind;
                c.branchId = branchId;
                c.brandContains = brandContains;
                c.modelContains = modelContains;
                c.vehicleTypeContains = vehicleTypeContains;
                c.subTypeContains = subTypeContains;
                c.colorContains = colorContains;
                c.previousOwnershipContains = previousOwnershipContains;
                c.plateContains = plateContains;
                c.yearMin = yearMin;
                c.yearMax = yearMax;
                c.priceMin = priceMin;
                c.priceMax = priceMax;
                c.kmMax = kmMax;
                return c;
            }
        }

        public void reset() {
            sortKind = SortKind.DEFAULT;
            branchId = null;
            brandContains = "";
            modelContains = "";
            vehicleTypeContains = "";
            subTypeContains = "";
            colorContains = "";
            previousOwnershipContains = "";
            plateContains = "";
            yearMin = null;
            yearMax = null;
            priceMin = null;
            priceMax = null;
            kmMax = null;
        }
    }

    @NonNull
    public static ArrayList<CarListing> apply(
            @Nullable List<CarListing> source,
            @NonNull FilterState state) {
        ArrayList<CarListing> out = new ArrayList<>();
        if (source == null) {
            return out;
        }
        String br = state.branchId != null ? state.branchId.trim() : "";
        String brandQ = state.brandContains.trim().toLowerCase(Locale.ROOT);
        String modelQ = state.modelContains.trim().toLowerCase(Locale.ROOT);
        String typeQ = state.vehicleTypeContains.trim().toLowerCase(Locale.ROOT);
        String subQ = state.subTypeContains.trim().toLowerCase(Locale.ROOT);
        String colorQ = state.colorContains.trim().toLowerCase(Locale.ROOT);
        String ownQ = state.previousOwnershipContains.trim().toLowerCase(Locale.ROOT);
        String plateQ = state.plateContains.trim().toLowerCase(Locale.ROOT);

        for (CarListing c : source) {
            if (!br.isEmpty()) {
                String bid = c.getBranchId() != null ? c.getBranchId() : "";
                if (!bid.equals(br)) {
                    continue;
                }
            }
            if (!brandQ.isEmpty()) {
                String b = c.getBrand() != null ? c.getBrand().toLowerCase(Locale.ROOT) : "";
                if (!b.contains(brandQ)) continue;
            }
            if (!modelQ.isEmpty()) {
                String m = c.getModelName() != null ? c.getModelName().toLowerCase(Locale.ROOT) : "";
                if (!m.contains(modelQ)) continue;
            }
            if (!typeQ.isEmpty()) {
                String t = c.getVehicleType() != null ? c.getVehicleType().toLowerCase(Locale.ROOT) : "";
                if (!t.contains(typeQ)) continue;
            }
            if (!subQ.isEmpty()) {
                String s = c.getSubType() != null ? c.getSubType().toLowerCase(Locale.ROOT) : "";
                if (!s.contains(subQ)) continue;
            }
            if (!colorQ.isEmpty()) {
                String col = c.getColor() != null ? c.getColor().toLowerCase(Locale.ROOT) : "";
                if (!col.contains(colorQ)) continue;
            }
            if (!ownQ.isEmpty()) {
                String o = c.getPreviousOwnership() != null
                        ? c.getPreviousOwnership().toLowerCase(Locale.ROOT) : "";
                if (!o.contains(ownQ)) continue;
            }
            if (!plateQ.isEmpty()) {
                String p = c.getPlateNumber() != null ? c.getPlateNumber().toLowerCase(Locale.ROOT) : "";
                if (!p.contains(plateQ)) continue;
            }
            int y = parseYear(c.getManufactureYear());
            if (state.yearMin != null && y > 0 && y < state.yearMin) continue;
            if (state.yearMax != null && y > 0 && y > state.yearMax) continue;
            if (state.yearMin != null && y <= 0) continue;
            if (state.yearMax != null && y <= 0) continue;

            double sale = c.getSalePrice();
            if (state.priceMin != null && sale < state.priceMin) continue;
            if (state.priceMax != null && sale > state.priceMax) continue;

            if (state.kmMax != null && c.getKilometer() > state.kmMax) continue;

            out.add(c);
        }
        sortInPlace(out, state.sortKind);
        return out;
    }

    private static int parseYear(@Nullable String manufactureYear) {
        if (manufactureYear == null) return 0;
        String s = manufactureYear.trim();
        if (s.isEmpty()) return 0;
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            if (digits.length() >= 4) {
                return Integer.parseInt(digits.substring(0, 4));
            }
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void sortInPlace(@NonNull ArrayList<CarListing> list, @NonNull SortKind kind) {
        Comparator<CarListing> cmp;
        switch (kind) {
            case SALE_PRICE_ASC:
                cmp = Comparator.comparingDouble(CarListing::getSalePrice);
                break;
            case SALE_PRICE_DESC:
                cmp = Comparator.comparingDouble(CarListing::getSalePrice).reversed();
                break;
            case CATALOG_PRICE_ASC:
                cmp = Comparator.comparingDouble(CarListing::getCatalogPrice);
                break;
            case CATALOG_PRICE_DESC:
                cmp = Comparator.comparingDouble(CarListing::getCatalogPrice).reversed();
                break;
            case YEAR_DESC:
                cmp = Comparator.comparingInt((CarListing c) -> parseYear(c.getManufactureYear())).reversed();
                break;
            case YEAR_ASC:
                cmp = Comparator.comparingInt(c -> parseYear(c.getManufactureYear()));
                break;
            case KM_ASC:
                cmp = Comparator.comparingInt(CarListing::getKilometer);
                break;
            case KM_DESC:
                cmp = Comparator.comparingInt(CarListing::getKilometer).reversed();
                break;
            case BRAND_ASC:
                cmp = Comparator.comparing(c -> nz(c.getBrand()).toLowerCase(Locale.ROOT));
                break;
            case MODEL_ASC:
                cmp = Comparator.comparing(c -> nz(c.getModelName()).toLowerCase(Locale.ROOT));
                break;
            case PLATE_ASC:
                cmp = Comparator.comparing(c -> nz(c.getPlateNumber()).toLowerCase(Locale.ROOT));
                break;
            case DEFAULT:
            default:
                return;
        }
        Collections.sort(list, cmp);
    }

    @NonNull
    private static String nz(@Nullable String s) {
        return s != null ? s : "";
    }
}
