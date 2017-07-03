package MIDIFloppies;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import static MIDIFloppies.FloppyHandler.NoteDurations;


//Razred za shranjevanje informacij o disketniku
public class FloppyDrive {

    //Pin za premik glave
    private GpioPinDigitalOutput MovePin;
    //Pin za smer glave
    private GpioPinDigitalOutput DirectionPin;

    //Maksimalen hod glave - 80 korakov
    private static final int MaxPosition = 80;
    
    //Trenutni položaj glave
    private int CurrentPosition = 0;
    //Trenutna nota
    public int CurrentNote = 0;
    //Trenutni interval note (v tickih)
    public int CurrentInterval = 0;
    //Trenutno število tickov
    private int CurrentTicks = 0;

    //Seznam vseh ustvarjenih disketnikov
    public static List<FloppyDrive> allDrives = new ArrayList<>();

    //Konstruktor
    public FloppyDrive(GpioPinDigitalOutput movePin, GpioPinDigitalOutput directionPin) throws InterruptedException {
        //Nastavi pina, ponastavi položaj glave in dodaj disketnik na seznam vseh
        this.MovePin = movePin;
        this.DirectionPin = directionPin;
        ResetDrive();
        allDrives.add(this);
    }

    public void Toggle() {
        try {
            //Spremeni napetost na pinu z smer glave - naredi korak glave.
            MovePin.toggle();
            //Če gre glava stran od začetne pozicije
            if (MovePin.isHigh()) {
                //Če smo na koncu hoda, obrni smer
                if (CurrentPosition + 1 > MaxPosition - 1 || CurrentPosition - 1 < 0) {
                    DirectionPin.toggle();
                }
                
                //Primerno zamakni spremenljivko za sledenje položaja glave
                if (DirectionPin.isHigh()) {
                    CurrentPosition++;
                } else {
                    CurrentPosition--;
                }
            }
        } catch (Exception e) {
            //Če pride do napake na GPIO, jo zanemarimo, v naslednjem ciklu navadno izgine.
        }
    }

    //Metoda za posodobitev disketnika
    public void Update() {
        //Če imamo določeno noto za disketnik
        if (CurrentNote != 0) {
            //Če se je zgodilo več tickov, kot je časovni interval note, 
            //premakni glavo in ponastavi število tickov, drugače dodaj tick.
            if (CurrentTicks >= CurrentInterval) {
                Toggle();
                CurrentTicks = 0;
            } else {
                CurrentTicks++;
            }
        }
    }

    //Ponastavi pogon
    void ResetDrive() throws InterruptedException {
        //Prisilno premakni glavo do konca v eno ter nato v drugo smer. Če ne
        //začnemo v izhodiščnem položaju, se premiki čez limit prezrejo zaradi
        //varovalke na disketniku - tako vedno končamo na istem položaju
        
        for (int i = 0; i < MaxPosition * 2; i++) {
            Toggle();
            Thread.sleep(0, FloppyHandler.TimerResolution * 1000);
            Toggle();
            Thread.sleep(0, FloppyHandler.TimerResolution * 1000);
        }
    }

    //Metoda za dodeljevanje note disketniku
    public static void AssignNote(int Note) {
        //Ustvari stream iz sezname disketnikov
        Stream<FloppyDrive> stream = allDrives.stream();
        //Poišči prazen disketnik ali vrni poljubnega, če takšen ne obstaja
        FloppyDrive drive = stream.filter(e -> e.CurrentNote == 0).findFirst().orElseGet(() -> {
            return stream.findAny().get();
        });
        //Disketniku nastavi noto
        drive.CurrentNote = Note;
        //Če je nota v definiranem obsegu
        if (Note < NoteDurations.length) {
            //Nastavi disketniku interval iz definiranega seznama, deli ga z
            //dvakratnikom intervala tickov - napaka v seznamu intervalov
            drive.CurrentInterval = (int)(NoteDurations[Note] / FloppyHandler.TimerResolution * 0.5f);
        }
    }

    //Metoda za spust note
    public static void ReleaseNote(int Note) {
        //V seznamu poišči vse disketnike, ki igrajo noto
        Stream<FloppyDrive> stream = allDrives.stream();
        stream = stream.filter(e -> e.CurrentNote == Note);
        //Nastavi interval in vrednost note na 0 - izklopi disketnik
        stream.forEach(e -> {
            e.CurrentNote = 0;
            e.CurrentInterval = 0;
        });
    }
}
