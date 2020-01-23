package src;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Random;

public class secureTTT{
    private static Socket socket = null;
    private static BufferedInputStream in = null;
    private static BufferedOutputStream out = null;
    private static String host = "itkomsrv.fotonik.dtu.dk";
    private static int port = 1465;
    private static byte[] b = new byte[52];
    private static byte[] b1 = new byte[16];
    private static byte[] b2 = new byte[16];
    private static byte[] b3 = new byte[16];
    private static byte[] b4 = new byte[20];
    private static byte[] b5 = new byte[18];
    private static byte[] fcs = new byte[2];
    private static BigInteger bigInt1g;
    private static BigInteger bigInt2p;
    private static BigInteger bigInt3A;
    private static BigInteger bigInt4kServer;
    private static BigInteger bigIntfcs;
    private static  Random r = new Random();

    public static void main(String[] args) throws IOException {
        startClient();

        //Læser serverens første besked.
        in.read(b,0, 52);

        //2 .. 17
        int j = 2;
        for(int i = 0; i < 16; i++) {
            b1[i] = b[j];
            j++;
        }
        bigInt1g = new BigInteger(b1);

        // 18 .. 33
        int k = 18;
        for(int i = 0; i < 16; i++) {
            b2[i] = b[k];
            k++;
        }

        bigInt2p = new BigInteger(b2);

        // 35 .. 54
        int l = 34;
        for(int i = 0; i < 16; i++) {
            b3[i] = b[l];
            l++;
        }

        bigInt3A = new BigInteger(b3);

        //FCS
        int FCS = 50;
        for(int i = 0; i < 2; i++){
            fcs[i] = b[FCS];
            FCS++;
        }
        bigIntfcs = new BigInteger(fcs);

        BigInteger big_b = new BigInteger(126,r);
        BigInteger B = difHelmanB(bigInt1g,bigInt2p,big_b);
        BigInteger Kryp = k(bigInt2p,bigInt3A,big_b);


        byte[] payload = B.toByteArray();
        payload = besked(payload);
        out.write(payload,0,20);
        out.flush();

        TicTacToe tictactoe = new TicTacToe(socket,out,in,Kryp);
        Thread t = new Thread(tictactoe);
        t.start();
    }

    private static byte[] besked(byte[] payload){
        byte[] besked = new byte[payload.length+4];

        besked[0] = 2;
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

    private static void startClient() throws IOException {
        //Opretter nyt socket samt BufferedOutputStream og BufferedInputStream.
        socket = new Socket(host, port);
        out = new BufferedOutputStream(socket.getOutputStream());
        in = new BufferedInputStream(socket.getInputStream());
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

    private static BigInteger difHelmanB(BigInteger g,BigInteger p,BigInteger b){
       return g.modPow(b,p);
    }

    private static BigInteger k(BigInteger p, BigInteger A, BigInteger b){
        return A.modPow(b,p);
    }
}
