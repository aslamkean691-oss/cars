package com.example.cars.model;

/**
 * שילוב יצרן+דגם+סמל לשימוש חוזר בהוספת רכב (נשמר מקומית).
 */
public class CarBrandModelPreset {

    private String brand;
    private String modelName;
    /** ריק = לפי סוג הרכב (מורשת לתאימות לאחור) */
    private String listIconEmoji;
    /** מפתח לוגו יצרן מ־{@link CarBrandLogoCatalog} */
    private String brandLogoKey;

    public CarBrandModelPreset() {}

    public CarBrandModelPreset(String brand, String modelName, String listIconEmoji, String brandLogoKey) {
        this.brand = brand;
        this.modelName = modelName;
        this.listIconEmoji = listIconEmoji;
        this.brandLogoKey = brandLogoKey;
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
}
