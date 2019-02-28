/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
 
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
 
/**
 *
 * @author dmcd2356
 */
public class AudioClipPlayer implements LineListener {

    // the audio clips used
    private static final String audioclipTada = "resources/tada.wav";
    
    private boolean playCompleted; // true if playback completes
     
    /**
     * Listens to the START and STOP events of the audio line.
     */
    @Override
    public void update(LineEvent event) {
        LineEvent.Type type = event.getType();
         
        if (type == LineEvent.Type.START) {
            System.out.println("Playback started.");
             
        } else if (type == LineEvent.Type.STOP) {
            playCompleted = true;
            System.out.println("Playback completed.");
        }
 
    }
     
    /**
     * Plays the "tada" audio file.
     */
    public void playTada () {
        play(audioclipTada);
    }
    
    /**
     * Play a given audio file.
     * @param audioFilePath Path of the audio file.
     */
    private void play (String audioFilePath) {
        URI audioFileURI;
        try {
            audioFileURI = getClass().getResource(audioFilePath).toURI();
        } catch (URISyntaxException ex) {
            System.out.println("File not found: " + audioFilePath);
            return;
        }
        File audioFile = new File(audioFileURI);
        if (!audioFile.isFile()) {
            System.out.println("File not found: " + audioFileURI);
            return;
        }
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            try (Clip audioClip = (Clip) AudioSystem.getLine(info)) {
                audioClip.addLineListener(this);
                audioClip.open(audioStream);
                audioClip.start();
                
                while (!playCompleted) {
                    // wait for the playback completes
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        System.out.println("AudioClipPlayer interrupted.");
                        return;
                        //ex.printStackTrace();
                    }
                }
            }
             
        } catch (UnsupportedAudioFileException ex) {
            System.out.println("The specified audio file is not supported.");
            //ex.printStackTrace();
        } catch (LineUnavailableException ex) {
            System.out.println("Audio line for playing back is unavailable.");
            //ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Error playing the audio file.");
            //ex.printStackTrace();
        }
    }
 
}
