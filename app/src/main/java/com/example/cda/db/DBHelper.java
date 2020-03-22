package com.example.cda.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.cda.entry.User;
import com.example.cda.ui.previous_alerts.Alert;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "BSAFE";
    private static final String USERS_TABLE_NAME = "USERS";
    private static final String USERS_COLUMN_ID = "ID";
    private static final String USERS_COLUMN_FIRST_NAME = "FIRST_NAME";
    private static final String USERS_COLUMN_SURNAME = "SURNAME";
    private static final String USERS_COLUMN_DATE_OF_BIRTH = "DOB";
    private static final String USERS_COLUMN_MOBILE_NUMBER = "MOBILE_NUMBER";
    private static final String USERS_COLUMN_EMERGENCY_CONTACT = "EMERGENCY_CONTACT";
    private static final String USERS_COLUMN_EMAIL = "EMAIL";
    private static final String USERS_COLUMN_PASSWORD = "PASSWORD";
    private static final String USERS_COLUMN_HEIGHT = "HEIGHT";
    private static final String USERS_COLUMN_WEIGHT = "WEIGHT";
    private static final String USERS_COLUMN_BLOOD_TYPE = "BLOOD_TYPE";
    private static final String USERS_COLUMN_BIBULOUS = "BIBULOUS";
    private static final String USERS_COLUMN_SMOKER = "SMOKER";
    private static final String USERS_COLUMN_MEDICAL_CONDITION = "MEDICAL_CONDITION";

    private static final String PREVIOUS_ALERTS_TABLE_NAME = "PREVIOUS_ALERTS";
    private static final String PREVIOUS_ALERTS_COLUMN_LATITUDE = "LATITUDE";
    private static final String PREVIOUS_ALERTS_COLUMN_LONGITUDE = "LONGITUDE";
    private static final String PREVIOUS_ALERTS_COLUMN_TIMESTAMP = "TIMESTAMP";
    private static final String PREVIOUS_ALERTS_COLUMN_SPEED = "SPEED";
    private static final String PREVIOUS_ALERTS_COLUMN_GFORCE= "GFORCE";




    public DBHelper(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + USERS_TABLE_NAME +
                        " (" + USERS_COLUMN_EMAIL + " TEXT PRIMARY KEY, " +
                        USERS_COLUMN_FIRST_NAME + " TEXT, " +
                        USERS_COLUMN_SURNAME + " TEXT, " +
                        USERS_COLUMN_DATE_OF_BIRTH + " TEXT, " +
                        USERS_COLUMN_MOBILE_NUMBER + " TEXT, " +
                        USERS_COLUMN_EMERGENCY_CONTACT + " TEXT, " +
                        USERS_COLUMN_PASSWORD + " TEXT, " +
                        USERS_COLUMN_WEIGHT + " TEXT, " +
                        USERS_COLUMN_HEIGHT + " TEXT, " +
                        USERS_COLUMN_BLOOD_TYPE + " TEXT, " +
                        USERS_COLUMN_SMOKER + " TEXT, " +
                        USERS_COLUMN_BIBULOUS + " TEXT, " +
                        USERS_COLUMN_MEDICAL_CONDITION + " TEXT)"
        );

        db.execSQL(
                "CREATE TABLE " + PREVIOUS_ALERTS_TABLE_NAME +
                        " (" + PREVIOUS_ALERTS_COLUMN_LATITUDE + " TEXT, " +
                        PREVIOUS_ALERTS_COLUMN_LONGITUDE + " TEXT, " +
                        PREVIOUS_ALERTS_COLUMN_TIMESTAMP + " TEXT, " +
                        PREVIOUS_ALERTS_COLUMN_SPEED + " TEXT, " +
                        PREVIOUS_ALERTS_COLUMN_GFORCE + " TEXT)"

        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + USERS_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PREVIOUS_ALERTS_TABLE_NAME);
        onCreate(db);
    }

    public boolean insertUser(User user){

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(USERS_COLUMN_EMAIL, user.getEmail());
        cv.put(USERS_COLUMN_FIRST_NAME, user.getFirstName());
        cv.put(USERS_COLUMN_SURNAME, user.getSurname());
        cv.put(USERS_COLUMN_DATE_OF_BIRTH, user.getDob());
        cv.put(USERS_COLUMN_MOBILE_NUMBER, user.getMobile());
        cv.put(USERS_COLUMN_EMERGENCY_CONTACT, user.getEmergency());
        cv.put(USERS_COLUMN_PASSWORD, user.getPassword());
        cv.put(USERS_COLUMN_WEIGHT, user.getWeight());
        cv.put(USERS_COLUMN_HEIGHT, user.getHeight());
        cv.put(USERS_COLUMN_BLOOD_TYPE, user.getBloodType());
        cv.put(USERS_COLUMN_SMOKER, user.getSmoker());
        cv.put(USERS_COLUMN_BIBULOUS, user.getBibulous());
        cv.put(USERS_COLUMN_MEDICAL_CONDITION, user.getMedicalCondition());
        db.insert(USERS_TABLE_NAME, null, cv);

        return true;
    }

    public User getUser(String email){
        User user = new User();
        user.setEmail(email);

        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + USERS_TABLE_NAME + " WHERE " + USERS_COLUMN_EMAIL + "=\""+email+"\";", null);

        if(c.moveToFirst()){
            user.setFirstName(c.getString(c.getColumnIndex(USERS_COLUMN_FIRST_NAME)));
            user.setSurname(c.getString(c.getColumnIndex(USERS_COLUMN_SURNAME)));
            user.setDob(c.getString(c.getColumnIndex(USERS_COLUMN_DATE_OF_BIRTH)));
            user.setMobile(c.getString(c.getColumnIndex(USERS_COLUMN_MOBILE_NUMBER)));
            user.setEmergency(c.getString(c.getColumnIndex(USERS_COLUMN_EMERGENCY_CONTACT)));
            user.setPassword(c.getString(c.getColumnIndex(USERS_COLUMN_PASSWORD)));
            user.setWeight(c.getString(c.getColumnIndex(USERS_COLUMN_WEIGHT)));
            user.setHeight(c.getString(c.getColumnIndex(USERS_COLUMN_HEIGHT)));
            user.setBloodType(c.getString(c.getColumnIndex(USERS_COLUMN_BLOOD_TYPE)));
            user.setSmoker(c.getString(c.getColumnIndex(USERS_COLUMN_SMOKER)));
            user.setBibulous(c.getString(c.getColumnIndex(USERS_COLUMN_BIBULOUS)));
            user.setMedicalCondition(c.getString(c.getColumnIndex(USERS_COLUMN_MEDICAL_CONDITION)));
        }
        return user;
    }

    public boolean valid(String email, String password){
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + USERS_TABLE_NAME + " WHERE " + USERS_COLUMN_EMAIL + "=\""+email+"\" AND " + USERS_COLUMN_PASSWORD + "=\""+password+"\";", null);
        return c.moveToFirst();
    }

    private boolean previousAlertsExist(){
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + PREVIOUS_ALERTS_TABLE_NAME + ";", null);
        boolean exist = (c.getCount() > 0);
        c.close();
        return exist;
    }

    public boolean insertAlert(Alert alert){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(PREVIOUS_ALERTS_COLUMN_LATITUDE, alert.getLatitude());
        cv.put(PREVIOUS_ALERTS_COLUMN_LONGITUDE, alert.getLongitude());
        cv.put(PREVIOUS_ALERTS_COLUMN_TIMESTAMP, alert.getTimestamp());
        cv.put(PREVIOUS_ALERTS_COLUMN_SPEED, alert.getSpeed());
        cv.put(PREVIOUS_ALERTS_COLUMN_GFORCE, alert.getGforce());

        db.insert(PREVIOUS_ALERTS_TABLE_NAME, null, cv);
        return true;
    }

    public ArrayList<Alert> getPreviousAlerts(){
        if(!previousAlertsExist()){
            return null;
        }

        ArrayList<Alert> alerts = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + PREVIOUS_ALERTS_TABLE_NAME + ";", null);
        while(c.moveToNext()){
            String latitude = c.getString(c.getColumnIndex(PREVIOUS_ALERTS_COLUMN_LATITUDE));
            String longitude = c.getString(c.getColumnIndex(PREVIOUS_ALERTS_COLUMN_LONGITUDE));
            String timestamp = c.getString(c.getColumnIndex(PREVIOUS_ALERTS_COLUMN_TIMESTAMP));
            String speed = c.getString(c.getColumnIndex(PREVIOUS_ALERTS_COLUMN_SPEED));
            String gforce = c.getString(c.getColumnIndex(PREVIOUS_ALERTS_COLUMN_GFORCE));
            alerts.add(new Alert(latitude, longitude, timestamp, speed, gforce));
        }
        return alerts;
    }
}