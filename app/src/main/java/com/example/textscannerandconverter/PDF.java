package com.example.textscannerandconverter;

import android.os.Environment;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class PDF {
    private String name, path;
    private static int lastPdfId = 0;

    public PDF(){
        this.name = "PDF" + ++lastPdfId;
        this.path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Text Scanner and Converter/";
    }

    public PDF(String name, String path){
        if(name == null){
            this.name = "PDF" + ++lastPdfId;
        }else{
            this.name = name;
        }
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getName(){
        return name;
    }

    public static ArrayList<PDF> createPDFList(int numFile){
        ArrayList<PDF> pdf = new ArrayList<PDF>();

        for (int i = 1; i <= numFile; i++){
            pdf.add(new PDF());
        }

        return pdf;
    }

    public static ArrayList<PDF> createPDFList(int numFile, String[] name, String[] filePath){
        ArrayList<PDF> pdf = new ArrayList<PDF>();

        for (int i = 1; i <= numFile; i++){
            pdf.add(new PDF(name[i - 1], filePath[i - 1]));
        }


        return pdf;
    }
}
