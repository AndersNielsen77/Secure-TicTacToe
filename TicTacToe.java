package src;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class TicTacToe implements Runnable {

    String Win = "SERVER WINS";
    String Win2 = "PLAYER WINS";
    String Win3 = "NOBODY WINS";
    int var1 = 0;
    private Socket socket;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    protected static BigInteger k;
    private static String b_print = "Initialize";

    public TicTacToe(Socket socket, BufferedOutputStream out, BufferedInputStream in, BigInteger k) {
        this.socket = socket;
        this.out = out;
        this.in = in;
        TicTacToe.k = k;
    }

    public byte[] makeByteArray(byte[] buffer) {
        if (buffer[0] == 3) {
            int size = buffer[1];
            byte[] payload = new byte[size];
            for (int i = 0; i < payload.length; i++) {
                payload[i] = buffer[i + 2];
            }
            return payload;
        }
        return buffer;
    }

    public static byte[] removeFcs(byte[] payloadFcs){
        //Fjerner FCS/kontrolsum fra payload for dekryption af beskeda server
        byte[] payload = new byte[payloadFcs.length-2];
        for (int i = 0; i < payload.length ; i++){
            payload[i] = payloadFcs[i];
        }
        return payload;
    }

    public byte[][] makeByte2DArray(byte[] buffer) {
        boolean loop = true;
        int numbOfMsg = 1;
        int multi = 0;

        if (buffer[0] == 3) {
            int size = buffer[1]+2;
                while (loop){
                    if (buffer[size] == 3) {
                    numbOfMsg = numbOfMsg + 1;
                    loop = false;
                    }else{ loop = false;}
                }
            byte[][] payload = new byte[numbOfMsg][size];

            for (int i = 0; i< numbOfMsg;i++){
                for (int j = 0; j < size;j++){
                    payload[i][j] = buffer[j+multi];
                }
                multi = size;
            }
        return payload;
    }
        return new byte[1][1];
    }

    public byte[] tobytearray(byte[][] twoArray, int row,int collum){
        boolean run= true;
        while (run) {
            if (twoArray.length > row) {
                byte[] bytearray = new byte[twoArray[row][1]];
                for (int i = 0; i < bytearray.length; i++) {
                    bytearray[i] = twoArray[row][i+2];
                }
                run = false;
                return bytearray;
            }else row = row-1;
        }
        return new byte[1];
    }

    public byte[][] opdelPayload(byte[] payload,int payloadOpdelSize){
        int multi = 0;
        int numbOf16Byte = (payload.length -2)/payloadOpdelSize;
        byte[][] opdelt = new byte[numbOf16Byte][payloadOpdelSize];

        for (int i = 0; i< numbOf16Byte;i++){
            for (int j = 0; j < payloadOpdelSize;j++){
                opdelt[i][j+multi] = payload[j];
            }
            multi = multi+payloadOpdelSize;
        }
        return opdelt;
    }

    @Override
    public void run() {
        //Start
        for(int i = 0; i < 2; i++) {
            byte[] buffer = new byte[256];
            try {
                in.read(buffer, 0, 256);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] payload = makeByteArray(buffer);
            byte[][] pay = makeByte2DArray(buffer);

            dekrypter(k, payload);
            byte[] yourturn = new byte[11];
            if( i == 1) {
                for(int l = 0; l < 11; l++) {
                    yourturn[l] = buffer[l+24];
                }
                dekrypter(k,yourturn);
            }
        }
        
        while((var1 != Win.compareTo(b_print)) && (var1 != Win2.compareTo(b_print)) && (var1 != Win3.compareTo(b_print))) {
            System.out.println("-------Kryptering-------");
            //Input
            Scanner scanner = new Scanner(System.in);
            String T = scanner.next();
            //String to BigInt
            byte[] h = T.getBytes(StandardCharsets.US_ASCII);
            h = besked(h);
            h = krypter(k, h);

            try {
                out.write(h, 0, h.length);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 0; i <2; i++) {
                byte[] buffer = new byte[256];
                try {
                    in.read(buffer, 0, 256);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[][] twoarray = makeByte2DArray(buffer);

                if (twoarray[0][0] != 0) {
                    for (int z = 0; z < twoarray.length; z++) {
                        dekrypter(k, tobytearray(twoarray, z, 1));
                    }
                }
            }
        }
        try {
            System.out.println("Spillet er slut");
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void dekrypter (BigInteger krypt, byte[] b5) {
        //Dekrypter byte array fra server
        int a = 125, c = 1;
        System.out.println("------Dekryptering------");

        byte[] k_array = krypt.toByteArray();
        byte[] X = {k_array[k_array.length-3], k_array[k_array.length-2], k_array[k_array.length-1]};
        //byte[] X2 = {0x12, 0x34, 0x56};
        BigInteger BX = new BigInteger(X);

        //Int to BigInt
        Integer Biga = Integer.valueOf(a);
        Integer Bigc = Integer.valueOf(c);
        BigInteger Xn = null;
        for (int j = 0; j < b5.length; j++) {

            //Xn+1 = (a ∙ Xn)
            Xn = BX.multiply(BigInteger.valueOf(Biga));
            //Xn+1 + c
            Xn = Xn.add(BigInteger.valueOf(Bigc));
            //mod 2^24
            Xn = Xn.mod(BigInteger.valueOf(16777216));

            //BigInt to byte array
            byte[] keybyte = Xn.toByteArray();

            if (keybyte[0] == 0) {
                byte[] tmp = new byte[keybyte.length - 1];
                System.arraycopy(keybyte, 1, tmp, 0, tmp.length);
                keybyte = tmp;
            }

            //Krypter / Dekrypter byte for byte
            b5[j] = (byte) (b5[j] ^ keybyte[1]);
            //System.out.println(b5[j]);
            BX = Xn;
        }
        byte[] b6 = removeFcs(b5);
        b_print = new String(b6, StandardCharsets.US_ASCII);
        System.out.println(b_print);
        k = Xn;
    }

    private static byte[] krypter(BigInteger krypt, byte[] b5) {
        //Krypter data sendt til server
        int a = 125, c = 1;

        byte[] k_array = krypt.toByteArray();
        byte[] X = {k_array[k_array.length-3], k_array[k_array.length-2], k_array[k_array.length-1]};
        //byte[] X2 = {0x12, 0x34, 0x56};
        BigInteger BX = new BigInteger(X);

        //Int to BigInt
        Integer Biga = Integer.valueOf(a);
        Integer Bigc = Integer.valueOf(c);
        BigInteger Xn = null;
        for (int j = 2; j < b5.length; j++) {

            //Xn+1 = (a ∙ Xn)
            Xn = BX.multiply(BigInteger.valueOf(Biga));
            //Xn+1 + c
            Xn = Xn.add(BigInteger.valueOf(Bigc));
            //mod 2^24
            Xn = Xn.mod(BigInteger.valueOf(16777216));

            //BigInt to byte array
            byte[] keybyte = Xn.toByteArray();

            if (keybyte[0] == 0) {
                byte[] tmp = new byte[keybyte.length - 1];
                System.arraycopy(keybyte, 1, tmp, 0, tmp.length);
                keybyte = tmp;
            }

            //Krypter / Dekrypter byte for byte
            b5[j] = (byte) (b5[j] ^ keybyte[1]);
            BX = Xn;
            k = Xn;
        }
        return b5;
    }

    private static byte[] besked(byte[] payload){
        byte[] besked = new byte[payload.length+4];

        besked[0] = 3;
        besked[1] = (byte) (payload.length+2);

        for (int i = 0; i < payload.length;i++) {
            besked[i + 2] = payload[i];
        }

        int kontrolSum = kontrolsum(toIntArray(besked));

        int c1 = kontrolSum >> 8;
        int c2 = kontrolSum & 0x000000FF;

        besked[payload.length+2] = (byte) c1;
        besked[payload.length+3] = (byte) c2;

        return besked;
    }

    private static int kontrolsum(int[] input){
        // Fletcher-16:
        // Input: data[] – array af N bytes i intervallet 0..255
        // Output: kontrolsum (Java-type: int)
        // sum1 og sum2 er temporære variable (Java-type: short eller int)
        int sum1 = 0;
        int sum2 = 0;
        int N = input.length-2;
        int kontrolsum = 0;

        for (int i = 0; i < N; i++) {
            sum1 = (sum1 + input[i]) % 255; // mod 255;
            sum2 = (sum2 + sum1) % 255; // mod 255;
        }
        kontrolsum = (sum2 << 8) | sum1;
        return kontrolsum;
    }

    private static int[] toIntArray(byte[] input){
        int[] data = new int[input.length];
        for (int i = 0; i < input.length; i++){
            if (input[i]<0){
                data[i]= input[i]+256;
            }else data[i] = input[i];
        }
        return data;
    }
}