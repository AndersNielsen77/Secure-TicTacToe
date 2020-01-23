package src;

/*
* Gruppe: Mads Ptak, Mikkel Rahbek og Anders Nielsen
*/

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.Scanner;

public class TTT {
    public static void main(String[] args) {
        Socket MySocket = null; // Vi initilisere vores socket

        try {
            // Bestemmer sv�rhedsgrad (Hvilken server man vil forbinde til, og efterf�lgende spille med)
            Scanner player = new Scanner(System.in); // Opretter en scanner for spilleren
            System.out.println("Bestem sv�rhedsgrad!");
            System.out.println("Skriv let eller sv�r:");
            String neu = player.nextLine();
            if (neu.equals("let")) {
                System.out.println("Du har valgt let sv�rhedsgrad!");
                MySocket = new Socket("itkomsrv.fotonik.dtu.dk", 1102);
            } else if (neu.equals("sv�r")) {
                System.out.println("Du har valgt sv�r sv�rhedsgrad!");
                MySocket = new Socket("itkomsrv.fotonik.dtu.dk", 1105);
            } else {
                // Kaster fejl, hvis bruger hverken skriver "sv�r" eller "let".
                throw new IllegalArgumentException();
            }

            System.out.println("--------------------------------");


            //Spillet begynder, og vi opretter en scanner som modtager input fra serveren
            Scanner netin = new Scanner(MySocket.getInputStream());
            for (int i = 0; i < 3; i++) { //F�rste forbindelse til serveren har 3 linjer text
                String textline = netin.nextLine();
                System.out.println(textline);
            }

            // Initialisere v�rdier til while loopet.
            String textline2 = "Initialize";
            String Win = "SERVER WINS";
            String Win2 = "PLAYER WINS";
            String Win3 = "NOBODY WINS";
            int var1 = 0;

            // Spiillet gentages s� l�nge at en vinder ikke er fundet, eller at br�ttet er fyldt ud.
            while ((var1 != Win.compareTo(textline2)) && (var1 != Win2.compareTo(textline2))
                    && (var1 != Win3.compareTo(textline2))) {

                PrintWriter pw2 = new PrintWriter(MySocket.getOutputStream()); //Opretter printwriter som kan skrive til serveren
                int t = player.nextInt(); //via en scanner fort�ller brugeren hvilken felt man vil placere sin brik p�.
                pw2.print(t + "\r\n"); //Linje som skal sendes til serveren
                pw2.flush(); //Sender alt i printwriteren til serveren
                //Loop som printer de 2 linjer serveren kommunikere tilbage til spilleren
                for (int i = 0; i < 2; i++) {
                    textline2 = netin.nextLine();
                    System.out.println(textline2);
                }
            }
            try { // Lukker kommunikationen mellem client og server, n�r spillet er slut
                MySocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) { //Hvis clienten ikke kan finde nogen server
            System.out.println("could not find local address!");
        } catch (IOException e) {
            System.out.println("could not find local address!v2");
        } catch (IllegalArgumentException e) { //Hvis bruger ikke opfylder kravet om sv�rhedsgrad
            System.out.println("Ingen sv�rhedsgrad valgt");
        }
    }
}
