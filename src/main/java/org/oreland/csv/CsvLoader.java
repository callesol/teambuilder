package org.oreland.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.oreland.db.Repository;
import org.oreland.entity.Activity;
import org.oreland.entity.Level;
import org.oreland.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonas on 10/5/15.
 */
public class CsvLoader {

    String dir = "csv";
    public String getActivitiesFilename() {
        return dir + "/" + "activities.csv";
    }
    public String getParticipantsFilename() {
        return dir + "/" + "participants.csv";
    }

    public String getInvitationsFilename() {
        return dir + "/" + "invitations.csv";
    }

    public void load(Repository repo) throws IOException, ParseException {
        loadActivities(repo);
        loadInvitations(repo);
        loadParticipants(repo);
    }

    public void save(Repository repo) throws IOException, ParseException {
        new File(dir).mkdirs();
        saveActivities(repo);
        saveInvitations(repo);
        saveParticipants(repo);
    }

    public void loadActivities(Repository repo) throws ParseException, IOException {
        File f = new File(getActivitiesFilename());
        if (!f.exists()) {
            return;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(
                new FileReader(getActivitiesFilename()));
        for (CSVRecord record : records) {
            Activity g = new Activity();
            g.id = record.get("id");
            g.date = formatter.parse(record.get("date"));
            g.description = record.get("description");
            g.type = Activity.Type.parse(record.get("type"));
            g.level = Level.parse(record.get("level"));
            g.synced = Boolean.parseBoolean(record.get("synced"));
            repo.add(g);
        }
    }

    public void saveActivities(Repository repo) throws IOException {
        final Appendable out = new FileWriter(getActivitiesFilename());
        final CSVPrinter printer = CSVFormat.EXCEL.withHeader("id", "date", "description", "type", "level", "synced").print(out);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        for (Activity game : repo.getActivities()) {
            List rec = new ArrayList();
            rec.add(game.id);
            rec.add(formatter.format(game.date));
            rec.add(game.description);
            rec.add(game.type.toString());
            if (game.level != null)
                rec.add(game.level.toString());
            else
                rec.add("");
            rec.add(game.synced);
            printer.printRecord(rec);
        }
        printer.close();
    }

    public void loadInvitations(Repository repo) throws ParseException, IOException {
        File f = new File(getInvitationsFilename());
        if (!f.exists()) {
            return;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmss");
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(new FileReader(getInvitationsFilename()));
        for (CSVRecord record : records) {
            Activity.Invitation g = new Activity.Invitation();
            String game_id = record.get("game");
            Activity game = repo.getActivity(game_id);
            Player p = new Player();
            p.first_name = record.get("first_name");
            p.last_name = record.get("last_name");
            p.ssno = record.get("ssno");
            g.invitation_date = formatter.parse(record.get("invitation_date"));
            g.response = Activity.Response.parse(record.get("response"));
            if (g.response != Activity.Response.NO_RESPONSE) {
                g.response_date = formatter.parse(record.get("response_date"));
            }
            game.invitations.add(g);
        }
    }

    public void saveInvitations(Repository repo) throws ParseException, IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmss");
        final Appendable out = new FileWriter(dir + "/" + "invitations.csv");
        final CSVPrinter printer = CSVFormat.EXCEL.withHeader("game", "first_name", "last_name", "ssno", "invitation_date", "reponse", "response_date").print(out);
        for (Repository.Pair<Activity, Activity.Invitation> invitation : repo.getInvitations()) {
            List rec = new ArrayList();
            rec.add(invitation.first.id);
            rec.add(invitation.second.player.first_name);
            rec.add(invitation.second.player.last_name);
            rec.add(invitation.second.player.ssno);
            rec.add(formatter.format(invitation.second.invitation_date));
            rec.add(invitation.second.response.toString());
            if (invitation.second.response != Activity.Response.NO_RESPONSE)
                rec.add(formatter.format(invitation.second.response_date));
            else
                rec.add("");
            printer.printRecord(rec);
        }
        printer.close();
    }

    public void loadParticipants(Repository repo) throws ParseException, IOException {
        File f = new File(getParticipantsFilename());
        if (!f.exists()) {
            return;
        }
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(new FileReader(getParticipantsFilename()));
        for (CSVRecord record : records) {
            Activity.Participant g = new Activity.Participant();
            String game_id = record.get("game");
            Activity game = repo.getActivity(game_id);
            Player p = new Player();
            p.first_name = record.get("first_name");
            p.last_name = record.get("last_name");
            p.ssno = record.get("ssno");
            game.participants.add(g);
        }
    }

    public void saveParticipants(Repository repo) throws ParseException, IOException {
        final Appendable out = new FileWriter(getParticipantsFilename());
        final CSVPrinter printer = CSVFormat.EXCEL.withHeader("game", "first_name", "last_name", "ssno").print(out);
        for (Repository.Pair<Activity, Activity.Participant> participant : repo.getParticipants()) {
            List rec = new ArrayList();
            rec.add(participant.first.id);
            rec.add(participant.second.player.first_name);
            rec.add(participant.second.player.last_name);
            rec.add(participant.second.player.ssno);
            printer.printRecord(rec);
        }
        printer.close();
    }
}
