package com.example.home_arduino;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Set;

public class ListaDispositivos extends ListActivity {

    private BluetoothAdapter mBluetoothAdapter = null;
    static String ENDERECO_MAC = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayAdapter<String> arrayBluetooth = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> dispositivosPareados = mBluetoothAdapter.getBondedDevices();

        if(dispositivosPareados.size() > 0){
            for(BluetoothDevice d : dispositivosPareados){
                String btName = d.getName();
                String macBt = d.getAddress();
                arrayBluetooth.add(btName + "\n" + macBt);
            }
        }
        setListAdapter(arrayBluetooth);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String info = ((TextView) v).getText().toString();
        //Toast.makeText(getApplicationContext(), "Info: "+ info, Toast.LENGTH_LONG).show();
        String mac = info.substring(info.length() - 17);
        Intent retornaMac = new Intent();
        retornaMac.putExtra(ENDERECO_MAC, mac);
        setResult(RESULT_OK, retornaMac);
        finish();
    }
}
