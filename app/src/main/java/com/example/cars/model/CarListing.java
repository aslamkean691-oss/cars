package com.example.cars.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/**
 * רכב במגרש — כל השדות נשמרים ב-Appwrite (אוסף cars).
 */
public class CarListing {

    @SerializedName("$id")
    private String id;

    private String ownerUserId;
    private String branchId;
    private String plateNumber;
    /** יצרן / מותג, למשל מאזדה, טויוטה */
    private String brand;
    /** דגם מפורט, למשל מאזדה 3 */
    private String modelName;
    /**
     * אימוג׳י לתצוגה ברשימה. ריק — לפי לוגו יצרן / סוג הרכב ({@link CarBrandLogoCatalog}, {@link CarTypeIcons}).
     * ב־Appwrite לעיתים שדה חובה: שלחו מחרוזת ריקה {@code ""} ולא {@code null}.
     */
    private String listIconEmoji;
    /**
     * מפתח לוגו יצרן מתוך {@link CarBrandLogoCatalog} (למשל mazda). ריק — לפי סוג / אימוג׳י ישן.
     * שדה string ב־Appwrite: brandLogoKey
     */
    private String brandLogoKey;
    private String vehicleType;
    private String subType;
    private String manufactureYear;
    private String color;
    private int kilometer;
    private String previousOwnership;
    private String description;
    private double catalogPrice;
    private double salePrice;
    private String imageUrl1;
    private String imageUrl2;
    private String imageUrl3;
    private String imageUrl4;
    private String imageUrl5;
    private String imageUrl6;
    /** כשנמכר — מוצג במסך \"רכבים שנמכרו\" למנהל; הוסף שדה boolean `sold` באוסף cars ב-Appwrite */
    private boolean sold;
    /**
     * אם false — הרכב מוסתר מרשימת הלקוחות (null או חסר ב-JSON = מוצג).
     * הוסף באוסף cars מאפיין boolean בשם displayToCustomer (או התאם ל-serialization).
     */
    private Boolean displayToCustomer;

    public CarListing() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getListIconEmoji() {
        return listIconEmoji;
    }

    public void setListIconEmoji(String listIconEmoji) {
        this.listIconEmoji = listIconEmoji;
    }

    public String getBrandLogoKey() {
        return brandLogoKey;
    }

    public void setBrandLogoKey(String brandLogoKey) {
        this.brandLogoKey = brandLogoKey;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getManufactureYear() {
        return manufactureYear;
    }

    public void setManufactureYear(String manufactureYear) {
        this.manufactureYear = manufactureYear;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getKilometer() {
        return kilometer;
    }

    public void setKilometer(int kilometer) {
        this.kilometer = kilometer;
    }

    public String getPreviousOwnership() {
        return previousOwnership;
    }

    public void setPreviousOwnership(String previousOwnership) {
        this.previousOwnership = previousOwnership;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getCatalogPrice() {
        return catalogPrice;
    }

    public void setCatalogPrice(double catalogPrice) {
        this.catalogPrice = catalogPrice;
    }

    public double getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(double salePrice) {
        this.salePrice = salePrice;
    }

    public String getImageUrl1() {
        return imageUrl1;
    }

    public void setImageUrl1(String imageUrl1) {
        this.imageUrl1 = imageUrl1;
    }

    public String getImageUrl2() {
        return imageUrl2;
    }

    public void setImageUrl2(String imageUrl2) {
        this.imageUrl2 = imageUrl2;
    }

    public String getImageUrl3() {
        return imageUrl3;
    }

    public void setImageUrl3(String imageUrl3) {
        this.imageUrl3 = imageUrl3;
    }

    public String getImageUrl4() {
        return imageUrl4;
    }

    public void setImageUrl4(String imageUrl4) {
        this.imageUrl4 = imageUrl4;
    }

    public String getImageUrl5() {
        return imageUrl5;
    }

    public void setImageUrl5(String imageUrl5) {
        this.imageUrl5 = imageUrl5;
    }

    public String getImageUrl6() {
        return imageUrl6;
    }

    public void setImageUrl6(String imageUrl6) {
        this.imageUrl6 = imageUrl6;
    }

    public boolean isSold() {
        return sold;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public boolean isDisplayedToCustomer() {
        return displayToCustomer == null || displayToCustomer;
    }

    public void setDisplayedToCustomer(Boolean displayToCustomer) {
        this.displayToCustomer = displayToCustomer;
    }

    /** תמונה ראשונה לתצוגה ברשימה */
    public String getPrimaryImageUrl() {
        if (imageUrl1 != null && !imageUrl1.isEmpty()) return imageUrl1;
        if (imageUrl2 != null && !imageUrl2.isEmpty()) return imageUrl2;
        if (imageUrl3 != null && !imageUrl3.isEmpty()) return imageUrl3;
        if (imageUrl4 != null && !imageUrl4.isEmpty()) return imageUrl4;
        if (imageUrl5 != null && !imageUrl5.isEmpty()) return imageUrl5;
        if (imageUrl6 != null && !imageUrl6.isEmpty()) return imageUrl6;
        return "";
    }

    @NonNull
    @Override
    public String toString() {
        return vehicleType + " " + plateNumber;
    }
}
