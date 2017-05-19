/**
 * Created by Narvik on 5/10/17.
 */
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.io.BufferedReader;
import java.util.Random;


public class UdpClient {
    public static void main ( String [] args ){

        try (Socket socket = new Socket("codebank.xyz", 38005)) {

            // Shows we are connected to the server
            System.out.println("Connected to server.");

            InputStream getIS = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            InputStreamReader readIS = new InputStreamReader(getIS, "UTF-8");
            BufferedReader serverMassage = new BufferedReader(readIS);

            byte IPv4packet[] ,randomData[], destAddr[], port[], udp[];
            short cs, length = 24;
            int dataLength = 2;
            long startTime = 0, endTime = 0;
            float avrageTime = 0;
            byte[] handShakePacket = new byte[24];
            Random rand = new Random();


            // 0-3
            handShakePacket[0]= 0b01000101;  // Version + HLen
            handShakePacket[1]= 0;           // TOS
            handShakePacket[2]= (byte)((length & 0xFF00)>>>8);     // 1st half of length
            handShakePacket[3]= (byte)(length & 0x00FF);           // 2nd half of length

            // 4-7
            handShakePacket[4]= 0;           // 1st half of identification
            handShakePacket[5]= 0;           // 2nd half of identification
            handShakePacket[6]= 0b01000000;  // Flag
            handShakePacket[7]= 0;           // Offset

            // 8-11
            handShakePacket[8]= 50;          // TTl
            handShakePacket[9]= 17;          // UDP Protocol
            handShakePacket[10]= 0;          // CheckSum
            handShakePacket[11]= 0;          // CheckSum

            // 12-15, Random IP
            handShakePacket[12]= (byte)192;
            handShakePacket[13]= (byte)168;
            handShakePacket[14]= (byte)1;
            handShakePacket[15]= (byte)64;

            // 16-19, Server IP
            destAddr  = socket.getInetAddress().getAddress();
            for (int i = 0,j = 16 ; i<destAddr.length; i++, j++){
                handShakePacket[j] = destAddr[i] ;
            }

            cs = checksum(handShakePacket);
            handShakePacket[10]= (byte)((cs & 0xFF00)>>>8);
            handShakePacket[11]= (byte)(cs & 0x00FF);

            // 20-24, 4 bytes of data hard-coded to 0xDEADBEEF
            handShakePacket[20] = (byte) 0xDE;
            handShakePacket[21] = (byte) 0xAD;
            handShakePacket[22] = (byte) 0xBE;
            handShakePacket[23] = (byte) 0xEF;
            
            System.out.print("Handshake Response: 0x");
            outStream.write(handShakePacket);
            for (int l = 0; l < 4; l++) {
                System.out.printf("%x",getIS.read());
            }

            port = new byte [2];
            port[0] = (byte) getIS.read();
            port[1] = (byte) getIS.read();

            System.out.println("\nPort number received: " + (((port[0] & 0xFF) << 8) | (port[1] & 0xFF)) );
            System.out.println();

            for (int k = 0 ; k < 12 ; k++){

                int size = (28 + dataLength);
                IPv4packet = new byte[size];
                System.out.println("Sending packet with " + dataLength + " bytes of data");

                // 0-3
                IPv4packet[0]= 0b01000101;  // Version + HLen
                IPv4packet[1]= 0;           // TOS
                IPv4packet[2]= (byte) (size >>> 8);     // 1st half of length
                IPv4packet[3]= (byte)(size );           // 2nd half of length

                // 4-7
                IPv4packet[4]= 0;           // 1st half of identification
                IPv4packet[5]= 0;           // 2nd half of identification
                IPv4packet[6]= 0b01000000;  // Flag
                IPv4packet[7]= 0;           // Offset

                // 8-11
                IPv4packet[8]= 50;          // TTl
                IPv4packet[9]= 17;          // UDP Protocol
                IPv4packet[10]= 0;          // CheckSum
                IPv4packet[11]= 0;          // CheckSum

                // 12-15,
                IPv4packet[12]= (byte)0x6a;
                IPv4packet[13]= (byte)0x64;
                IPv4packet[14]= (byte)0xf5;
                IPv4packet[15]= (byte)0x0d;

                // 16-19, Server IP
                destAddr  = socket.getInetAddress().getAddress();
                for (int i = 0,j = 16 ; i<destAddr.length; i++, j++){
                    IPv4packet[j] = destAddr[i] ;
                }

                cs = checksum(IPv4packet);
                IPv4packet[10]= (byte)((cs & 0xFF00)>>>8);
                IPv4packet[11]= (byte)(cs & 0x00FF);

                IPv4packet[20] = 3;
                IPv4packet[21] = 1;
                IPv4packet[22] = port[0];
                IPv4packet[23] = port[1];

                IPv4packet[24] = (byte) ((8 + dataLength) >>> 8);
                IPv4packet[25] = (byte) (8 + dataLength);
                IPv4packet[26]= 0;
                IPv4packet[27]= 0;


                randomData = new byte [size - 28];
                rand.nextBytes(randomData);
                for (int i = 28, j = 0 ; i < size; i++, j++) {
                    IPv4packet[i] = randomData[j];
                }

                udp = new byte [20 + dataLength];
                udp [0] = (byte) 0x6a;
                udp [1] = (byte) 0x64;
                udp [2] = (byte) 0xf5;
                udp [3] = (byte) 0x0d;

                udp [4] = (byte) 0x34;
                udp [5] = (byte) 0x25;
                udp [6] = (byte) 0x58;
                udp [7] = (byte) 0x9a;

                udp [8] = 0;
                udp [9] = 17;
                udp [10] = (byte) ((8 + dataLength) >>> 8);
                udp [11] = (byte) (8 + dataLength);

                for (int i = 12 , j = 20 ; j < size ; i++, j++) {
                    udp[i] = IPv4packet[j];
                }

                cs = checksum(udp);
                IPv4packet[26]= (byte) ((cs & 0xFF00)>>>8);
                IPv4packet[27]= (byte) (cs & 0x00FF ) ;

                startTime = System.currentTimeMillis();
                outStream.write(IPv4packet);
                System.out.printf("Response: 0x%x",getIS.read());
                endTime = System.currentTimeMillis();

                for (int l = 0; l < 3; l++) {
                    System.out.printf("%x",getIS.read());
                }

                avrageTime += endTime - startTime;
                System.out.println("\nRTT: " + (endTime - startTime) + "ms\n");
                dataLength *= 2;
            }
            System.out.printf("Average RTT: %.2fms", (avrageTime / 12) );

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