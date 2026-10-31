package com.ilia.advanceclock;

import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression guard: new direct sockets/HTTP or unguarded native streams require an explicit security review. */
public class NetworkBoundarySourceTest {
    @Test public void appNetworkingCannotSilentlyBypassTheReadOnlyBoundary()throws Exception {
        Path root=Paths.get("src/main/java/com/ilia/advanceclock");
        if(!Files.isDirectory(root))root=Paths.get("app/src/main/java/com/ilia/advanceclock");
        assertTrue(Files.isDirectory(root));
        try(var files=Files.list(root)) {
            for(Path path:(Iterable<Path>)files::iterator) {
                if(!path.toString().endsWith(".java"))continue;
                String source=Files.readString(path);
                if(!path.getFileName().toString().equals("ReadOnlyNetwork.java"))assertFalse(path.toString(),source.contains(".openConnection("));
                assertFalse(path.toString(),source.contains("new Socket("));
                assertFalse(path.toString(),source.contains("SpeechRecognizer.createSpeechRecognizer("));
                for(String line:source.split("\\n"))if(line.contains(".setDataSource(") || line.contains(".setVideoURI("))
                    assertTrue(path+": "+line,line.contains("LocalMedia.require("));
            }
        }
    }
}
