package no.java.sleepingpill.core.session;


import no.java.sleepingpill.core.commands.*;
import no.java.sleepingpill.core.event.Event;
import org.jsonbuddy.JsonArray;
import org.jsonbuddy.JsonFactory;
import org.jsonbuddy.JsonObject;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SessionHolderTest {
    private static final String CONFERENCE_ID = "eventx";

    private SessionHolder sessionHolder = new SessionHolder();

    @Test
    public void shouldFindSessionByEmail() throws Exception {
        String sessionId = createSession();

        List<Session> sessions = sessionHolder.sessionsByEmail("darth@deathstar.com");
        assertThat(sessions).hasSize(1).extracting(Session::getId).containsOnly(sessionId);

    }

    private String createSession() {
        SpeakerData darth = new SpeakerData()
            .setEmail("darth@deathstar.com")
            .setName("Darth Vader")
            .addData("bio", DataField.simplePublicStringValue("Here is my bio"));

        CreateNewSession sessionOne = new CreateNewSession()
                .setConferenceId(CONFERENCE_ID)
                .addSpeaker(darth)
                .addData("title", DataField.simplePublicStringValue("SessionOne"));

        sessionHolder.eventAdded(sessionOne.createEvent());
        return sessionOne.getSessionId();
    }

    @Test
    public void shouldBeAbleToUpdateBio() throws Exception {
        String sessionId = createSession();

        Session session = sessionHolder.sessionFromId(sessionId).orElseThrow(() -> new RuntimeException("Did not find session"));

        Speaker darthSpeaker = session.getSpeakers().get(0);

        SpeakerData darthUpdate = new SpeakerData()
                .setId(Optional.of(darthSpeaker.getId()))
                .addData("bio", DataField.simplePublicStringValue("Darth updated bio"));

        UpdateSession updateSession = new UpdateSession(session.getId(),CONFERENCE_ID)
                .addSpeakerData(darthUpdate);

        Event addSpeakerEvent = updateSession.createEvent(session);
        sessionHolder.eventAdded(addSpeakerEvent);

        session = sessionHolder.sessionFromId(sessionId).orElseThrow(() -> new RuntimeException("Did not find session"));

        assertThat(session.getSpeakers()).hasSize(1);
        darthSpeaker = session.getSpeakers().get(0);

        assertThat(darthSpeaker.getEmail()).isEqualTo("darth@deathstar.com");
        assertThat(darthSpeaker.dataValue("bio")).contains(DataField.simplePublicStringValue("Darth updated bio"));


    }

    @Test
    public void shouldBeAbleToAddASpeaker() throws Exception {
        String sessionId = createSession();
        Session session = sessionHolder.sessionFromId(sessionId).orElseThrow(() -> new RuntimeException("Did not find session"));

        Speaker darthSpeaker = session.getSpeakers().get(0);

        SpeakerData darthUpdate = new SpeakerData()
                .setId(Optional.of(darthSpeaker.getId()));

        SpeakerData luke = new SpeakerData()
                .setEmail("luke@endor.com")
                .setName("Luke Skywalker")
                .addData("bio",DataField.simplePublicStringValue("Is Darth Vader my father?"));


        UpdateSession updateSession = new UpdateSession(session.getId(),CONFERENCE_ID)
                .addSpeakerData(darthUpdate)
                .addSpeakerData(luke);

        Event addSpeakerEvent = updateSession.createEvent(session);
        sessionHolder.eventAdded(addSpeakerEvent);

        session = sessionHolder.sessionFromId(sessionId).orElseThrow(() -> new RuntimeException("Did not find session"));

        assertThat(session.getSpeakers()).hasSize(2);

    }


    @Test
    public void shouldBeAbleToDeleteSpeaker() throws Exception {
        String sessionId = createSession();

        Session session = sessionHolder.sessionFromId(sessionId).orElseThrow(() -> new RuntimeException("Did not find session"));

        Speaker darthSpeaker = session.getSpeakers().get(0);

        SpeakerData darthUpdate = new SpeakerData()
                .setId(Optional.of(darthSpeaker.getId()));



        UpdateSession updateSession = new UpdateSession(session.getId(),CONFERENCE_ID)
                .addSpeakerData(darthUpdate);

        Event addSpeakerEvent = updateSession.createEvent(session);
        sessionHolder.eventAdded(addSpeakerEvent);

        session = sessionHolder.sessionFromId(sessionId).orElseThrow(() -> new RuntimeException("Did not find session"));

        assertThat(session.getSpeakers()).hasSize(1);
        assertThat(session.getSpeakers().get(0).getId()).isEqualTo(darthSpeaker.getId());

    }

    @Test
    public void shouldDeleteSession() throws Exception {
        String sessionId = createSession();

        DeleteSession deleteSession = new DeleteSession(CONFERENCE_ID,sessionId);

        sessionHolder.eventAdded(deleteSession.createEvent());

        assertThat(sessionHolder.allSessions()).isEmpty();

    }

    @Test
    public void shouldBeAbleToAddComment() throws Exception {
        String sessionId = createSession();

        Session session = sessionHolder.sessionFromId(sessionId).orElseThrow(() -> new RuntimeException("Did not find session"));

        UpdateSession updateSession = new UpdateSession(session.getId(), CONFERENCE_ID);
        updateSession.setLastUpdated(session.getLastUpdated());

        List<NewComment> newComments = Collections.singletonList(
                new NewComment("anders@pkom", "Program comitee", "Please fill in all fields in the form"));

        updateSession.addComments(newComments);

        Event addCommentEvent = updateSession.createEvent(session);
        sessionHolder.eventAdded(addCommentEvent);

        session = sessionHolder.sessionFromId(sessionId).orElseThrow(() -> new RuntimeException("Did not find session"));

        List<Comment> sessionComments = session.getComments();

        assertThat(sessionComments).hasSize(1);

        Comment comment = sessionComments.get(0);

        assertThat(comment.getId()).isNotNull();

    }
}