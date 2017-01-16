package se.hshn.de.pathtracker;

import android.hardware.Sensor;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by florens on 14.01.17.
 */

public class DatasetStoreService {


    public static void store(List<Measurement> dataset){
        ///storage/emulated/0
        String baseDir = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        String fileName = "Measurements.csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer = null;

        try {

            writer = new CSVWriter(new FileWriter(filePath), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] data = {"time", "acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z", "mag_x", "mag_y", "mag_z", "azimuth", "pitch", "roll"};
            writer.writeNext(data);

            for(Measurement m : dataset){
                writeCsvLine(m, writer);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(writer !=null){
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }


    private static void writeCsvLine(Measurement m, CSVWriter writer) {

        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.GERMAN);
        otherSymbols.setDecimalSeparator('.');

        String[] data = {
                Float.toString(m.getTimestamp()),
                "",
                "",
                "",
                "",
                "",
                "",
                Float.toString(m.getMagx()),
                Float.toString(m.getMagy()),
                Float.toString(m.getMagz()),
                Float.toString(m.getAzimuth()),
                Float.toString(m.getPitch()),
                Float.toString(m.getRoll())
        };
        writer.writeNext(data);


    }
}
