package com.example.cars.Hellper;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import com.example.cars.model.AdminAccount;
import com.example.cars.model.AdminSecurityState;
import com.example.cars.model.AppwriteCollections;

/**
 * طبقة الوصول إلى البيانات - طبقة وسيطة عامة للتطبيق مع Appwrite
 * 
 * هذا الكلاس يوفر واجهة موحدة وسهلة لإدارة:
 * - قواعد البيانات والمستندات
 * - المستخدمين والمصادقة  
 * - الملفات والتخزين
 * 
 * يمكن لأي مطور استخدام هذا الكلاس لإدارة البيانات بسهولة
 */
public class DALAppWriteConnection {
    
    private static final String TAG = "DALAppWriteConnection";
    /** תג נוסף לסינון ב-Logcat (רק ASCII) */
    private static final String TAG_LOGIN = "APPWRITE_LOGIN";
    /** טוקן סשן ב-prefs כשההתחברות מאוסף {@link AppwriteCollections#ADMIN_ACCOUNTS} בלבד. */
    private static final String DB_SESSION_PREFIX = "DBSESSION|";

    // === إعدادات الاتصال مع Appwrite ===
    private static final String BASE_URL = "https://fra.cloud.appwrite.io/v1";
    private static final String PROJECT_ID = "6907346c003127baa02c";
    private static final String API_KEY = "standard_f9c90d573e5f1e69fd69859b5c15534d229079ca3c04ed66345128a755158626b26049832fae25e4be0e7c274c67b64e7835de79715ba64f2d364c6464b7fe7d2379d60002246dfcc91a2f3821ba09c5c8a477116c9c681c3a907a1f1e64482fe4af6cbf32e15da294b270553242d993502c638076bc36f3111cf5ac311adcbf";
    private static final String MAIN_DATABASE_ID = "690c601d0020a4b09dcb"; // AppDb
    private static final String MAIN_STORAGE_BUCKET_ID = "69073c84002d7805b1f0"; // AppWriteStorage
    private static final String USER_FILES_BUCKET_ID = "69056d1b001eba1f06eb"; // يمكن تغييره لاحقاً
    
    // === متغيرات حالة الجلسة ===
    private String currentUserId = null;
    private String currentUserEmail = null;
    private String sessionId = null;
    /** JWT מ-Appwrite או טוקן מקומי {@link #DB_SESSION_PREFIX} כשהכניסה מאוסף admin_accounts */
    private String sessionJwt = null;
    /** true אם נכנסו דרך אוסף admin_accounts (ללא Auth). */
    private boolean sessionFromDatabaseTable = false;

    private Context context;
    private Gson gson;
    /** מילוי כש־{@link #saveDocument} נכשל — כדי להחזיר הודעת Appwrite ב־{@link #saveData} */
    private volatile String lastSaveDocumentError;
    
    /**
     * منشئ الكلاس الرئيسي
     * @param context تطبيق أندرويد للحصول على الأذونات والموارد
     */
    public DALAppWriteConnection(Context context) {
        this.context = context;
        this.gson = new Gson();
        
    }

    public static String appwriteProjectId() {
        return PROJECT_ID;
    }

    public static String appwriteApiKey() {
        return API_KEY;
    }

    public static String appwriteBaseUrl() {
        return BASE_URL;
    }

    /**
     * שחזור מצב משתמש מהטוקן שנשמר ב-SharedPreferences (בלי קריאת רשת).
     */
    public void restoreSessionFromJwt(String jwt, String userId, String email) {
        this.sessionJwt = jwt;
        this.currentUserId = userId;
        this.currentUserEmail = email;
        this.sessionId = userId;
        this.sessionFromDatabaseTable = jwt != null && jwt.startsWith(DB_SESSION_PREFIX);
    }

    public String getSessionJwt() {
        return sessionJwt;
    }
    
    /**
     * اختبار الاتصال مع Appwrite
     * @return true إذا كان الاتصال ناجح، false إذا فشل
     */
    public boolean testConnection() {
        try {
            URL url = new URL(BASE_URL + "/health");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return responseCode == HttpURLConnection.HTTP_OK || responseCode == 401;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    // === نماذج البيانات الأساسية ===
    
    /**
     * نموذج بيانات المستخدم
     */
    public static class UserData {
        public String userId;
        public String email;
        public String firstName;
        public String lastName;
        public String phone;
        public String profileImageUrl;
        public java.util.Date createdAt;
        public java.util.Date lastLoginAt;
        public boolean isActive;
        /** JWT מ-session (secret) — לשמירה ב-SharedPreferences בלבד */
        public String sessionJwt;
        
        public UserData() {}
        
        public UserData(String userId, String email, String firstName, String lastName) {
            this.userId = userId;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.createdAt = new java.util.Date();
            this.isActive = true;
        }
    }
    
    /**
     * نموذج نتيجة العملية
     */
    public static class OperationResult<T> {
        public boolean success;
        public String message;
        public T data;
        public String errorCode;
        
        public OperationResult() {}
        
        public OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public OperationResult(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }
    
    /**
     * نموذج الملف المرفوع
     */
    public static class FileInfo {
        public String fileId;
        public String fileName;
        public String fileUrl;
        public String mimeType;
        public long fileSize;
        public String uploadedBy;
        public java.util.Date uploadDate;
        public String bucketId;
        
        public FileInfo() {}
    }
    
    // === دوال إدارة المستخدمين ===
    
    /**
     * إنشاء مستخدم افتراضي جديد في النظام
     * @param email البريد الإلكتروني للمستخدم
     * @param password كلمة المرور
     * @param firstName الاسم الأول
     * @param lastName الاسم الأخير
     * @param phone رقم الهاتف (اختياري)
     * @return نتيجة العملية مع بيانات المستخدم
     * 
     * مثال على الاستخدام:
     * UserData user = new UserData();
     * user.email = "test@example.com";
     * user.firstName = "أحمد";
     * user.lastName = "محمد";
     * 
     * OperationResult<UserData> result = dal.createDefaultUser(user.email, "password123", 
     *                                                          user.firstName, user.lastName, "0123456789");
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم إنشاء المستخدم: " + result.data.userId);
     * } else {
     *     Log.e("ERROR", "فشل في إنشاء المستخدم: " + result.message);
     * }
     */
    public OperationResult<UserData> createDefaultUser(String email, String password, 
                                                      String firstName, String lastName, String phone) {
        try {
            // إنشاء معرف فريد للمستخدم
            String userId = UUID.randomUUID().toString();
            
            // إعداد بيانات المستخدم
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", userId);
            userData.put("email", email);
            userData.put("firstName", firstName);
            userData.put("lastName", lastName);
            userData.put("phone", phone != null ? phone : "");
            userData.put("createdAt", new Date().toString());
            userData.put("isActive", true);
            
            // إنشاء المستخدم في Appwrite
            URL url = new URL(BASE_URL + "/account");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            
            // إرسال بيانات المستخدم
            String jsonBody = String.format(
                "{\"userId\":\"%s\", \"email\":\"%s\", \"password\":\"%s\", \"name\":\"%s %s\"}",
                userId, email, password, firstName, lastName
            );
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 201 || responseCode == 200) {
                // إنشاء جدول خاص بالمستخدم
                createUserTable(userId);
                
                // إنشاء مستخدم في قاعدة البيانات المحلية
                UserData user = new UserData(userId, email, firstName, lastName);
                user.phone = phone;
                user.createdAt = new Date();
                
                
                return new OperationResult<>(true, "تم إنشاء المستخدم بنجاح", user);
            } else {
                String errorMessage = readErrorResponse(connection);
                return new OperationResult<>(false, "فشل في إنشاء المستخدم: " + errorMessage);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في إنشاء المستخدم: " + e.getMessage());
        }
    }
    
    /**
     * تسجيل الدخول للمستخدم الموجود
     * @param email البريد الإلكتروني
     * @param password كلمة المرور
     * @return نتيجة العملية مع بيانات المستخدم
     * 
     * مثال على الاستخدام:
     * OperationResult<UserData> result = dal.loginUser("user@example.com", "password123");
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم تسجيل الدخول بنجاح");
     *     Log.d("USER", "معرف المستخدم: " + result.data.userId);
     *     Log.d("EMAIL", "البريد الإلكتروني: " + result.data.email);
     * } else {
     *     Log.e("ERROR", "فشل تسجيل الدخول: " + result.message);
     * }
     */
    public OperationResult<UserData> loginUser(String email, String password) {
        Log.w(TAG_LOGIN, "loginUser (admin_accounts DB) email=" + (email != null ? email : "null"));
        if (email == null || email.trim().isEmpty()) {
            return new OperationResult<>(false, "חסר אימייל");
        }
        if (password == null) {
            return new OperationResult<>(false, "חסרה סיסמה");
        }
        if (!ensureAdminAccountsCollection()) {
            Log.e(TAG_LOGIN, "ensureAdminAccountsCollection failed");
            return new OperationResult<>(false,
                    "לא ניתן ליצור את אוסף admin_accounts — בדוק הרשאות API Key ל-Databases");
        }
        OperationResult<ArrayList<AdminAccount>> listed = listAdminAccountsByEmail(email.trim());
        if (!listed.success || listed.data == null) {
            Log.e(TAG_LOGIN, "list admin_accounts failed: " + listed.message);
            return new OperationResult<>(false,
                    listed.message != null ? listed.message : "שגיאת שרת");
        }
        AdminAccount match = null;
        for (AdminAccount row : listed.data) {
            if (row == null) {
                continue;
            }
            String em = row.getEmail() != null ? row.getEmail().trim() : "";
            String pw = row.getPassword() != null ? row.getPassword() : "";
            if (em.equalsIgnoreCase(email.trim()) && pw.equals(password)) {
                match = row;
                break;
            }
        }
        if (match == null || match.getId() == null || match.getId().isEmpty()) {
            Log.w(TAG_LOGIN, "no admin_accounts row matched email");
            return new OperationResult<>(false, "אימייל או סיסמה שגויים");
        }
        String docId = match.getId();
        String token = DB_SESSION_PREFIX + docId + "|" + UUID.randomUUID();
        this.sessionFromDatabaseTable = true;
        this.currentUserId = docId;
        this.currentUserEmail = match.getEmail() != null ? match.getEmail().trim() : email.trim();
        this.sessionId = docId;
        this.sessionJwt = token;

        UserData user = new UserData(docId, this.currentUserEmail, "", "");
        user.sessionJwt = token;
        Log.w(TAG_LOGIN, "login OK table docId=" + docId);
        Log.w(TAG, "loginUser: הצלחה (אוסף admin_accounts)");
        return new OperationResult<>(true, "تم تسجيل الدخول بنجاح", user);
    }

    private boolean isDatabaseTableSession() {
        return sessionFromDatabaseTable
                || (sessionJwt != null && sessionJwt.startsWith(DB_SESSION_PREFIX));
    }

    /**
     * יוצר את אוסף {@link AppwriteCollections#ADMIN_ACCOUNTS} בפעם הראשונה (API Key) ומזריע שורות ברירת מחדל אם ריק.
     */
    private boolean ensureAdminAccountsCollection() {
        final String cid = AppwriteCollections.ADMIN_ACCOUNTS;
        if (tableExists(cid, null)) {
            return true;
        }
        Log.w(TAG_LOGIN, "admin_accounts missing — creating collection (first run)");
        String schema = "email:string:true:email,password:string:true:password";
        if (!createTable(cid, null, schema)) {
            Log.e(TAG_LOGIN, "createTable admin_accounts failed");
            return false;
        }
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!tableExists(cid, null)) {
            Log.e(TAG_LOGIN, "admin_accounts still not listed after create");
            return false;
        }
        seedDefaultAdminAccountsIfEmpty();
        return true;
    }

    private void seedDefaultAdminAccountsIfEmpty() {
        try {
            OperationResult<ArrayList<AdminAccount>> all =
                    getData(AppwriteCollections.ADMIN_ACCOUNTS, null, AdminAccount.class);
            if (!all.success || all.data == null || !all.data.isEmpty()) {
                return;
            }
            AdminAccount primary = new AdminAccount();
            primary.setEmail(SuperAdminConfig.EMAIL);
            primary.setPassword(SuperAdminConfig.DEFAULT_PASSWORD);
            OperationResult<ArrayList<AdminAccount>> r1 =
                    saveData(primary, AppwriteCollections.ADMIN_ACCOUNTS, null);
            if (!r1.success) {
                Log.w(TAG_LOGIN, "seed row " + SuperAdminConfig.EMAIL + " failed: " + r1.message);
            }
            AdminAccount alias = new AdminAccount();
            alias.setEmail(SuperAdminConfig.EMAIL_ALIAS);
            alias.setPassword(SuperAdminConfig.DEFAULT_PASSWORD);
            OperationResult<ArrayList<AdminAccount>> r2 =
                    saveData(alias, AppwriteCollections.ADMIN_ACCOUNTS, null);
            if (!r2.success) {
                Log.w(TAG_LOGIN, "seed row " + SuperAdminConfig.EMAIL_ALIAS + " failed: " + r2.message);
            }
            if (r1.success || r2.success) {
                Log.w(TAG_LOGIN, "seeded default admin_accounts (change passwords after first login)");
            }
        } catch (Throwable t) {
            Log.w(TAG_LOGIN, "seedDefaultAdminAccountsIfEmpty: " + t.getMessage());
        }
    }

    /** כל השורות עם אימייל תואם (ללא שאילתת queries — לא דורש אינדקס). */
    private OperationResult<ArrayList<AdminAccount>> listAdminAccountsByEmail(String email) {
        String emailNorm = email.trim();
        OperationResult<ArrayList<AdminAccount>> all =
                getData(AppwriteCollections.ADMIN_ACCOUNTS, null, AdminAccount.class);
        if (!all.success || all.data == null) {
            return all;
        }
        ArrayList<AdminAccount> matches = new ArrayList<>();
        for (AdminAccount row : all.data) {
            if (row == null) {
                continue;
            }
            String em = row.getEmail() != null ? row.getEmail().trim() : "";
            if (em.equalsIgnoreCase(emailNorm)) {
                matches.add(row);
            }
        }
        Log.w(TAG_LOGIN, "admin_accounts scan matched " + matches.size() + " row(s) for email");
        return new OperationResult<>(true, "ok", matches);
    }

    private OperationResult<Void> updateTableAdminPassword(String oldPassword, String newPassword) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            return new OperationResult<>(false, "אין מזהה משתמש");
        }
        OperationResult<AdminAccount> cur =
                getDataById(AppwriteCollections.ADMIN_ACCOUNTS, currentUserId, null, AdminAccount.class);
        if (!cur.success || cur.data == null) {
            return new OperationResult<>(false,
                    "לא ניתן לטעון את רשומת המנהל: " + (cur.message != null ? cur.message : ""));
        }
        String stored = cur.data.getPassword() != null ? cur.data.getPassword() : "";
        if (!stored.equals(oldPassword)) {
            return new OperationResult<>(false, "סיסמה נוכחית שגויה");
        }
        cur.data.setPassword(newPassword);
        OperationResult<AdminAccount> upd =
                updateData(cur.data, AppwriteCollections.ADMIN_ACCOUNTS, currentUserId, null);
        if (upd.success) {
            return new OperationResult<>(true, "סיסמה עודכנה");
        }
        return new OperationResult<>(false, upd.message != null ? upd.message : "עדכון נכשל");
    }

    private static String truncateForLog(String s) {
        if (s == null) {
            return "null";
        }
        final int max = 2000;
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "... [נחתך]";
    }

    private static boolean shouldRetryPostWithoutSchema(@Nullable String errorResponse) {
        if (errorResponse == null || errorResponse.isEmpty()) {
            return false;
        }
        String e = errorResponse.toLowerCase(Locale.ROOT);
        return e.contains("unknown attribute")
                || e.contains("document_invalid_structure")
                || e.contains("attribute_not_found")
                || e.contains("invalid document")
                || e.contains("general_unknown");
    }

    @Nullable
    private static String extractUnknownAttributeName(@Nullable String err) {
        if (err == null || err.isEmpty()) {
            return null;
        }
        Matcher m = Pattern.compile("Unknown attribute[^A-Za-z0-9_]*([A-Za-z0-9_]+)", Pattern.CASE_INSENSITIVE)
                .matcher(err);
        if (m.find()) {
            return m.group(1);
        }
        Matcher m2 = Pattern.compile("Unknown attribute:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(err);
        if (m2.find()) {
            return m2.group(1);
        }
        return null;
    }

    /**
     * עדכון סיסמה: אם נכנסו דרך אוסף {@link AppwriteCollections#ADMIN_ACCOUNTS} — עדכון במסמך;
     * אחרת ניסיון דרך Appwrite Auth (JWT).
     */
    public OperationResult<Void> updateAccountPassword(String oldPassword, String newPassword) {
        if (isDatabaseTableSession()) {
            if (oldPassword == null || newPassword == null || newPassword.length() < 8) {
                return new OperationResult<>(false, "סיסמה חדשה חייבת לפחות 8 תווים");
            }
            return updateTableAdminPassword(oldPassword, newPassword);
        }
        if (sessionJwt == null || sessionJwt.isEmpty()) {
            return new OperationResult<>(false, "אין סשן — התחבר מחדש");
        }
        if (oldPassword == null || newPassword == null || newPassword.length() < 8) {
            return new OperationResult<>(false, "סיסמה חדשה חייבת לפחות 8 תווים");
        }
        try {
            URL url = new URL(BASE_URL + "/account/password");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PATCH");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-JWT", sessionJwt);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);

            Map<String, String> body = new HashMap<>();
            body.put("password", newPassword);
            body.put("oldPassword", oldPassword);
            String jsonBody = gson.toJson(body);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }
            int code = connection.getResponseCode();
            if (code >= 200 && code < 300) {
                return new OperationResult<>(true, "סיסמה עודכנה");
            }
            return new OperationResult<>(false, readErrorResponse(connection));
        } catch (Exception e) {
            return new OperationResult<>(false, e.getMessage());
        }
    }

    /**
     * יוצר מסמך admin_security למנהל ראשי אם חסר (לאחר כניסה ראשונה).
     */
    public void ensureAdminSecurityRecord(String userId, String email) {
        if (!SuperAdminConfig.isSuperAdminEmail(email) || userId == null || userId.isEmpty()) {
            return;
        }
        try {
            AdminSecurityState existing = findAdminSecurityState(userId);
            if (existing != null) {
                return;
            }
            AdminSecurityState doc = new AdminSecurityState();
            doc.setUserId(userId);
            doc.setEmail(email.trim());
            doc.setHasChangedDefaultPassword(false);
            saveData(doc, AppwriteCollections.ADMIN_SECURITY, null);
        } catch (Exception ignored) {
        }
    }

    /** null אם אין מסמך או שגיאה */
    public AdminSecurityState findAdminSecurityState(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        try {
            OperationResult<ArrayList<AdminSecurityState>> res =
                    getData(AppwriteCollections.ADMIN_SECURITY, null, AdminSecurityState.class);
            if (!res.success || res.data == null) {
                return null;
            }
            for (AdminSecurityState s : res.data) {
                if (userId.equals(s.getUserId())) {
                    return s;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public OperationResult<Void> markSuperAdminPasswordChanged(String documentId) {
        if (documentId == null || documentId.isEmpty()) {
            return new OperationResult<>(false, "חסר מזהה מסמך");
        }
        AdminSecurityState patch = new AdminSecurityState();
        patch.setId(documentId);
        patch.setHasChangedDefaultPassword(true);
        OperationResult<AdminSecurityState> u =
                updateData(patch, AppwriteCollections.ADMIN_SECURITY, documentId, null);
        if (u.success) {
            return new OperationResult<>(true, u.message != null ? u.message : "עודכן");
        }
        return new OperationResult<>(false, u.message);
    }

    /**
     * فحص حالة تسجيل الدخول للمستخدم الحالي
     * @return true إذا كان المستخدم مسجل الدخول، false إذا لم يكن كذلك
     * 
     * مثال على الاستخدام:
     * if (dal.isUserLoggedIn()) {
     *     Log.d("STATUS", "المستخدم مسجل الدخول");
     *     Log.d("USER_ID", "معرف المستخدم: " + dal.getCurrentUserId());
     *     Log.d("EMAIL", "البريد الإلكتروني: " + dal.getCurrentUserEmail());
     * } else {
     *     Log.d("STATUS", "المستخدم غير مسجل الدخول");
     * }
     */
    public boolean isUserLoggedIn() {
        boolean isLoggedIn = sessionJwt != null && !sessionJwt.isEmpty()
                && currentUserId != null && !currentUserId.isEmpty();
        
        if (isLoggedIn) {
        } else {
        }
        
        return isLoggedIn;
    }
    
    /**
     * تسجيل خروج المستخدم الحالي
     * @return نتيجة العملية
     * 
     * مثال على الاستخدام:
     * OperationResult<Void> result = dal.logoutUser();
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم تسجيل الخروج بنجاح");
     *     // يمكن الآن توجيه المستخدم لصفحة تسجيل الدخول
     * } else {
     *     Log.e("ERROR", "فشل تسجيل الخروج: " + result.message);
     * }
     */
    public OperationResult<Void> logoutUser() {
        if (isDatabaseTableSession()) {
            this.currentUserId = null;
            this.currentUserEmail = null;
            this.sessionId = null;
            this.sessionJwt = null;
            this.sessionFromDatabaseTable = false;
            return new OperationResult<>(true, "تم تسجيل الخروج بنجاح");
        }
        try {
            URL url = new URL(BASE_URL + "/account/sessions/current");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            if (sessionJwt != null && !sessionJwt.isEmpty()) {
                connection.setRequestProperty("X-Appwrite-JWT", sessionJwt);
            } else {
                connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            }
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();

            this.currentUserId = null;
            this.currentUserEmail = null;
            this.sessionId = null;
            this.sessionJwt = null;
            this.sessionFromDatabaseTable = false;

            if (responseCode == 200 || responseCode == 204) {
                return new OperationResult<>(true, "تم تسجيل الخروج بنجاح");
            } else {
                return new OperationResult<>(false, "فشل تسجيل الخروج");
            }

        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في تسجيل الخروج: " + e.getMessage());
        }
    }
    
    /**
     * حذف المستخدم الحالي من النظام
     * @param userId معرف المستخدم المراد حذفه
     * @return نتيجة العملية
     * 
     * تحذير: هذه العملية نهائية ولا يمكن التراجع عنها
     * 
     * مثال على الاستخدام:
     * OperationResult<Void> result = dal.deleteUser("user-uuid-here");
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم حذف المستخدم بنجاح");
     * } else {
     *     Log.e("ERROR", "فشل حذف المستخدم: " + result.message);
     * }
     */
    public OperationResult<Void> deleteUser(String userId) {
        try {
            // حذف المستخدم من Appwrite
            URL url = new URL(BASE_URL + "/account");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            // حذف جدول المستخدم
            deleteUserTable(userId);
            
            // مسح بيانات الجلسة إذا كان المستخدم الحالي
            if (currentUserId != null && currentUserId.equals(userId)) {
                this.currentUserId = null;
                this.currentUserEmail = null;
                this.sessionId = null;
            }
            
            if (responseCode == 200 || responseCode == 204) {
                return new OperationResult<>(true, "تم حذف المستخدم بنجاح");
            } else {
                String errorMessage = readErrorResponse(connection);
                return new OperationResult<>(false, "فشل حذف المستخدم: " + errorMessage);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في حذف المستخدم: " + e.getMessage());
        }
    }
    
    /**
     * إنشاء مستخدم جديد مع جدول مخصص له
     * @param userModel نموذج المستخدم (يجب أن يحتوي على userId، email، وبيانات إضافية)
     * @param password كلمة المرور
     * @param tableName اسم الجدول الخاص بالمستخدم
     * @return نتيجة العملية مع بيانات المستخدم المحدثة
     * 
     * مثال على الاستخدام:
     * UserData user = new UserData();
     * user.email = "newuser@example.com";
     * user.firstName = "سارة";
     * user.lastName = "أحمد";
     * user.userId = "custom-user-id";
     * 
     * OperationResult<UserData> result = dal.registerNewUser(user, "securepassword", "user_posts");
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم تسجيل المستخدم الجديد");
     *     Log.d("TABLE", "تم إنشاء جدول: " + "user_posts");
     * }
     */
    public OperationResult<UserData> registerNewUser(UserData userModel, String password, String tableName) {
        try {
            if (userModel.userId == null || userModel.userId.isEmpty()) {
                userModel.userId = UUID.randomUUID().toString();
            }
            
            // إنشاء المستخدم في Appwrite
            OperationResult<UserData> userResult = createDefaultUser(
                userModel.email, password, userModel.firstName, userModel.lastName, userModel.phone
            );
            
            if (!userResult.success) {
                return userResult;
            }
            
            // إنشاء جدول مخصص للمستخدم
            boolean tableCreated = createUserTable(userModel.userId, tableName);
            
            if (tableCreated) {
                userModel.createdAt = new Date();
                userModel.isActive = true;
                
                return new OperationResult<>(true, "تم تسجيل المستخدم الجديد بنجاح", userModel);
            } else {
                return new OperationResult<>(false, "تم إنشاء المستخدم لكن فشل في إنشاء الجدول المخصص");
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في تسجيل المستخدم الجديد: " + e.getMessage());
        }
    }
    
    // === دوال مساعدة للمستخدمين ===
    
    /**
     * الحصول على معرف المستخدم الحالي
     * @return معرف المستخدم أو null إذا لم يكن مسجل الدخول
     */
    public String getCurrentUserId() {
        return currentUserId;
    }
    
    /**
     * الحصول على البريد الإلكتروني للمستخدم الحالي
     * @return البريد الإلكتروني أو null إذا لم يكن مسجل الدخول
     */
    public String getCurrentUserEmail() {
        return currentUserEmail;
    }
    
    /**
     * إنشاء جدول خاص بالمستخدم
     * @param userId معرف المستخدم
     * @return true إذا تم الإنشاء بنجاح، false إذا فشل
     */
    private boolean createUserTable(String userId) {
        return createUserTable(userId, "user_" + userId.replaceAll("-", "_") + "_data");
    }
    
    /**
     * إنشاء جدول مخصص للمستخدم
     * @param userId معرف المستخدم
     * @param tableName اسم الجدول
     * @return true إذا تم الإنشاء بنجاح، false إذا فشل
     */
    private boolean createUserTable(String userId, String tableName) {
        try {
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            
            // إعداد بيانات الجدول (Collection) بشكل صحيح لـ Appwrite
            Map<String, Object> collectionData = new HashMap<>();
            collectionData.put("collectionId", UUID.randomUUID().toString());
            collectionData.put("name", "جدول بيانات " + userId);
            collectionData.put("createdFrom", "DALAppWriteConnection");
            collectionData.put("userId", userId);
            
            String jsonBody = gson.toJson(collectionData);
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            if (responseCode == 201 || responseCode == 200) {
                
                // تعطيل permissions مؤقتاً - TODO: إصلاح API endpoints
                /*
                // إضافة Document permissions للجدول الجديد
                addDocumentPermissions(userId, tableName);
                */
                
                // تعطيل schema attributes مؤقتاً - TODO: إصلاح API endpoints
                /*
                // تفعيل إنشاء schema attributes إذا تم توفيرها
                if (tableName.toLowerCase().contains("clothes")) {
                    createDefaultClothesSchema(userId, tableName);
                }
                */
                
                return true;
            } else {
                // قراءة رسالة الخطأ
                String errorResponse = readErrorResponse(connection);
                return false;
            }
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * حذف جدول المستخدم
     * @param userId معرف المستخدم
     */
    private void deleteUserTable(String userId) {
        try {
            String tableName = "user_" + userId.replaceAll("-", "_") + "_data";
            
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + tableName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            if (responseCode == 200 || responseCode == 204) {
            }
            
        } catch (Exception e) {
        }
    }
    
    /**
     * قراءة الرسالة الخطأ من استجابة Appwrite
     * @param connection اتصال HTTP
     * @return رسالة الخطأ أو "فشل غير محدد"
     */
    /**
     * קריאת גוף שגיאה מ-Appwrite (או הודעת HTTP כשאין גוף).
     */
    private String readErrorResponse(HttpURLConnection connection) {
        int code = -1;
        try {
            code = connection.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG, "readErrorResponse: getResponseCode", e);
            return "getResponseCode: " + e.getMessage();
        }
        InputStream errStream = null;
        try {
            errStream = connection.getErrorStream();
        } catch (Exception e) {
            Log.w(TAG, "getErrorStream", e);
        }
        if (errStream == null) {
            Log.e(TAG_LOGIN, "readErrorResponse: errorStream null HTTP " + code);
            Log.w(TAG, "readErrorResponse: errorStream null, HTTP " + code);
            return "HTTP " + code + " (אין גוף שגיאה מפורט)";
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(errStream, StandardCharsets.UTF_8))) {
            StringBuilder errorMessage = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                errorMessage.append(line);
            }
            String body = errorMessage.toString().trim();
            if (body.isEmpty()) {
                return "HTTP " + code + " (גוף ריק)";
            }
            return body;
        } catch (Exception e) {
            Log.e(TAG_LOGIN, "readErrorResponse read body: " + e.getMessage(), e);
            Log.e(TAG, "readErrorResponse: read body", e);
            return "HTTP " + code + " — " + e.getMessage();
        }
    }
    
    /**
     * استخراج قيمة من سلسلة الاستجابة JSON
     * @param responseString سلسلة الاستجابة
     * @param key المفتاح المراد استخراج قيمته
     * @return قيمة المفتاح أو null إذا لم يكن موجوداً
     */
    private String extractValue(String responseString, String key) {
        try {
            // تحليل السلسلة الأولية
            String[] parts = responseString.split("\"");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals(key)) {
                    return parts[i + 2]; // القيمة هي بعد المفتاح والعلامة "
                }
            }
        } catch (Exception e) {
        }
        return null;
    }
    
    // === دوال إدارة قواعد البيانات العامة ===
    
    /**
     * حفظ كائن أو مجموعة كائنات في قاعدة البيانات
     * إذا كان الجدول غير موجود، سيتم إنشاؤه تلقائياً
     * إذا كان موجوداً، سيتم إضافة البيانات إليه
     * 
     * @param data البيانات المراد حفظها (كائن واحد أو ArrayList)
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @return نتيجة العملية مع قائمة الكائنات المحفوظة
     * 
     * مثال على الاستخدام:
     * // حفظ كائن واحد
     * Product product = new Product();
     * product.name = "هاتف ذكي";
     * product.price = 999.99;
     * 
     * OperationResult<ArrayList<Product>> result = dal.saveData(product, "products");
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم حفظ " + result.data.size() + " منتج");
     * } else {
     *     Log.e("ERROR", "فشل حفظ البيانات: " + result.message);
     * }
     * 
     * // حفظ مجموعة كائنات
     * ArrayList<Product> products = new ArrayList<>();
     * products.add(product1);
     * products.add(product2);
     * 
     * OperationResult<ArrayList<Product>> bulkResult = dal.saveData(products, "products");
     * 
     * if (bulkResult.success) {
     *     Log.d("BULK", "تم حفظ " + bulkResult.data.size() + " منتج");
     * }
     */
    public <T> OperationResult<ArrayList<T>> saveData(T data, String tableName, String collectionId) {
        try {
            // التحقق من صحة البيانات
            if (data == null) {
                return new OperationResult<>(false, "البيانات المراد حفظها لا يمكن أن تكون فارغة");
            }
            
            // معالجة الكائن الواحد
            ArrayList<T> dataList;
            if (!(data instanceof Collection)) {
                dataList = new ArrayList<>();
                dataList.add(data);
            } else {
                @SuppressWarnings("unchecked")
                ArrayList<T> castedList = (ArrayList<T>) data;
                dataList = castedList;
            }
            
            if (dataList.isEmpty()) {
                return new OperationResult<>(false, "قائمة البيانات فارغة");
            }
            
            // إنشاء الجدول إذا لم يكن موجوداً
            // إذا كان الجدول موجوداً مسبقاً، سيتم استخدامه مباشرة
            String expectedCollectionName = collectionId != null ? collectionId : tableName;
            
            // الحصول على الـ collection ID الفعلي
            String actualCollectionId = getActualCollectionId(tableName, collectionId);
            if (actualCollectionId == null) {
                actualCollectionId = expectedCollectionName;
            }
            
            
            // فقط إذا لم يكن موجوداً، سنحاول إنشاؤه
            if (!tableExists(tableName, collectionId)) {
                
                // استنتاج schema تلقائياً من الكائن الأول
                String schema = null;
                if (!dataList.isEmpty()) {
                    schema = inferSchemaFromObject(dataList.get(0));
                }
                
                boolean tableCreated = ensureTableExists(tableName, collectionId, schema); // مع schema
                if (!tableCreated) {
                    return new OperationResult<>(false, "فشل في إنشاء الجدول: " + tableName);
                }
                
                // تحديث الـ collection ID بعد الإنشاء
                actualCollectionId = getActualCollectionId(tableName, collectionId);
                if (actualCollectionId == null) {
                    actualCollectionId = expectedCollectionName;
                }
            } else {
                
                // التحقق من وجود attributes - إذا كانت فارغة، أضف schema تلقائياً
                String schema = null;
                if (!dataList.isEmpty()) {
                    schema = inferSchemaFromObject(dataList.get(0));
                    createTableAttributes(actualCollectionId, schema, tableName);
                }
            }
            
            
            ArrayList<T> savedItems = new ArrayList<>();
            int successCount = 0;
            
            // حفظ كل عنصر
            for (T item : dataList) {
                try {
                    // إنشاء مستند جديد أو تحديث موجود
                    String documentId = getObjectId(item);
                    if (documentId == null || documentId.isEmpty()) {
                        documentId = UUID.randomUUID().toString();
                    }
                    
                    Map<String, Object> documentData = convertObjectToMap(item);
                    documentData.entrySet().removeIf(e ->
                            e.getKey() != null && e.getKey().startsWith("$"));
                    
                    // تنظيف البيانات من الحقول غير المرغوب فيها
                    documentData.remove("documentId");
                    documentData.remove("createdAt");
                    documentData.remove("createdBy");
                    documentData.remove("class"); // إزالة أي حقول من Java
                    documentData.remove("$"); // إزالة أي حقول خاصة
                    documentData.remove("metadata"); // إزالة metadata - يسبب مشاكل
                    
                    // تحويل "id" إلى اسم فريد حسب نوع الكائن
                    if (documentData.containsKey("id")) {
                        Object rawId = documentData.get("id");
                        if (rawId != null && !rawId.toString().trim().isEmpty()) {
                            String className = item.getClass().getSimpleName().toLowerCase();
                            String newIdKey = className + "Id";
                            documentData.put(newIdKey, rawId);
                        }
                        documentData.remove("id");
                    }
                    
                    // ملاحظة: لا نضيف metadata لأن Appwrite يتطلب تعريف الحقول مسبقاً
                    // يجب إضافة الحقول من لوحة التحكم Appwrite Console
                    
                    boolean saved = saveDocument(tableName, actualCollectionId, documentId, documentData);
                    if (saved) {
                        savedItems.add(item);
                        successCount++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "saveData item exception", e);
                }
            }
            
            if (successCount > 0) {
                String message = "تم حفظ " + successCount + " عنصر بنجاح من أصل " + dataList.size();
                return new OperationResult<>(true, message, savedItems);
            } else {
                String detail = lastSaveDocumentError;
                String msg = (detail != null && !detail.isEmpty())
                        ? detail
                        : "فشل في حفظ جميع العناصر";
                return new OperationResult<>(false, msg);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في حفظ البيانات: " + e.getMessage());
        }
    }
    
    /**
     * جلب البيانات من جدول محدد
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @param classType نوع الكلاس المطلوب تحويل البيانات إليه
     * @return نتيجة العملية مع قائمة البيانات
     * 
     * مثال على الاستخدام:
     * // جلب المنتجات
     * OperationResult<ArrayList<Product>> result = dal.getData("products", null, Product.class);
     * 
     * if (result.success) {
     *     ArrayList<Product> products = result.data;
     *     Log.d("PRODUCTS", "تم جلب " + products.size() + " منتج");
     *     
     *     for (Product product : products) {
     *         Log.d("PRODUCT", "الاسم: " + product.name + ", السعر: " + product.price);
     *     }
     * } else {
     *     Log.e("ERROR", "فشل جلب البيانات: " + result.message);
     * }
     */
    public <T> OperationResult<ArrayList<T>> getData(String tableName, String collectionId, Class<T> classType) {
        try {
            // التحقق من وجود الجدول
            if (!tableExists(tableName, collectionId)) {
                return new OperationResult<>(false, "الجدول غير موجود: " + tableName);
            }
            
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                            (collectionId != null ? collectionId : tableName) + "/documents");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // تحليل الاستجابة وتحويلها لكائنات Java
                ArrayList<T> items = parseDocumentResponse(response.toString(), classType);
                
                return new OperationResult<>(true, "تم جلب البيانات بنجاح", items);
                
            } else {
                String errorMessage = readErrorResponse(connection);
                return new OperationResult<>(false, "فشل جلب البيانات: " + errorMessage);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في جلب البيانات: " + e.getMessage());
        }
    }
    
    /**
     * تحديث كائن موجود في قاعدة البيانات
     * @param data البيانات المحدثة
     * @param tableName اسم الجدول
     * @param documentId معرف المستند المراد تحديثه
     * @param collectionId معرف المجموعة (اختياري)
     * @return نتيجة العملية مع البيانات المحدثة
     * 
     * مثال على الاستخدام:
     * Product product = existingProduct; // منتج موجود
     * product.price = 799.99; // تحديث السعر
     * 
     * OperationResult<Product> result = dal.updateData(product, "products", 
     *                                                  "document-id-here", null);
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم تحديث المنتج بنجاح");
     *     Log.d("NEW_PRICE", "السعر الجديد: " + result.data.price);
     * } else {
     *     Log.e("ERROR", "فشل تحديث المنتج: " + result.message);
     * }
     */
    public <T> OperationResult<T> updateData(T data, String tableName, String documentId, String collectionId) {
        try {
            if (data == null || documentId == null || documentId.isEmpty()) {
                return new OperationResult<>(false, "البيانات أو معرف المستند لا يمكن أن يكون فارغاً");
            }
            
            Map<String, Object> documentData = convertObjectToMap(data);
            documentData.entrySet().removeIf(e ->
                    e.getKey() != null && e.getKey().startsWith("$"));
            // לא מוסיפים updatedAt/updatedBy כברירת מחדל — אוספים ב-Appwrite ללא השדות האלה נדחים ב-PATCH.

            String patchErr = patchDocument(tableName, collectionId, documentId, documentData);
            if (patchErr == null) {
                return new OperationResult<>(true, "تم التحديث بنجاح", data);
            }
            return new OperationResult<>(false, "فشل في تحديث المستند: " + patchErr);
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في تحديث البيانات: " + e.getMessage());
        }
    }
    
    /**
     * حذف مستند من قاعدة البيانات
     * @param tableName اسم الجدول
     * @param documentId معرف المستند المراد حذفه
     * @param collectionId معرف المجموعة (اختياري)
     * @return نتيجة العملية
     * 
     * تحذير: هذه العملية نهائية ولا يمكن التراجع عنها
     * 
     * مثال على الاستخدام:
     * OperationResult<Void> result = dal.deleteData("products", "document-id-here", null);
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم حذف المنتج بنجاح");
     * } else {
     *     Log.e("ERROR", "فشل حذف المنتج: " + result.message);
     * }
     */
    public OperationResult<Void> deleteData(String tableName, String documentId, String collectionId) {
        try {
            if (documentId == null || documentId.isEmpty()) {
                return new OperationResult<>(false, "معرف المستند لا يمكن أن يكون فارغاً");
            }
            
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                            (collectionId != null ? collectionId : tableName) + "/documents/" + documentId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200 || responseCode == 204) {
                return new OperationResult<>(true, "تم الحذف بنجاح");
            } else {
                String errorMessage = readErrorResponse(connection);
                return new OperationResult<>(false, "فشل حذف المستند: " + errorMessage);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في حذف البيانات: " + e.getMessage());
        }
    }
    
    /**
     * جلب عنصر واحد من قاعدة البيانات بواسطة معرفه
     * @param tableName اسم الجدول
     * @param documentId معرف المستند
     * @param collectionId معرف المجموعة (اختياري)
     * @param classType نوع الكلاس المطلوب
     * @return نتيجة العملية مع العنصر المحدد
     * 
     * مثال على الاستخدام:
     * OperationResult<Product> result = dal.getDataById("products", "document-id-here", null, Product.class);
     * 
     * if (result.success) {
     *     Product product = result.data;
     *     Log.d("PRODUCT", "الاسم: " + product.name + ", السعر: " + product.price);
     * } else {
     *     Log.e("ERROR", "فشل جلب المنتج: " + result.message);
     * }
     */
    public <T> OperationResult<T> getDataById(String tableName, String documentId, String collectionId, Class<T> classType) {
        try {
            if (documentId == null || documentId.isEmpty()) {
                return new OperationResult<>(false, "معرف المستند لا يمكن أن يكون فارغاً");
            }
            
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                            (collectionId != null ? collectionId : tableName) + "/documents/" + documentId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                JsonObject root = gson.fromJson(response.toString(), JsonObject.class);
                JsonObject payload = flattenAppwriteDocument(root);
                T item = gson.fromJson(payload, classType);
                
                if (item != null) {
                    return new OperationResult<>(true, "تم جلب العنصر بنجاح", item);
                } else {
                    return new OperationResult<>(false, "فشل في تحويل البيانات");
                }
                
            } else {
                String errorMessage = readErrorResponse(connection);
                return new OperationResult<>(false, "فشل جلب العنصر: " + errorMessage);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في جلب العنصر: " + e.getMessage());
        }
    }
    
    /**
     * إنشاء جدول جديد في قاعدة البيانات
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @param schema هيكل الجدول (اختياري)
     * @return true إذا تم الإنشاء بنجاح، false إذا فشل
     * 
     * مثال على الاستخدام:
     * boolean created = dal.createTable("products", null, 
     *                                   "name:string,price:number,description:text");
     * 
     * if (created) {
     *     Log.d("SUCCESS", "تم إنشاء جدول المنتجات بنجاح");
     * } else {
     *     Log.e("ERROR", "فشل في إنشاء جدول المنتجات");
     * }
     */
    public boolean createTable(String tableName, String collectionId, String schema) {
        try {
            String actualCollectionId = collectionId != null ? collectionId : tableName;
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            
            // إعداد بيانات الجدول (Collection) بشكل صحيح لـ Appwrite
            Map<String, Object> collectionData = new HashMap<>();
            collectionData.put("collectionId", actualCollectionId);
            collectionData.put("name", tableName);
            collectionData.put("enabled", true); // تفعيل المجموعة
            
            String jsonBody = gson.toJson(collectionData);
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }
            
            int responseCode = connection.getResponseCode();
            String response = "";
            if (responseCode >= 200 && responseCode < 300) {
                // قراءة الاستجابة الناجحة
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();
                response = responseBuilder.toString();
                
                // تفعيل إنشاء schema attributes
                if (schema != null && !schema.isEmpty()) {
                    createTableAttributes(actualCollectionId, schema, tableName);
                    
                    // انتظار 3 ثواني لتفعيل attributes في Appwrite
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }
                } else {
                    // إضافة schema افتراضي للملابس
                    if (tableName.toLowerCase().contains("clothes")) {
                        createDefaultClothesSchema(actualCollectionId, tableName);
                    }
                }
                
                return true;
            } else {
                String errorResponse = readErrorResponse(connection);
                if (responseCode == HttpURLConnection.HTTP_CONFLICT
                        || (errorResponse != null && (errorResponse.contains("already exists")
                        || errorResponse.contains("Collection with the requested ID already exists")))) {
                    Log.w(TAG, "createTable: collection already exists: " + actualCollectionId);
                    return true;
                }
                Log.e(TAG, "createTable failed " + actualCollectionId + " HTTP " + responseCode
                        + " " + truncateForLog(errorResponse));
                return false;
            }
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * בדיקת קיום אוסף ישירות (Appwrite 1.9+) — אמין יותר מרשימת collections שעשויה להיות מקוצרת/מדורגת.
     *
     * @return קוד HTTP (200 קיים, 404 אין), או שלילי בשגיאת רשת
     */
    private int getCollectionHttpResponseCode(@Nullable String collectionId) {
        if (collectionId == null || collectionId.isEmpty()) {
            return -1;
        }
        try {
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + collectionId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            return connection.getResponseCode();
        } catch (Exception e) {
            Log.w(TAG, "getCollectionHttpResponseCode: " + collectionId, e);
            return -1;
        }
    }

    /**
     * فحص وجود جدول في قاعدة البيانات
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @return true إذا كان الجدول موجوداً، false إذا لم يكن كذلك
     */
    public boolean tableExists(String tableName, String collectionId) {
        try {
            String expectedCollectionId = collectionId != null ? collectionId : tableName;
            int direct = getCollectionHttpResponseCode(expectedCollectionId);
            if (direct == HttpURLConnection.HTTP_OK) {
                return true;
            }

            // محاولة الوصول إلى جداول قاعدة البيانات
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // قراءة قائمة الجداول الموجودة
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String responseString = response.toString();
                
                // البحث عن الجدول في الاستجابة
                
                // إصلاح البحث الدقيق - البحث عن المطابقة التامة في $id أو name
                // البحث عن "$id\":\"expectedCollectionId\" أو "\"name\":\"expectedCollectionId\""
                boolean found = false;
                String searchPatternId = "\"$id\":\"" + expectedCollectionId + "\"";
                String searchPatternName = "\"name\":\"" + expectedCollectionId + "\"";
                
                if (responseString.contains(searchPatternId) || responseString.contains(searchPatternName)) {
                    found = true;
                } else {
                    // في حالة فشل المطابقة الدقيقة، نبحث عن تطابق جزئي آمن
                    // نتحقق من أن الاسم الصحيح موجود في قائمة collections
                    String collectionsPattern = "\"collections\":[";
                    int collectionsIndex = responseString.indexOf(collectionsPattern);
                    
                    if (collectionsIndex != -1) {
                        String collectionsContent = responseString.substring(collectionsIndex);
                        
                        // استخراج جميع IDs والأسماء
                        if (collectionsContent.contains("\"" + expectedCollectionId + "\"")) {
                            found = true;
                        } else {
                            // محاولة أخيرة: البحث عن جداول مماثلة للملابس
                            // إذا كان البحث عن "clothes" وجدنا "clothes2"، نعتبره تطابق
                            if (expectedCollectionId.equals("clothes") && collectionsContent.contains("clothes2")) {
                                found = true;
                            } else {
                                found = false;
                            }
                        }
                    } else {
                        found = false;
                    }
                }
                
                return found;
            } else {
                return false;
            }
            
        } catch (Exception e) {
            // في حالة الخطأ، نفترض أن الجدول موجود لتجنب إعادة إنشائه
            return true;
        }
    }
    
    // === دوال مساعدة لقواعد البيانات ===
    
    /**
     * التأكد من وجود الجدول وإنشاؤه إذا لم يكن موجوداً
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @param schema تعريف schema للجدول (اختياري)
     * @return true إذا كان موجوداً أو تم إنشاؤه بنجاح
     */
    private boolean ensureTableExists(String tableName, String collectionId, String schema) {
        String expectedName = collectionId != null ? collectionId : tableName;
        
        // أولاً، تأكد من وجود الجدول
        if (tableExists(tableName, collectionId)) {
            return true;
        } else {
            return createTable(tableName, collectionId, schema);
        }
    }
    
    /**
     * التأكد من وجود الجدول وإنشاؤه إذا لم يكن موجوداً (signature قديم للتوافق)
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @return true إذا كان موجوداً أو تم إنشاؤه بنجاح
     */
    private boolean ensureTableExists(String tableName, String collectionId) {
        return ensureTableExists(tableName, collectionId, null);
    }
    
    /**
     * حفظ مستند في قاعدة البيانات
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @param documentId معرف المستند
     * @param documentData بيانات المستند
     * @return true إذا تم الحفظ بنجاح، false إذا فشل
     */
    /** @return null אם הצליח, אחרת טקסט שגיאה מהשרת */
    private String patchDocument(String tableName, String collectionId, String documentId,
            Map<String, Object> documentData) {
        try {
            String cid = collectionId != null ? collectionId : tableName;
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + cid
                    + "/documents/" + documentId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PATCH");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);

            Map<String, Object> cleanData = new HashMap<>();
            for (Map.Entry<String, Object> entry : documentData.entrySet()) {
                String key = entry.getKey();
                if (key.equals("metadata") || key.equals("documentId") || key.equals("id")) {
                    continue;
                }
                cleanData.put(key, entry.getValue());
            }
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("data", cleanData);
            String jsonBody = gson.toJson(requestBody);
            byte[] payload = jsonBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload);
            }
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return null;
            }
            String err = readErrorResponse(connection);
            Log.e(TAG, "PATCH " + cid + "/" + documentId + " HTTP " + responseCode
                    + " body=" + truncateForLog(err));
            return err != null && !err.isEmpty() ? err : ("HTTP " + responseCode);
        } catch (Exception e) {
            Log.e(TAG, "patchDocument exception", e);
            return e.getMessage() != null ? e.getMessage() : "patch exception";
        }
    }

    private boolean saveDocument(String tableName, String collectionId, String documentId, Map<String, Object> documentData) {
        lastSaveDocumentError = null;
        String cid = collectionId != null ? collectionId : tableName;
        try {
            Map<String, Object> cleanData = new HashMap<>();
            for (Map.Entry<String, Object> entry : documentData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key.equals("metadata") || key.equals("documentId")
                        || key.equals("createdAt") || key.equals("createdBy")
                        || key.equals("class") || key.equals("$") || key.equals("id")) {
                    continue;
                }
                cleanData.put(key, value);
            }
            cleanData.entrySet().removeIf(e -> e.getValue() == null);

            String err = postCreateDocument(cid, documentId, cleanData);
            if (err == null) {
                return true;
            }
            lastSaveDocumentError = err;
            Log.e(TAG, "saveDocument POST " + cid + " fail body=" + truncateForLog(err));

            if (shouldRetryPostWithoutSchema(err)) {
                if (saveWithoutSchemaValidation(tableName, collectionId, documentId, new HashMap<>(cleanData))) {
                    lastSaveDocumentError = null;
                    return true;
                }
            }

            String workErr = err;
            for (int stripPass = 0; stripPass < 8 && workErr != null; stripPass++) {
                String attr = extractUnknownAttributeName(workErr);
                if (attr == null || !cleanData.containsKey(attr)) {
                    break;
                }
                Log.w(TAG, "saveDocument strip unknown attribute: " + attr);
                cleanData.remove(attr);
                workErr = postCreateDocument(cid, documentId, cleanData);
                if (workErr == null) {
                    lastSaveDocumentError = null;
                    return true;
                }
                lastSaveDocumentError = workErr;
                Log.e(TAG, "saveDocument POST after strip body=" + truncateForLog(workErr));
                if (shouldRetryPostWithoutSchema(workErr)) {
                    if (saveWithoutSchemaValidation(tableName, collectionId, documentId, new HashMap<>(cleanData))) {
                        lastSaveDocumentError = null;
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            lastSaveDocumentError = e.getMessage() != null ? e.getMessage() : "save exception";
            Log.e(TAG, "saveDocument exception", e);
            return false;
        }
    }

    /** @return null אם הצליח, אחרת גוף השגיאה מ-Appwrite */
    @Nullable
    private String postCreateDocument(String collectionId, String documentId, Map<String, Object> cleanData) {
        try {
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/"
                    + collectionId + "/documents");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("documentId", documentId);
            requestBody.put("data", cleanData);
            String jsonBody = gson.toJson(requestBody);
            byte[] payload = jsonBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return null;
            }
            String errorResponse = readErrorResponse(connection);
            return errorResponse != null && !errorResponse.isEmpty()
                    ? errorResponse
                    : ("HTTP " + responseCode);
        } catch (Exception e) {
            Log.e(TAG, "postCreateDocument", e);
            return e.getMessage() != null ? e.getMessage() : "post exception";
        }
    }
    
    /**
     * حفظ البيانات بدون schema validation نهائياً
     * 
     * ملاحظة: هذا حل مؤقت للمشكلة:
     * - Appwrite schema attributes API endpoints تعطي خطأ 404 في v1.8.0
     * - الجداول الموجودة فارغة من schema attributes
     * - الحل الأمثل: إنشاء schema attributes يدوياً من Appwrite dashboard
     * 
     * Schema attributes المطلوبة لـ clothes2:
     * - name: string (required)
     * - price: double (required) 
     * - dateAdded: datetime (required)
     * - imageUrl: string (optional)
     */
    private boolean saveWithoutSchemaValidation(String tableName, String collectionId, String documentId, Map<String, Object> documentData) {
        try {
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                            (collectionId != null ? collectionId : tableName) + "/documents");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            
            // تنظيف إضافي: إزالة أي حقول غير مطلوبة
            Map<String, Object> cleanData = new HashMap<>(documentData);
            cleanData.remove("metadata");
            cleanData.remove("documentId");
            cleanData.remove("createdAt");
            cleanData.remove("createdBy");
            
            // طلب JSON مبسط جداً - يجب تضمين documentId
            Map<String, Object> simpleRequest = new HashMap<>();
            simpleRequest.put("documentId", documentId); // إضافة documentId
            simpleRequest.put("data", cleanData);
            
            String jsonBody = gson.toJson(simpleRequest);
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                return true;
            } else {
                String errorResponse = readErrorResponse(connection);
                lastSaveDocumentError = errorResponse;
                Log.e(TAG, "saveWithoutSchemaValidation fail HTTP " + responseCode
                        + " body=" + truncateForLog(errorResponse));
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "saveWithoutSchemaValidation exception", e);
            return false;
        }
    }
    
    /**
     * تحويل كائن Java إلى Map
     * @param obj الكائن المراد تحويله
     * @return Map يحتوي على بيانات الكائن
     */
    private Map<String, Object> convertObjectToMap(Object obj) {
        try {
            String json = gson.toJson(obj);
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    
    /**
     * الحصول على معرف الكائن (ID)
     * @param obj الكائن المراد فحصه
     * @return معرف الكائن أو null إذا لم يكن موجوداً
     */
    private String getObjectId(Object obj) {
        try {
            Map<String, Object> map = convertObjectToMap(obj);
            Object id = map.get("id");
            if (id == null) {
                id = map.get("$id");
            }
            if (id == null) {
                id = map.get("userId");
            }
            if (id == null) {
                id = map.get("documentId");
            }
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * تحليل استجابة جلب المستندات وتحويلها لكائنات Java
     * @param responseString استجابة JSON
     * @param classType نوع الكلاس المطلوب
     * @return قائمة الكائنات
     */
    private JsonObject flattenAppwriteDocument(JsonObject documentJson) {
        if (documentJson == null) {
            return null;
        }
        if (documentJson.has("data") && documentJson.get("data").isJsonObject()) {
            JsonObject payload = documentJson.getAsJsonObject("data").deepCopy();
            for (String meta : new String[]{"$id", "$collectionId", "$databaseId", "$createdAt", "$updatedAt", "$permissions"}) {
                if (documentJson.has(meta)) {
                    payload.add(meta, documentJson.get(meta));
                }
            }
            return payload;
        }
        return documentJson;
    }

    private <T> ArrayList<T> parseDocumentResponse(String responseString, Class<T> classType) {
        try {
            ArrayList<T> items = new ArrayList<>();
            
            // استخدام Gson لتحليل الاستجابة بشكل صحيح
            JsonObject jsonResponse = gson.fromJson(responseString, JsonObject.class);
            
            if (jsonResponse != null && jsonResponse.has("documents")) {
                JsonArray documentsArray = jsonResponse.getAsJsonArray("documents");
                
                for (int i = 0; i < documentsArray.size(); i++) {
                    try {
                        JsonObject documentJson = documentsArray.get(i).getAsJsonObject();
                        JsonObject payload = flattenAppwriteDocument(documentJson);
                        
                        // تحويل المستند إلى كائن Java
                        T item = gson.fromJson(payload, classType);
                        
                        if (item != null) {
                            items.add(item);
                        }
                    } catch (Exception e) {
                        // خطأ في تحليل مستند
                    }
                }
            }
            
            return items;
        } catch (Exception e) {
            // خطأ في تحليل الاستجابة
            return new ArrayList<>();
        }
    }
    
    // === دوال إدارة التخزين والملفات ===
    
    /**
     * رفع ملف إلى التخزين وحفظ معلوماته في جدول خاص
     * @param fileData بيانات الملف (byte array)
     * @param fileName اسم الملف
     * @param mimeType نوع الملف (مثل "image/jpeg", "application/pdf")
     * @param bucketId معرف التخزين (اختياري، يستخدم التخزين الرئيسي إذا null)
     * @return نتيجة العملية مع معلومات الملف المرفوع
     * 
     * مثال على الاستخدام:
     * // رفع صورة
     * byte[] imageData = getImageBytesFromCamera(); // الحصول على بيانات الصورة
     * 
     * OperationResult<FileInfo> result = dal.uploadFile(imageData, "photo.jpg", "image/jpeg", null);
     * 
     * if (result.success) {
     *     FileInfo fileInfo = result.data;
     *     Log.d("SUCCESS", "تم رفع الملف بنجاح");
     *     Log.d("FILE_URL", "رابط الملف: " + fileInfo.fileUrl);
     *     Log.d("FILE_ID", "معرف الملف: " + fileInfo.fileId);
     * } else {
     *     Log.e("ERROR", "فشل رفع الملف: " + result.message);
     * }
     * 
     * // رفع مستند PDF
     * byte[] pdfData = getPdfBytes();
     * OperationResult<FileInfo> pdfResult = dal.uploadFile(pdfData, "document.pdf", "application/pdf", null);
     */
    public OperationResult<FileInfo> uploadFile(byte[] fileData, String fileName, String mimeType, String bucketId) {
        try {
            // التحقق من صحة البيانات
            if (fileData == null || fileData.length == 0) {
                return new OperationResult<>(false, "بيانات الملف لا يمكن أن تكون فارغة");
            }
            
            if (fileName == null || fileName.isEmpty()) {
                return new OperationResult<>(false, "اسم الملف لا يمكن أن يكون فارغاً");
            }
            
            // استخدام التخزين الرئيسي إذا لم يتم تحديد تخزين آخر
            String actualBucketId = bucketId != null ? bucketId : MAIN_STORAGE_BUCKET_ID;
            
            // إنشاء معرف فريد للملف
            String fileId = UUID.randomUUID().toString();
            
            // إعداد طلب رفع الملف
            URL url = new URL(BASE_URL + "/storage/buckets/" + actualBucketId + "/files");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(30000); // 30 ثانية للملفات الكبيرة
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);
            
            // إنشاء multipart form data
            String boundary = "----WebKitFormBoundary";
            
            StringBuilder formData = new StringBuilder();
            formData.append("--").append(boundary).append("\r\n");
            formData.append("Content-Disposition: form-data; name=\"fileId\"\r\n\r\n");
            formData.append(fileId).append("\r\n");
            
            formData.append("--").append(boundary).append("\r\n");
            formData.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
            formData.append("Content-Type: ").append(mimeType != null ? mimeType : "application/octet-stream").append("\r\n\r\n");
            
            byte[] headerBytes = formData.toString().getBytes("utf-8");
            byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes("utf-8");
            
            // كتابة البيانات
            try (OutputStream os = connection.getOutputStream()) {
                os.write(headerBytes);
                os.write(fileData);
                os.write(footerBytes);
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 201 || responseCode == 200) {
                // قراءة الاستجابة
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // تحليل الاستجابة وإنشاء معلومات الملف
                FileInfo fileInfo = parseFileResponse(response.toString(), fileName, mimeType, actualBucketId, fileId);
                
                return new OperationResult<>(true, "تم رفع الملف بنجاح", fileInfo);
                
            } else {
                String errorMessage = readErrorResponse(connection);
                return new OperationResult<>(false, "فشل رفع الملف: " + errorMessage);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في رفع الملف: " + e.getMessage());
        }
    }
    
    /**
     * جلب قائمة بجميع الملفات في تخزين محدد
     * @param bucketId معرف التخزين (اختياري، يستخدم التخزين الرئيسي إذا null)
     * @return نتيجة العملية مع قائمة الملفات
     * 
     * مثال على الاستخدام:
     * OperationResult<ArrayList<FileInfo>> result = dal.getStorageFiles(null);
     * 
     * if (result.success) {
     *     ArrayList<FileInfo> files = result.data;
     *     Log.d("FILES", "عدد الملفات: " + files.size());
     *     
     *     for (FileInfo file : files) {
     *         Log.d("FILE", "الاسم: " + file.fileName);
     *         Log.d("FILE", "النوع: " + file.mimeType);
     *         Log.d("FILE", "الحجم: " + (file.fileSize / 1024) + " KB");
     *     }
     * } else {
     *     Log.e("ERROR", "فشل جلب الملفات: " + result.message);
     * }
     */
    public OperationResult<ArrayList<FileInfo>> getStorageFiles(String bucketId) {
        try {
            String actualBucketId = bucketId != null ? bucketId : MAIN_STORAGE_BUCKET_ID;
            
            URL url = new URL(BASE_URL + "/storage/buckets/" + actualBucketId + "/files");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // تحليل الاستجابة
                ArrayList<FileInfo> files = parseStorageFilesResponse(response.toString(), actualBucketId);
                
                return new OperationResult<>(true, "تم جلب الملفات بنجاح", files);
                
            } else {
                String errorMessage = readErrorResponse(connection);
                return new OperationResult<>(false, "فشل جلب الملفات: " + errorMessage);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في جلب الملفات: " + e.getMessage());
        }
    }
    
    /**
     * حذف ملف من التخزين
     * @param fileId معرف الملف المراد حذفه
     * @param bucketId معرف التخزين (اختياري، يستخدم التخزين الرئيسي إذا null)
     * @return نتيجة العملية
     * 
     * تحذير: هذه العملية نهائية ولا يمكن التراجع عنها
     * 
     * مثال على الاستخدام:
     * OperationResult<Void> result = dal.deleteFile("file-id-here", null);
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم حذف الملف بنجاح");
     * } else {
     *     Log.e("ERROR", "فشل حذف الملف: " + result.message);
     * }
     */
    public OperationResult<Void> deleteFile(String fileId, String bucketId) {
        try {
            if (fileId == null || fileId.isEmpty()) {
                return new OperationResult<>(false, "معرف الملف لا يمكن أن يكون فارغاً");
            }
            
            String actualBucketId = bucketId != null ? bucketId : MAIN_STORAGE_BUCKET_ID;
            
            URL url = new URL(BASE_URL + "/storage/buckets/" + actualBucketId + "/files/" + fileId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200 || responseCode == 204) {
                return new OperationResult<>(true, "تم حذف الملف بنجاح");
            } else {
                String errorMessage = readErrorResponse(connection);
                return new OperationResult<>(false, "فشل حذف الملف: " + errorMessage);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في حذف الملف: " + e.getMessage());
        }
    }
    
    /**
     * جلب معلومات ملف محدد بواسطة معرفه
     * @param fileId معرف الملف
     * @param bucketId معرف التخزين (اختياري، يستخدم التخزين الرئيسي إذا null)
     * @return نتيجة العملية مع معلومات الملف
     * 
     * مثال على الاستخدام:
     * OperationResult<FileInfo> result = dal.getFileInfo("file-id-here", null);
     * 
     * if (result.success) {
     *     FileInfo fileInfo = result.data;
     *     Log.d("FILE", "الاسم: " + fileInfo.fileName);
     *     Log.d("FILE", "الرابط: " + fileInfo.fileUrl);
     *     Log.d("FILE", "الحجم: " + fileInfo.fileSize + " بايت");
     * } else {
     *     Log.e("ERROR", "فشل جلب معلومات الملف: " + result.message);
     * }
     */
    public OperationResult<FileInfo> getFileInfo(String fileId, String bucketId) {
        try {
            if (fileId == null || fileId.isEmpty()) {
                return new OperationResult<>(false, "معرف الملف لا يمكن أن يكون فارغاً");
            }
            
            String actualBucketId = bucketId != null ? bucketId : MAIN_STORAGE_BUCKET_ID;
            
            URL url = new URL(BASE_URL + "/storage/buckets/" + actualBucketId + "/files/" + fileId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // تحليل الاستجابة
                FileInfo fileInfo = parseSingleFileResponse(response.toString(), actualBucketId);
                
                if (fileInfo != null) {
                    return new OperationResult<>(true, "تم جلب معلومات الملف بنجاح", fileInfo);
                } else {
                    return new OperationResult<>(false, "فشل في تحليل بيانات الملف");
                }
                
            } else {
                String errorMessage = readErrorResponse(connection);
                return new OperationResult<>(false, "فشل جلب معلومات الملف: " + errorMessage);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في جلب معلومات الملف: " + e.getMessage());
        }
    }
    
    /**
     * تحديث ملف موجود في التخزين
     * @param fileId معرف الملف المراد تحديثه
     * @param newFileData بيانات الملف الجديدة
     * @param newFileName الاسم الجديد للملف (اختياري)
     * @param newMimeType النوع الجديد للملف (اختياري)
     * @param bucketId معرف التخزين (اختياري)
     * @return نتيجة العملية مع معلومات الملف المحدث
     * 
     * مثال على الاستخدام:
     * // تحديث الصورة
     * byte[] newImageData = getNewImageBytes();
     * 
     * OperationResult<FileInfo> result = dal.updateFile("existing-file-id", 
     *                                                   newImageData, 
     *                                                   "updated_photo.jpg", 
     *                                                   "image/jpeg", 
     *                                                   null);
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم تحديث الملف بنجاح");
     *     Log.d("NEW_NAME", "الاسم الجديد: " + result.data.fileName);
     * } else {
     *     Log.e("ERROR", "فشل تحديث الملف: " + result.message);
     * }
     */
    public OperationResult<FileInfo> updateFile(String fileId, byte[] newFileData, String newFileName, String newMimeType, String bucketId) {
        try {
            if (fileId == null || fileId.isEmpty()) {
                return new OperationResult<>(false, "معرف الملف لا يمكن أن يكون فارغاً");
            }
            
            if (newFileData == null || newFileData.length == 0) {
                return new OperationResult<>(false, "بيانات الملف الجديدة لا يمكن أن تكون فارغة");
            }
            
            String actualBucketId = bucketId != null ? bucketId : MAIN_STORAGE_BUCKET_ID;
            
            // حذف الملف القديم
            OperationResult<Void> deleteResult = deleteFile(fileId, actualBucketId);
            if (!deleteResult.success) {
                return new OperationResult<>(false, "فشل في حذف الملف القديم: " + deleteResult.message);
            }
            
            // رفع الملف الجديد
            String finalFileName = newFileName != null ? newFileName : "updated_" + System.currentTimeMillis() + ".bin";
            String finalMimeType = newMimeType != null ? newMimeType : "application/octet-stream";
            
            OperationResult<FileInfo> uploadResult = uploadFile(newFileData, finalFileName, finalMimeType, actualBucketId);
            
            if (uploadResult.success) {
                return new OperationResult<>(true, "تم تحديث الملف بنجاح", uploadResult.data);
            } else {
                return new OperationResult<>(false, "فشل في رفع الملف الجديد: " + uploadResult.message);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في تحديث الملف: " + e.getMessage());
        }
    }
    
    // === دوال مساعدة للتخزين ===
    
    /**
     * تحليل استجابة رفع الملف
     * @param responseString استجابة JSON
     * @param fileName اسم الملف
     * @param mimeType نوع الملف
     * @param bucketId معرف التخزين
     * @param fileId معرف الملف
     * @return معلومات الملف
     */
    private FileInfo parseFileResponse(String responseString, String fileName, String mimeType, String bucketId, String fileId) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.fileId = fileId;
        fileInfo.fileName = fileName;
        fileInfo.mimeType = mimeType;
        fileInfo.bucketId = bucketId;
        fileInfo.uploadedBy = currentUserId != null ? currentUserId : "system";
        fileInfo.uploadDate = new Date();
        
        try {
            // بناء الرابط الصحيح للملف في Appwrite Frankfurt region
            // استخدام /download مع API key في الرابط للوصول للملف
            fileInfo.fileUrl = BASE_URL + "/storage/buckets/" + bucketId + "/files/" + fileId + "/download?project=" + PROJECT_ID;
            
            // محاولة استخراج حجم الملف إذا كان متوفراً من الاستجابة
            String sizeString = extractValue(responseString, "sizeOriginal");
            if (sizeString == null || sizeString.isEmpty()) {
                sizeString = extractValue(responseString, "size");
            }
            
            if (sizeString != null && !sizeString.isEmpty()) {
                try {
                    fileInfo.fileSize = Long.parseLong(sizeString);
                } catch (NumberFormatException e) {
                    fileInfo.fileSize = 0;
                }
            }
            
        } catch (Exception e) {
            // في حالة الخطأ، استخدم الرابط الافتراضي
            fileInfo.fileUrl = BASE_URL + "/storage/buckets/" + bucketId + "/files/" + fileId + "/download?project=" + PROJECT_ID;
        }
        
        return fileInfo;
    }
    
    /**
     * تحليل استجابة جلب الملفات من التخزين
     * @param responseString استجابة JSON
     * @param bucketId معرف التخزين
     * @return قائمة الملفات
     */
    private ArrayList<FileInfo> parseStorageFilesResponse(String responseString, String bucketId) {
        ArrayList<FileInfo> files = new ArrayList<>();
        
        try {
            String filesArray = extractValue(responseString, "files");
            if (filesArray != null && !filesArray.equals("null")) {
                // تحليل الملفات
                if (filesArray.contains("{")) {
                    String[] fileObjects = filesArray.split("\\{");
                    for (String fileObj : fileObjects) {
                        if (fileObj.contains("}")) {
                            String fileContent = "{" + fileObj;
                            FileInfo fileInfo = parseSingleFileResponse(fileContent, bucketId);
                            if (fileInfo != null) {
                                files.add(fileInfo);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
        }
        
        return files;
    }
    
    /**
     * تحليل استجابة ملف واحد
     * @param responseString استجابة JSON
     * @param bucketId معرف التخزين
     * @return معلومات الملف أو null إذا فشل التحليل
     */
    private FileInfo parseSingleFileResponse(String responseString, String bucketId) {
        try {
            FileInfo fileInfo = new FileInfo();
            fileInfo.fileId = extractValue(responseString, "$id");
            fileInfo.fileName = extractValue(responseString, "name");
            fileInfo.mimeType = extractValue(responseString, "mimeType");
            fileInfo.bucketId = bucketId;
            
            if (fileInfo.fileId != null) {
                fileInfo.fileUrl = "https://cloud.appwrite.io/v1/storage/buckets/" + bucketId + "/files/" + fileInfo.fileId;
                
                // استخراج الحجم
                String sizeString = extractValue(responseString, "sizeOriginal");
                if (sizeString == null) {
                    sizeString = extractValue(responseString, "size");
                }
                
                if (sizeString != null) {
                    try {
                        fileInfo.fileSize = Long.parseLong(sizeString);
                    } catch (NumberFormatException e) {
                        fileInfo.fileSize = 0;
                    }
                }
                
                // استخراج معلومات إضافية إذا كانت متوفرة
                String uploadedBy = extractValue(responseString, "uploadedBy");
                if (uploadedBy != null) {
                    fileInfo.uploadedBy = uploadedBy;
                }
                
                return fileInfo;
            }
            
        } catch (Exception e) {
        }
        
        return null;
    }
    
    /**
     * إضافة Document permissions للجدول الجديد للسماح بالحفظ
     * @param collectionId معرف المجموعة
     * @param tableName اسم الجدول
     */
    private void addDocumentPermissions(String collectionId, String tableName) {
        try {
            // إضافة document permissions للعامة (allows read and write)
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                            collectionId + "/permissions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            
            // إعداد permissions للعامة
            Map<String, Object> permissionData = new HashMap<>();
            permissionData.put("permission", "documents.read,documents.write");
            
            String jsonBody = gson.toJson(permissionData);
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
            } else {
                String errorResponse = readErrorResponse(connection);
            }
            
        } catch (Exception e) {
        }
    }
    
    /**
     * إنشاء schema افتراضي لجدول الملابس
     * @param collectionId معرف المجموعة
     * @param tableName اسم الجدول
     */
    private void createDefaultClothesSchema(String collectionId, String tableName) {
        try {
            
            // تحديد attributes للملابس
            String[] attributes = {
                "name:string:text:نص",
                "price:number:decimal:السعر",
                "dateAdded:date:datetime:تاريخ الإضافة",
                "imageUrl:string:url:رابط الصورة"
            };
            
            for (String attribute : attributes) {
                createAttribute(collectionId, attribute, tableName);
            }
            
        } catch (Exception e) {
        }
    }
    
    /**
     * إضافة attribute واحد للجدول
     * @param collectionId معرف المجموعة
     * @param attribute تعريف الـ attribute بصيغة "name:type:required:label"
     * @param tableName اسم الجدول (للمساعدة في logging)
     */
    private void createAttribute(String collectionId, String attribute, String tableName) {
        try {
            String[] parts = attribute.split(":");
            if (parts.length < 2) {
                return;
            }
            
            String name = parts[0];
            String type = parts[1];
            boolean required = parts.length > 2 ? Boolean.parseBoolean(parts[2]) : false;
            String label = parts.length > 3 ? parts[3] : name;
            
            // تحويل أنواع البيانات لـ Appwrite
            String appwriteType;
            int size = 255; // الحجم الافتراضي للنصوص
            
            switch (type.toLowerCase()) {
                case "string":
                case "text":
                case "url":
                    appwriteType = "string";
                    size = type.equalsIgnoreCase("url") ? 500 : 255;
                    break;
                case "number":
                case "decimal":
                case "double":
                case "float":
                    appwriteType = "float"; // Appwrite uses "float" not "double"
                    break;
                case "integer":
                case "int":
                    appwriteType = "integer";
                    break;
                case "date":
                case "datetime":
                    appwriteType = "datetime";
                    break;
                case "boolean":
                    appwriteType = "boolean";
                    break;
                default:
                    appwriteType = "string";
            }
            
            // إصلاح API endpoint للـ attributes في Appwrite v1.8.0
            // الـ endpoint الصحيح: POST /databases/{databaseId}/collections/{collectionId}/attributes/{type}
            String endpoint;
            if (appwriteType.equals("string")) {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/string";
            } else if (appwriteType.equals("float")) {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/float";
            } else if (appwriteType.equals("integer")) {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/integer";
            } else if (appwriteType.equals("datetime")) {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/datetime";
            } else if (appwriteType.equals("boolean")) {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/boolean";
            } else {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/string";
            }
            
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            
            Map<String, Object> attributeData = new HashMap<>();
            attributeData.put("key", name); // اسم الحقل
            attributeData.put("required", required);
            
            // إضافة خصائص خاصة بكل نوع
            if (appwriteType.equals("string")) {
                attributeData.put("size", size);
                attributeData.put("default", null);
            } else if (appwriteType.equals("float")) {
                attributeData.put("min", null);
                attributeData.put("max", null);
                attributeData.put("default", null);
            } else if (appwriteType.equals("integer")) {
                attributeData.put("min", null);
                attributeData.put("max", null);
                attributeData.put("default", null);
            } else if (appwriteType.equals("datetime")) {
                attributeData.put("default", null);
            }
            
            String jsonBody = gson.toJson(attributeData);
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                // تم إضافة الصلاحية بنجاح
            } else {
                String errorResponse = readErrorResponse(connection);
                // فشل إضافة الصلاحية
            }
            
        } catch (Exception e) {
            // خطأ في تحليل الاستجابة
        }
    }
    
    /**
     * إضافة table attributes مخصصة
     * @param collectionId معرف المجموعة
     * @param schema تعريف schema بصيغة "name:type:required:label,name2:type2:required2:label2"
     * @param tableName اسم الجدول
     */
    private void createTableAttributes(String collectionId, String schema, String tableName) {
        try {
            String[] attributes = schema.split(",");
            for (String attribute : attributes) {
                createAttribute(collectionId, attribute.trim(), tableName);
            }
        } catch (Exception e) {
        }
    }
    
    /**
     * العثور على الـ collection ID الفعلي لجدول محدد
     * @param tableName اسم الجدول المطلوب
     * @param collectionId معرف المجموعة (اختياري)
     * @return الـ collection ID الفعلي، أو null إذا لم يتم العثور عليه
     */
    private String getActualCollectionId(String tableName, String collectionId) {
        try {
            String expectedCollectionId = collectionId != null ? collectionId : tableName;
            if (getCollectionHttpResponseCode(expectedCollectionId) == HttpURLConnection.HTTP_OK) {
                return expectedCollectionId;
            }

            // محاولة الوصول إلى جداول قاعدة البيانات
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // قراءة قائمة الجداول الموجودة
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String responseString = response.toString();
                
                // البحث الدقيق عن الجدول
                String searchPatternId = "\"$id\":\"" + expectedCollectionId + "\"";
                String searchPatternName = "\"name\":\"" + expectedCollectionId + "\"";
                
                if (responseString.contains(searchPatternId) || responseString.contains(searchPatternName)) {
                    return expectedCollectionId;
                }
                
                // البحث عن جدول موجود يحتوي على الاسم المطلوب
                String collectionsPattern = "\"collections\":[";
                int collectionsIndex = responseString.indexOf(collectionsPattern);
                
                if (collectionsIndex != -1) {
                    String collectionsContent = responseString.substring(collectionsIndex);
                    
                    // إذا وجد تطابق جزئي
                    if (collectionsContent.contains("\"" + expectedCollectionId + "\"")) {
                        
                        // استخراج الـ $id الفعلي من الاستجابة
                        String actualId = extractCollectionIdFromResponse(responseString, expectedCollectionId);
                        if (actualId != null) {
                            return actualId;
                        } else {
                            return expectedCollectionId;
                        }
                    } else {
                        // محاولة أخيرة: البحث عن جداول مماثلة للملابس
                        if (expectedCollectionId.equals("clothes") && collectionsContent.contains("clothes2")) {
                            
                            // استخراج clothes2 كـ actual ID
                            String clothes2Id = extractCollectionIdFromResponse(responseString, "clothes2");
                            if (clothes2Id != null) {
                                return clothes2Id;
                            } else {
                                return "clothes2";
                            }
                        }
                    }
                }
                
                return null;
            } else {
                return null;
            }
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * استنتاج schema من كائن تلقائياً
     * @param obj الكائن المراد استنتاج schema منه
     * @return schema بصيغة "key:type:required:label,..."
     */
    private String inferSchemaFromObject(Object obj) {
        try {
            Map<String, Object> map = convertObjectToMap(obj);
            StringBuilder schema = new StringBuilder();
            
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // تجاهل الحقول الخاصة
                if (key.equals("class") || key.equals("$") || key.equals("metadata") ||
                    key.equals("documentId") || key.equals("createdAt") || key.equals("createdBy")) {
                    continue;
                }
                
                // تحويل "id" إلى اسم فريد
                if (key.equals("id")) {
                    String className = obj.getClass().getSimpleName().toLowerCase();
                    key = className + "Id";
                }
                
                // تحديد نوع البيانات
                String type = "string"; // افتراضي
                boolean required = false; // افتراضي
                
                if (value != null) {
                    if (value instanceof String) {
                        String strValue = (String) value;
                        if (strValue.isEmpty()) {
                            required = false; // فارغ = اختياري
                        } else {
                            type = "string";
                            required = true;
                        }
                    } else if (value instanceof Integer) {
                        type = "integer";
                        required = true;
                    } else if (value instanceof Long) {
                        type = "integer";
                        required = true;
                    } else if (value instanceof Float || value instanceof Double) {
                        type = "float";
                        required = true;
                    } else if (value instanceof Boolean) {
                        type = "boolean";
                        required = true;
                    } else if (value instanceof java.util.Date) {
                        type = "datetime";
                        required = true;
                    } else {
                        type = "string";
                        required = false;
                    }
                } else {
                    // null = اختياري
                    type = "string";
                    required = false;
                }
                
                // إضافة للschema
                if (schema.length() > 0) {
                    schema.append(",");
                }
                schema.append(key).append(":").append(type).append(":").append(required).append(":").append(key);
            }
            
            return schema.toString();
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractCollectionIdFromResponse(String responseString, String expectedCollectionId) {
        try {
            // البحث عن \"name\":\"expectedCollectionId\" في الاستجابة
            String namePattern = "\"name\":\"" + expectedCollectionId + "\"";
            int nameStart = responseString.indexOf(namePattern);
            
            if (nameStart != -1) {
                // البحث عن بداية object التالي
                int objectStart = responseString.indexOf("{", nameStart);
                if (objectStart != -1) {
                    // البحث عن \"$id\":\"actualId\" في نفس الـ object
                    String idPattern = "\"$id\":\"";
                    int idStart = responseString.indexOf(idPattern, objectStart);
                    
                    if (idStart != -1) {
                        int idContentStart = idStart + idPattern.length();
                        int idEnd = responseString.indexOf("\"", idContentStart);
                        
                        if (idEnd != -1) {
                            return responseString.substring(idContentStart, idEnd);
                        }
                    }
                }
            }
            
            // طريقة بديلة: البحث عن $id مباشرة
            String directIdPattern = "\"name\":\"" + expectedCollectionId + "\",\"$id\":\"";
            int directStart = responseString.indexOf(directIdPattern);
            
            if (directStart != -1) {
                int idStart = directStart + directIdPattern.length();
                int idEnd = responseString.indexOf("\"", idStart);
                
                if (idEnd != -1) {
                    return responseString.substring(idStart, idEnd);
                }
            }
            
        } catch (Exception e) {
        }
        return null;
    }
}
