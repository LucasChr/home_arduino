package com.example.home_arduino;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button btnConexao, btnLamp1, btnLamp2;
    private static final int SOLICITA_ATIVACAO = 1;
    private static final int SOLICITA_CONEXAO = 2;
    private static final int MESSAGE_READ = 3;

    ConnectedThread threadConexao;
    Handler mHandler;
    StringBuilder dadosBluetooth = new StringBuilder();

    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothDevice mDevice = null;
    BluetoothSocket mSocket = null;
    boolean conexao;
    private static String MAC = null;

    UUID M_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConexao = (Button) findViewById(R.id.btnConexao);
        btnLamp1 = (Button) findViewById(R.id.btnLamp1);
        btnLamp2 = (Button) findViewById(R.id.btnLamp2);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "Seu dispositivo não possui bluetooth", Toast.LENGTH_LONG).show();
        } else if (!mBluetoothAdapter.isEnabled()){
            Intent ativaBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(ativaBt, SOLICITA_ATIVACAO);
        }

        btnConexao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(conexao){
                    try {
                        mSocket.close();
                        conexao = false;
                        btnConexao.setText("Conectar");
                        Toast.makeText(getApplicationContext(), "Desconectado", Toast.LENGTH_LONG).show();
                    }catch (IOException e){
                        Toast.makeText(getApplicationContext(), "Falha: " + e, Toast.LENGTH_LONG).show();
                    }
                }else{
                    Intent lista = new Intent(MainActivity.this, ListaDispositivos.class);
                    startActivityForResult(lista, SOLICITA_CONEXAO);
                }
            }
        });

        btnLamp1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(conexao){
                    threadConexao.enviar("lamp1");
                }else{
                    Toast.makeText(getApplicationContext(), "Bluetooth desconectado", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnLamp2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(conexao){
                    threadConexao.enviar("lamp2");
                }else{
                    Toast.makeText(getApplicationContext(), "Bluetooth desconectado", Toast.LENGTH_LONG).show();
                }
            }
        });

        mHandler = new Handler(){

            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what == MESSAGE_READ){
                    String recebidos  = (String) msg.obj;
                    dadosBluetooth.append(recebidos);

                    int fim = dadosBluetooth.indexOf("}");
                    if(fim > 0){
                        String dadosCompletos = dadosBluetooth.substring(0, fim);
                        int tam = dadosCompletos.length();
                        if(dadosBluetooth.charAt(0) == '{'){
                            String dadosFinais = dadosBluetooth.substring(1, tam);
                            Log.d("Recebidos", dadosFinais);
                            if(dadosFinais.contains("lamp1on")){
                                btnLamp1.setText("Lâmpada 1 ON");
                            }else{
                                btnLamp1.setText("Lâmpada 1 OFF");
                            }
                            if(dadosFinais.contains("lamp2on")){
                                btnLamp1.setText("Lâmpada 2 ON");
                            }else{
                                btnLamp1.setText("Lâmpada 2 OFF");
                            }
                        }
                        dadosBluetooth.delete(0, dadosBluetooth.length());
                    }
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SOLICITA_ATIVACAO:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "Bluetooth ativado", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth não ativado", Toast.LENGTH_LONG).show();
                    finish();
                }
            case SOLICITA_CONEXAO:
                if (resultCode == Activity.RESULT_OK) {
                    MAC = data.getExtras().getString(ListaDispositivos.ENDERECO_MAC);
                    mDevice = mBluetoothAdapter.getRemoteDevice(MAC);

                    try{
                        mSocket = mDevice.createRfcommSocketToServiceRecord(M_UUID);
                        mSocket.connect();
                        conexao = true;

                        threadConexao = new ConnectedThread(mSocket);
                        threadConexao.start();

                        btnConexao.setText("Desconectar");
                        Toast.makeText(getApplicationContext(), "Conectado", Toast.LENGTH_LONG).show();
                    }catch (IOException e){
                        conexao = false;
                        Toast.makeText(getApplicationContext(), "Falha ao conectar: " + e, Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "Falha ao obter o MAC", Toast.LENGTH_LONG).show();
                }
        }

    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                   bytes = mmInStream.read(buffer);
                   String dadosBt = new String(buffer, 0, bytes);
                   mHandler.obtainMessage(MESSAGE_READ, bytes, -1, dadosBt).sendToTarget();
                } catch (IOException e) {

                    break;
                }
            }
        }

        public void enviar(String dadosEnviar) {
            byte[] msgBuffer = dadosEnviar.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) { }
        }
    }
}