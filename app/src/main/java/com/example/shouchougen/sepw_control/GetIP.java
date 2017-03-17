package com.example.shouchougen.sepw_control;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;


public class GetIP extends Activity {
    String ipname = null;
    String btname = null;
    int resolution = 10;
    SharedPreferences pref = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set it to full-screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        final Builder builder = new Builder(this);   // setting a alert dialog
        builder.setTitle("login dialog");                       // hint of alert dialog title

        //setup the layout
        LinearLayout loginForm = (LinearLayout)getLayoutInflater().inflate( R.layout.login, null);
        final EditText iptext = (EditText)loginForm.findViewById(R.id.ipedittext);
        final EditText bttext = (EditText)loginForm.findViewById(R.id.bttext);
        final EditText Resolution = (EditText)loginForm.findViewById(R.id.resolution);
        builder.setView(loginForm);

        pref = getSharedPreferences("pref_name", 0);
        btname = pref.getString("btname", "98:D3:31:B3:EE:2E");
        ipname = pref.getString("ipname", "192.168.1.44");
        String resolution_str = "25";

        iptext.setText(ipname);
        bttext.setText(btname);
        Resolution.setText(resolution_str);

        //create a login button
        builder.setPositiveButton("Login"
                // login listener
                , new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // seting the login information
                ipname = iptext.getText().toString().trim();
                btname = bttext.getText().toString().trim();
                //resolution_str = Resolution.getText().toString().trim();
                pref.edit().putString("btname", btname).commit();
                pref.edit().putString("ipname", ipname).commit();
                //pref.edit().putString("resolution", resolution_str).commit();

                try {
                    resolution = Integer.parseInt(Resolution.getText().toString());
                } catch(NumberFormatException nfe) {
                    //System.out.println("Could not parse " + nfe);
                }

                //ipname = data.getString("ipname");;
                Bundle data = new Bundle();
                data.putString("ipname", ipname);
                data.putString("btname", btname);
                data.putInt("resolution", resolution);
                Intent intent = new Intent(GetIP.this, MainActivity.class);
                intent.putExtras(data);
                startActivity(intent);
            }
        });
        // create a canceal button
        builder.setNegativeButton("Canceal"
                ,  new OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                //not to login
                System.exit(1);
            }
        });
        //show the dialog
        builder.create().show();
    }
}