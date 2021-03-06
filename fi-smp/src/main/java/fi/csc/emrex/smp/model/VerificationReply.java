/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.csc.emrex.smp.model;

import fi.csc.emrex.common.model.Person;
import org.apache.commons.lang.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerificationReply {

    private String sessionId;

    private int score;

    private boolean nameVerified;

    private boolean bDayVerified;

    private String fullNameInElmo;

    private String fullNameFromHomeInstitute;

    private String homeInstitute;

    private final List<String> messages;

    public VerificationReply() {
        messages = new ArrayList<String>();
    }

    public String getFullNameFromHomeInstitute() {
        return fullNameFromHomeInstitute;
    }

    public void setFullNameFromHomeInstitute(String fullNameFromHomeInstitute) {
        this.fullNameFromHomeInstitute = fullNameFromHomeInstitute;
    }

    public String getFullNameInElmo() {
        return fullNameInElmo;
    }

    public void setFullNameInElmo(String fullNameInElmo) {
        this.fullNameInElmo = fullNameInElmo;
    }

    public String getHomeInstitute() {
        return homeInstitute;
    }

    public void setHomeInstitute(String homeInstitute) {
        this.homeInstitute = homeInstitute;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isVerified() {
        return nameVerified && bDayVerified;
    }

    public boolean isNameVerified() {
        return nameVerified;
    }

    public void setNameVerified(boolean nameVerified) {
        this.nameVerified = nameVerified;
    }

    public boolean isbDayVerified() {
        return bDayVerified;
    }

    public void setbDayVerified(boolean bDayVerified) {
        this.bDayVerified = bDayVerified;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void addMessage(String msg) {
        messages.add(msg);
    }

    public static VerificationReply verify(Person firstPerson, Person otherPerson, double threshold) {
        VerificationReply r = new VerificationReply();
        if (firstPerson == null || otherPerson == null) {
            r.setNameVerified(false);
            r.setbDayVerified(false);
            r.addMessage("Person missing");
            return r;
        }
        r.setFullNameFromHomeInstitute(firstPerson.getFullName());
        r.setFullNameInElmo(otherPerson.getFullName());
        r.setHomeInstitute(firstPerson.getHomeOrganizationName());
        boolean bdMatch = false;
        boolean nameMatch = false;
        int match = 0;
        LocalDate vbd = firstPerson.getBirthDate();
        LocalDate ebd = otherPerson.getBirthDate();

        String message = "";
        if (ebd == null || vbd == null) {
            message = "Birthdate not set for " + (ebd == null ? "elmo" : "local") + " person.";
            r.addMessage(message);
            log.info(message);
        } else if (!ebd.equals(vbd)) {
            message = "Birthdate does not match.";
            r.addMessage(message);
            log.info(message);
            log.info("haka bday " + vbd.toString() + "elmo bday:" + ebd.toString());

        } else {
            bdMatch = true;
        }
        double score = 0;
        score += levenshteinDistance(firstPerson.getLastName(), otherPerson.getLastName());
        score += levenshteinDistance(firstPerson.getFirstName(), otherPerson.getFirstName());
        double length = 0;
        String fullname = firstPerson.getFullName();
        if (fullname != null) {
            length = fullname.length();
        } else {
            length = 1;
        }
        double ratio = score / length;
        r.addMessage("Error ratio " + ratio + " based on Levenshtein check on name.");
        if (ratio > threshold) {
            r.addMessage("Ratio over threshold " + threshold);
        } else {
            nameMatch = true;
        }

        r.setNameVerified(nameMatch);
        r.setbDayVerified(bdMatch);

        return r;
    }

    private static int levenshteinDistance(String s, String t) {
        if (s == null && t == null) {
            return 0;
        }
        if (s == null || s.length() == 0) {
            return t.length();
        }
        if (t == null || t.length() == 0) {
            return s.length();
        }

        int[] v0 = new int[t.length() + 1];
        int[] v1 = new int[t.length() + 1];

        for (int i = 0; i < v0.length; i++) {
            v0[i] = i;
        }

        for (int i = 0; i < s.length(); i++) {
            // calculate v1 (current row distances) from the previous row v0

            // first element of v1 is A[i+1][0]
            // edit distance is delete (i+1) chars from s to match empty t
            v1[0] = i + 1;

            // use formula to fill in the rest of the row
            for (int j = 0; j < t.length(); j++) {
                int cost = (s.charAt(i) == t.charAt(j)) ? 0 : 1;
                v1[j + 1] = Math.min(Math.min(v1[j] + 1, v0[j + 1] + 1), v0[j] + cost);
            }

            // copy v1 (current row) to v0 (previous row) for next iteration
            for (int j = 0; j < v0.length; j++) {
                v0[j] = v1[j];
            }
        }

        return v1[t.length()];
    }

}
