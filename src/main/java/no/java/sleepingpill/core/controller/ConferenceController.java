package no.java.sleepingpill.core.controller;

import no.java.sleepingpill.core.session.SessionService;
import static spark.Spark.after;
import static spark.Spark.get;
import static spark.Spark.post;
import static no.java.sleepingpill.core.util.JsonUtil.jsonBuddyString;

public class ConferenceController {
    private SessionService sessionService;

    public ConferenceController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public ConferenceController(){
        this(SessionService.instance());
    }

    public void initSpark(){
        get("/data/conference", (req, res) -> sessionService.allConferences(), jsonBuddyString());
    }

}
