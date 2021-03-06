package no.java.sleepingpill.core.session;

import no.java.sleepingpill.core.util.DateUtil;
import org.jsonbuddy.JsonArray;
import org.jsonbuddy.JsonFactory;
import org.jsonbuddy.JsonNode;
import org.jsonbuddy.JsonObject;

import java.util.*;

import static no.java.sleepingpill.core.session.SessionVariables.*;

public class Session extends DataObject {
    private final String conferenceId;
    private final Optional<String> addedByEmail;
    private volatile String lastUpdated;
    private volatile SessionStatus sessionStatus = SessionStatus.DRAFT;
    private volatile List<Speaker> speakers = new ArrayList<>();
    private volatile List<Comment> comments = new ArrayList<>();

    public Session(String id, String conferenceId,Optional<String> addedByEmail) {
        super(id);
        this.conferenceId = conferenceId;
        this.addedByEmail = addedByEmail;
    }



    @Override
    public Map<String, DataField> getData() {
        return super.getData();
    }

    @Override // Needed for json generation
    public String getId() {
        return super.getId();
    }


    public String getConferenceId() {
        return conferenceId;
    }

    public JsonObject asSingleSessionJson() {
        JsonObject result = JsonFactory.jsonObject()
                .put("id", getId())
                .put(SESSION_ID, getId())
                .put(SPEAKER_ARRAY, JsonArray.fromNodeStream(speakers.stream().map(Speaker::singleSessionData)))
                .put(DATA_OBJECT, dataAsJson())
                .put(SESSION_STATUS,sessionStatus)
                .put(CONFERENCE_ID,conferenceId)
                .put(LAST_UPDATED,lastUpdated)
                .put(COMMENT_ARRAY,JsonArray.fromNodeStream(comments.stream().map(Comment::toJson)))
                ;
        addedByEmail.ifPresent(mail -> result.put(SessionVariables.POSTED_BY_MAIL,mail));
        return result;
    }

    public boolean isPublic() {
        return sessionStatus == SessionStatus.APPROVED || sessionStatus == SessionStatus.HISTORIC;
    }

    public JsonObject asPublicSessionJson() {
        if (!isPublic()) {
            throw new RuntimeException("Tried to handle private session as public");
        }
        JsonObject result = JsonFactory.jsonObject();
        result.put(SESSION_ID,getId());
        result.put(CONFERENCE_ID,conferenceId);
        Map<String, DataField> data = getData();
        for (String key : data.keySet()) {
            data.get(key).readPublicData().ifPresent(da -> result.put(key,da));
        }
        result.put(SPEAKER_ARRAY,JsonArray.fromNodeStream(speakers.stream().map(Speaker::asPublicJson)));
        return result;
    }



    public SessionStatus getSessionStatus() {
        return sessionStatus;
    }

    @Override
    public void addData(JsonObject update) {
        super.addData(update);
        Optional<JsonArray> optSpeaker = update.arrayValue(SPEAKER_ARRAY);
        optSpeaker.ifPresent(this::updateSpeakers);
        update.arrayValue(COMMENT_ARRAY).ifPresent(this::updateComments);
        this.lastUpdated = update.stringValue(LAST_UPDATED).orElse(DateUtil.get().generateLastUpdated());

    }

    private void updateComments(JsonArray updatedCommentsJson) {
        updatedCommentsJson.objectStream()
                .map(Comment::fromJson)
                .forEach(this.comments::add);
    }

    private void updateSpeakers(JsonArray updatedSpeakersJson) {
        List<Speaker> updatedSpeakers = new ArrayList<>();

        for (Speaker exisisting : this.speakers) {
            Optional<JsonObject> updateOnSpeaker = updatedSpeakersJson.objectStream()
                    .filter(ob -> ob.objectValue("id").orElse(JsonFactory.jsonObject()).stringValue("value").equals(Optional.of(exisisting.getId())))
                    .findAny();
            updateOnSpeaker.map(exisisting::update).ifPresent(updatedSpeakers::add);
        }

        updatedSpeakersJson.objectStream()
                .filter(ob -> speakerExists(updatedSpeakers, ob))
                .forEach(ob -> updatedSpeakers.add(Speaker.fromJson(getId(),ob)));


        this.speakers = updatedSpeakers;
    }

    private boolean speakerExists(List<Speaker> updatedSpeakers, JsonObject ob) {
        Optional<String> id = ob.objectValue("id").map(vo -> vo.stringValue("value").orElse(null));
        return !updatedSpeakers.stream().filter(speak -> Optional.of(speak.getId()).equals(id)).findAny().isPresent();
    }

    public List<Speaker> getSpeakers() {
        return new ArrayList<>(speakers);
    }

    public List<Comment> getComments() {
        return new ArrayList<>(comments);
    }

    public Session setSessionStatus(SessionStatus sessionStatus) {
        this.sessionStatus = sessionStatus;
        return this;
    }

    public boolean isRelatedToEmail(String email) {
        if (email == null) {
            return false;
        }
        if (addedByEmail.filter(ab -> ab.equalsIgnoreCase(email)).isPresent()) {
            return true;
        }
        boolean match = speakers.stream()
                .anyMatch(sp -> email.equalsIgnoreCase(sp.getEmail()));
        return match;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return Objects.equals(getId(), session.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public Session setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }
}
