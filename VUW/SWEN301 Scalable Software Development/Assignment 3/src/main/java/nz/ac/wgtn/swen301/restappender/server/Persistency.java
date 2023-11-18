package nz.ac.wgtn.swen301.restappender.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Persistency {
    public static final List<LogEvent> DB = Collections.synchronizedList(new ArrayList<>());
}
