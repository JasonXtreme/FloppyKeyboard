package MIDIFloppies;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.system.SystemInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

//Razred za upravljanje z disketniki. Tipa Singleton
public class FloppyHandler implements Runnable {

    //Interna spremenljivka za edino instanco razreda.
    private static FloppyHandler instance = null;

    //Konstruktor
    protected FloppyHandler() {
    }

    //Funkcija za dostop do edine instance razreda. V kolikor ne obstaja, se
    //ustvari, drugače se vrne obstoječa instanca.
    public static FloppyHandler getInstance() {
        if (instance == null) {
            instance = new FloppyHandler();
        }
        return instance;
    }

    //Gpio Controller Pi4J knjižnice za upravljanje z RPi GPIO
    final static GpioController gpio = GpioFactory.getInstance();

    //Resolucija obdelave sprememb v mikrosekundah
    static final int TimerResolution = 40;
    
    //Trajanje intervala note v mikrosekundah (en tick vsakih n mikrosekund, definiranih zgoraj.
    //Privzeto z interneta, nepreverjeno.    
    public final static int[] NoteDurations = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        30578, 28861, 27242, 25713, 24270, 22909, 21622, 20409, 19263, 18182, 17161, 16198, //C1 - B1
        15289, 14436, 13621, 12856, 12135, 11454, 10811, 10205, 9632, 9091, 8581, 8099, //C2 - B2
        7645, 7218, 6811, 6428, 6068, 5727, 5406, 5103, 4816, 4546, 4291, 4050, //C3 - B3
        3823, 3609, 3406, 3214, 3034, 2864, 2703, 2552, 2408, 2273, 2146, 2025, //C4 - B4
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    //Seznam vseh GPIO pinov
    static GpioPinDigitalOutput[] pins;

    //Metoda za inicializacijo GPIO pinov in disketnikov
    public static void initPins() throws InterruptedException {
        //Seznam vseh pinov na RPi 3 B
        Pin[] tmppins = RaspiPin.allPins(SystemInfo.BoardType.RaspberryPi_3B);
        
        //Inicializacija seznama
        pins = new GpioPinDigitalOutput[tmppins.length];

        //Uporaba for namesto foreach, ker potrebujem enumerator
        //Inicializacija pinov - nastavitve privzetega stanja pinov ob zagonu in
        //ob koncu na LOW ter ustvarjanje instance iz teoretične enumeracije pinov
        
        for (int i = 0; i < tmppins.length; i++) {
            pins[i] = gpio.provisionDigitalOutputPin(tmppins[i], "", PinState.LOW);
            pins[i].setShutdownOptions(Boolean.TRUE, PinState.LOW);
        }
        
        //Hardcode števila povezanih disketnikov. Če jih inicializiramo več,
        //se lahko note pošiljajo na prazne pine.
        int numDrives = 5;
        for (int i = 0; i < numDrives*2; i += 2) {
            //Inicializacija disketnika
            FloppyDrive d = new FloppyDrive(pins[i], pins[i + 1]);
        }
    }   
    
    //Metoda, ki posodobi vse povezane disketnike ob ticku ustrezne niti
    private static void Tick() {
        FloppyDrive.allDrives.stream().forEach(e -> e.Update());
    }

    //Metoda, ki ponastavi položaj glav vseh disketnikov ob zagonu
    public static void resetDrives() {
        FloppyDrive.allDrives.stream().forEach(e -> {
            try {
                e.ResetDrive();
            } catch (InterruptedException ex) {
                Logger.getLogger(FloppyHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    //Metoda za upravljanje s pritiski in spusti. -12 premakne tone oktavo nižje,
    //da so v dosegu disketnikov, saj slednji ne zmorejo višjih tonov
    public static void HandleNote(int Note, boolean Pressed) {
        if (Pressed) {
            FloppyDrive.AssignNote(Note - 12);
        } else {
            FloppyDrive.ReleaseNote(Note - 12);
        }
    }      

    //Runnable nit, ki posodablja stanje disketnikov
    @Override
    public void run() {
        //pridobi trenutek v nanosekundah in inicializiraj spremenljivke
        long lastTime = System.nanoTime();
        long currentTime = lastTime;
        long delta = 0;
        while (true) {
            //Pridobi novi čas
            currentTime = System.nanoTime();
            //Poračunaj razliko med trenutnim in zadnjim časom
            delta = currentTime - lastTime;
            //Če je razlika večja kot je določena ločljivost premika, izvedi tick
            //*1000, ker imamo opravka z nanosekundami in je operacija množenja
            //hitrejša od operacije deljenja
            if (delta > TimerResolution * 1000) {
                //Zapomni si zadnji čas in izvedi tick. Zaradi tega dela kode se
                //občasno, ko je RPi preveč zaseden, spremeni frekvenca not.
                lastTime = currentTime;
                Tick();
            }

        }      
    }
}
