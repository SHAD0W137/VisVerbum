package com.example.visverbum.service;

import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class HTTPRequest implements Runnable{

    public static String api = "https://api.dictionaryapi.dev/api/v2/entries/en/"; // Language is hardcoded for now
    private Handler handler;
    private URL url;
    public HTTPRequest(Handler h, String WORD){
        this.handler = h;
        try {
            this.url = new URL("https://api.dictionaryapi.dev/api/v2/entries/en/"+WORD);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            Scanner in = new Scanner(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            while(in.hasNext()){
                response.append(in.nextLine());
            }
            in.close();
            connection.disconnect();
            Message msg = Message.obtain();
            msg.obj = response.toString();
            handler.sendMessage(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
