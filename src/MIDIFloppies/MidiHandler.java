package MIDIFloppies;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class MidiHandler {

    //Spremenljivka za izbrano MIDI napravo
    MidiDevice device;

    //Konstruktor
    public MidiHandler() throws IOException {
        //Seznam podatkov o vseh MIDI napravah na voljo
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        
        try {
            //Izpis vseh naprav
            for (int i = 0; i < infos.length; i++) {
                device = MidiSystem.getMidiDevice(infos[i]);
                System.out.println(i);
                System.out.println(infos[i].getDescription());
                System.out.println(infos[i]);  
            }

            //Hardcode za klaviaturo priklopljeno na RPi 3
            int devNum = 1;
            
            //System.out.println("Please input number of device to use: "); 
            
            //try {
                // Scanner input = new Scanner(System.in);
                // devNum = input.nextInt();
            //} catch (Exception e) {
            //}
            //-uporaba zgornjega v primeru, da naprava nima stalnega naslova
            
            //Izbor podane naprave
            device = MidiSystem.getMidiDevice(infos[devNum]);

            //Pridobivanje povezave na oddajnik MIDI naprave
            Transmitter trans = device.getTransmitter();
            trans.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));
            
            //Odpiranje povezave na napravo
            device.open();
            
            //Izpis uspešne povezave
            System.out.println(device.getDeviceInfo() + " Was Opened");
        } catch (MidiUnavailableException e) {
            //Izpis morebitne napake
            System.out.println(e.getMessage());
        }
    }

    //Funkcija za zaustavitev naprave
    public void close() {
        device.close();
    }

    //Razred za upravljanje z inputom MIDI naprave - dedovanje razreda Receiver
    public class MidiInputReceiver implements Receiver {

        //Ime izbrane naprave
        public String name;

        //Konstruktor
        public MidiInputReceiver(String name) {
            this.name = name;
        }

        //Prepis metode za prejem sporočila
        @Override
        public void send(MidiMessage msg, long timeStamp) {
            try {
                //Če ima sporočilo vsebino
                if (msg.getLength() > 1) {
                    //Če je stanje sporočila 144 (note on)
                    if (msg.getStatus() == 144) {
                        //Preberi sporočilo in iz njega izlušči tipko in glasnost
                        //Če je glasnost 0, javi spust tipke, drugače javi pritisk
                        byte[] message = msg.getMessage();
                        int Key = message[1];
                        int Velocity = message[2];
                        if (Velocity <= 0) {
                            KeyUp(Key);
                        } else {
                            KeyDown(Key);
                        }
                    }
                }
            } catch (Exception e) {
                //Izpis napake
                System.out.println("Message exception: " + e.getMessage());
            }
        }

        //Prazna metoda close, da zadostimo vmesniku Receiver
        @Override
        public void close() {
        }
    }

    //Vmesnik za event listener, vsebuje funkciji za pritisk in spust tipke
    public interface MidiEventListener {
        void KeyDown(int Key);
        void KeyUp(int Key);
    }
    
    //Interni seznam vseh poslušalcev dogodka
    private List<MidiEventListener> listeners = new ArrayList<>();

    //Metoda za dodajanje poslušalca
    public void addListener(MidiEventListener toAdd) {
        listeners.add(toAdd);
    }

    //Metoda za odstranjevanje poslušalca, pred tem preveri, če sploh obstaja
    public void removeListener(MidiEventListener toRemove) {
        if (listeners.contains(toRemove)) {
            listeners.remove(toRemove);
        }
    }

    //Metoda za odstranjevanje vseh poslušalcev
    public void clearListeners() {
        listeners = new ArrayList<>();
    }

    //Metoda za signal spusta tipke
    public void KeyUp(int key) {
        listeners.forEach((hl) -> {
            hl.KeyUp(key);
        });
    }

    //Metoda za signal pritiska tipke
    public void KeyDown(int key) {
        listeners.forEach((hl) -> {
            hl.KeyDown(key);
        });
    }
}
