package MIDIFloppies;

import java.io.IOException;
import MIDIFloppies.MidiHandler.MidiEventListener;

/**
 *
 * @author Matija Koželj
 * Program je namenjen poganjanju na Raspberry Pi Model 3 B+
 * Namen programa je povezava MIDI klaviature na disketnike tako, da lahko 
 * slednji nadomestijo zvočnike. Vsak disketnik potrebuje za delovanje dva IO
 * pina, enega za izbor smeri premika glave, ter drugega za izvajanje samega
 * premika. 
 * 
 * Po shemi na http://pi4j.com/pins/model-3b-rev1.html se pini v programu
 * številčijo po vrsti, torej je pin za izbor smeri na disketniku ena IO_0, pin 
 * za premik pa IO_1, naslednji disketnik uporablja pina IO_2 in IO_3 ter tako 
 * naprej.
 * 
 * Povezava disketnika je odvisna od modela disketnika in uporabljenega kabla.
 * 
 * Princip ustvarjanja zvoka je relativno enostaven, sprememba napetosti na pinu
 * za premik premakne glavo za korak - če so ti premiki izvedeni s pravo 
 * frekvenco, zazveni ton. Več o tem v sami kodi.
 */

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        /** Vstop v program, ustvarjanje instance razreda MidiHandler, ki je
        * namenjena upravljanju z MIDI napravami in njihovim IO
        */ 
        MidiHandler handler = new MidiHandler();

        //Event handler za pritisk in spust tipke na klaviaturi
        MidiEventListener listener = new MidiEventListener() {
            @Override
            public void KeyDown(int Key) {
                System.out.println("Key Down: " + Key);
                //Ker uporabljamo samo eno instanco za upravljanje z disketniki,
                //je razred statičen. Prav tako nekatere metode.
                FloppyHandler.HandleNote(Key, true);
            }
            @Override
            public void KeyUp(int Key) {
                System.out.println("Key Up: " + Key);
                FloppyHandler.HandleNote(Key, false);
            }
        };
        
        //Dodajanje event handlerja instanci MidiHandlerja
        handler.addListener(listener);
        //Ustvarjanje FloppyHandler singleton-a, vsi naslednji klici bodo vrnili
        //isto instanco
        FloppyHandler.getInstance();
        //Inicializacija pin IO in disketnikov
        FloppyHandler.initPins();
        
        //Zagon niti za premikanje glav disketnikov 
        Thread t = new Thread(FloppyHandler.getInstance());
        t.run();
        
        //Čakanje na konec programa
        System.in.read();
        handler.close();
    }

}
