/**
 * Created by Narvik on 5/10/17.
 */
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.io.BufferedReader;

public class UdpClient {
    public static void main ( String [] args ){

        try (Socket socket = new Socket("codebank.xyz", 38005)) {

            // Shows we are connected to the server
            System.out.println("Connected to server.");

            InputStream getIS = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            InputStreamReader readIS = new InputStreamReader(getIS, "UTF-8");
            BufferedReader serverMassage = new BufferedReader(readIS);

            byte[] IPv4packet ;
            byte[] destAddr ;
            short cs, length;
            int dataLength = 2;


            for (int k = 0 ; k < 12 ; k++){

                length = (short)(20 + dataLength);
                IPv4packet = new byte[length];
                System.out.println("Data Length: " + dataLength);

                // 0-3
                IPv4packet[0]= 0b01000101;  // Version + HLen
                IPv4packet[1]= 0;           // TOS
                IPv4packet[2]= (byte)((length & 0xFF00)>>>8);     // 1st half of length
                IPv4packet[3]= (byte)(length & 0x00FF);           // 2nd half of length

                // 4-7
                IPv4packet[4]= 0;           // 1st half of identification
                IPv4packet[5]= 0;           // 2nd half of identification
                IPv4packet[6]= 0b01000000;  // Flag
                IPv4packet[7]= 0;           // Offset

                // 8-11
                IPv4packet[8]= 50;          // TTl
                IPv4packet[9]= 6;           // Protocol
                IPv4packet[10]= 0;          // CheckSum
                IPv4packet[11]= 0;          // CheckSum

                // 12-15, Random IP
                IPv4packet[12]= (byte)192;
                IPv4packet[13]= (byte)168;
                IPv4packet[14]= (byte)1;
                IPv4packet[15]= (byte)64;

                // 16-19, Server IP
                destAddr  = socket.getInetAddress().getAddress();
                for (int i = 0,j = 16 ; i<destAddr.length; i++, j++){
                    IPv4packet[j] = destAddr[i] ;
                }

                // 20-?, Data
                for (int i = 20; i< length; i++) {
                    IPv4packet[i] = 0;
                }

                cs = checksum(IPv4packet);
                IPv4packet[10]= (byte)((cs & 0xFF00)>>>8);
                IPv4packet[11]= (byte)(cs & 0x00FF);


                // Send data to the server
                for (int i = 0; i< length; i++) {
                    outStream.write(IPv4packet[i]);
                }

                // Read respond from the server
                String massage = serverMassage.readLine();
                System.out.println(massage+"\n");

                dataLength *= 2;
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
    public static short checksum(byte[] b){
        int sum = 0, i = 0;

        while(i < b.length-1) {
            sum += ((b[i]<<8 & 0xFF00) | (b[i+1] & 0xFF));

            if((sum & 0xFFFF0000) > 0) {
                sum &= 0xFFFF;
                sum++;
            }
            i += 2;
        }

        if((b.length)%2 == 1) {
            sum += ((b[i]<<8) & 0xFF00);

            // Wrap around, carry occurred
            if((sum & 0xFFFF0000) > 0) {
                sum &= 0xFFFF;
                sum++;
            }
        }

        // Return the 1's complement
        return (short) ~(sum & 0xFFFF);
    }
}
