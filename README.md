# FloppyKeyboard
Java code enabling midi input to be translated into floppy drive musical output.

The code is thoroughly commented in Slovenian, but it should translate fine with Google translate if any of the code proves not to be self-explanatory.

The intent of the project is enabling a Raspberry PI 3 to serve as a medium between a USB MIDI keyboard and a standard 3.5 floppy drive. It translates signals from the keyboard into impulses through the RPi IO pins, to which up to 10 floppy drives are connected. 

To control the IO Pins, the pi4j library (http://pi4j.com/) is used, so the numbering is a bit different than on the original pin layout. It can be found here: http://pi4j.com/pins/model-3b-rev1.html

To enable a floppy drive, a few prerequisites must be met connecting the floppy drives. First of all, the RPi pinout has insufficient power to power a floppy drive, so an external power supply will be needed. As for the wiring on the pins, you need two pins to controll the floppy drive, one for direction control and one to control the steps. 

1. Hotwire pin 13 and 14 on the floppy drive. This should turn it on. 
2. Connect the direction control pin (floppy pin 17) to a odd numbered pin on the PI looking at the chart above. 
3. Connect the step control pin (floppy pin 19) to the previous evem numbered pin on the PI looking at the chart above. 
4. Ground the floppy pin 18 to the PI (NOT the power source!)

So for example, connect the direction control pin to pin 1 on the PI, the step control pin to pin 0 on the PI. This will be floppy 0. Floppy one goes on pins 3 and 2 and so on. The floppy holes here are regarded in order if the tick is facing upward - the upper left pin is pin 1, the on under it is pin 2, the one right of 1 is 3 and so on.
