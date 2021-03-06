package org.oreland.sync;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.oreland.Pair;
import org.oreland.db.Repository;
import org.oreland.entity.Activity;
import org.oreland.entity.Level;
import org.oreland.entity.Player;
import org.oreland.entity.TargetLevel;
import org.oreland.sync.util.FormValues;
import org.oreland.sync.util.SyncHelper;
import org.oreland.ui.Dialog;
import org.oreland.ui.DialogBuilder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;


/**
 * Created by jonas on 10/1/15.
 */
public class MyClub extends DefaultSynchronizer {

    public static final String CHARSET = "utf-8";
    public static final String START_URL = "https://www.myclub.se/";
    public static final String BASE_URL = "https://member.myclub.se";
    public static final String LOGIN_URL = "https://accounts.myclub.se/d/users/sign_in"; //.json";
    public static final String CALENDAR_URL = BASE_URL + "/activities/team/show/";
    public static final String PARTICIPANTS_URL = BASE_URL + "/activities/team/view_parts/";
    public static final String INVITATIONS_URL = BASE_URL + "/activities/team/view_invited/";
    public static final String DESCRIPTION_URL = BASE_URL + "/activities/team/edit_info/";
    public static final String PLAYER_URL = BASE_URL + "/ut/team/";

    long id = 0;
    private String username = null;
    private String password = null;
    private String authToken = null;
    private Properties config = null;

    @Override
    public String getName() {
        return "MyClub";
    }

    @Override
    public void init(Properties config) {
        username = config.getProperty("username", null);
        password = config.getProperty("password", null);
    }

    @Override
    public void reset() {
        username = null;
        password = null;
        authToken = null;
    }

    private void addRequestHeaders(HttpURLConnection conn) {
        conn.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.addRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0; Trident/5.0)");
        conn.addRequestProperty("Accept-Encoding", CHARSET);
        conn.addRequestProperty("Accept-Language", "en-US,en;q=0.5");
        conn.addRequestProperty("Connection", "keep-alive");
        conn.addRequestProperty("Accept-Charset", CHARSET);
    }

    public Status login(Properties config, DialogBuilder builder) {
        System.out.println("Login...");
        for (int i = 0; i < 3; i++) {
            Status s = connect();
            if (s == Status.NEED_AUTH) {
                builder.setQuestion("Username: ");
                Dialog.Result result = builder.build().show();
                if (result != Dialog.Result.OK)
                    return s;
                username = result.stringResult;
                builder.setQuestion("Password: ");
                result = builder.build().show();
                if (result != Dialog.Result.OK)
                    return s;
                password = result.stringResult;
            } else if (s == Status.OK) {
                config.put("username", username);
                config.put("password", password);
                break;
            } else {
                return s;
            }
        }
        return Status.ERROR;
    }

    public Status setup(Properties config, DialogBuilder builder) {
        login(config, builder);
        Document doc;
        try {
            if (!config.containsKey("club")) {
                Pair<String,String> club = select("Select club: ", builder, BASE_URL, "a[href*=change_club]");
                config.setProperty("clubName", club.first);
                config.setProperty("club", club.second);
            }
            setClubEx(config.getProperty("clubName"), config.getProperty("club"));
        } catch (IOException e) {
            e.printStackTrace();
            return Status.ERROR;
        }
        try {
            if (!config.containsKey("section")) {
                Pair<String,String> section = select("Select section: ", builder, BASE_URL, "a[href*=change_section]");
                config.setProperty("sectionName", section.first);
                config.setProperty("section", section.second);
            }
            setSectionEx(config.getProperty("sectionName"), config.getProperty("section"));
        } catch (IOException e) {
            e.printStackTrace();
            return Status.ERROR;
        }
        try {
            if (!config.containsKey("team")) {
                Pair<String,String> team = select("Select team: ", builder, BASE_URL, "a[href*=change_team]");
                config.setProperty("teamName", team.first);
                config.setProperty("team", team.second);
                String teamno = team.second.substring(team.second.indexOf('=') + 1);
                config.setProperty("teamno", teamno);
            }
            setTeamEx(config.getProperty("teamName"), config.getProperty("team"));
        } catch (IOException e) {
            e.printStackTrace();
            return Status.ERROR;
        }

        try {
            if (!config.containsKey("period")) {
                // matches HT-2xxx and VT-2xxx
                Pair<String,String> period = select("Select period: ", builder, CALENDAR_URL, "a[href*=T-2]");
                config.setProperty("periodName", period.first);
                config.setProperty("period", period.second);
            }
            setPeriodEx(config.getProperty("periodName"), config.getProperty("period"));
        } catch (IOException e) {
            e.printStackTrace();
            return Status.ERROR;
        }

        try {
            if (!config.containsKey("Spelarinfo")) {
                doc = get(PLAYER_URL + config.getProperty("teamno") + "/members/");
                Element element = doc.select("p:contains(Spelarinformation) > *").first();
                String spelarinfo_field = element.attr("value");
                config.put("Spelarinfo", spelarinfo_field);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Status.ERROR;
        }

        this.config = config;
        return Status.OK;
    }

    public void setClubEx(String name, String key) throws IOException {
        System.out.println("Loading club: " + name);
        Document doc = get(BASE_URL + key);
    }

    public void setSectionEx(String name, String key) throws IOException {
        System.out.println("Loading section: " + name);
        Document doc = get(BASE_URL + key);
    }

    public void setTeamEx(String name, String key) throws IOException {
        System.out.println("Loading team: " + name);
        Document doc = get(BASE_URL + key);
    }

    public void setPeriodEx(String name, String key) throws IOException {
        System.out.println("Loading period: " + name);
        Document doc = get(BASE_URL + key);
    }

    private Pair<String, String> select(String prompt, DialogBuilder builder, String baseUrl, String query) throws IOException {
        Document doc = get(baseUrl);
        Set<String> values = new HashSet<>();
        List<String> choices = new ArrayList<>();
        List<String> choiceValues = new ArrayList<>();
        for (Element e : doc.select(query)) {
            String link = e.attr("href");
            String value = e.text();
            if (!values.contains(value)) {
                values.add(value);
                choices.add(value);
                choiceValues.add(link);
            }
        }

        builder.setQuestion(prompt);
        builder.setChoices(choices);
        Dialog.Result result = builder.build().show();
        return new Pair<>(choices.get(result.intResult - 1),
                choiceValues.get(result.intResult - 1));
    }

    public List<Pair<String,String>> selectMulti(String prompt, DialogBuilder builder, String baseUrl, String query) throws IOException {
        Document doc = get(baseUrl);
        Set<String> values = new HashSet<>();
        List<String> choices = new ArrayList<>();
        List<String> choiceValues = new ArrayList<>();
        for (Element e : doc.select(query)) {
            String link = e.attr("href");
            String value = e.text();
            if (!values.contains(value)) {
                values.add(value);
                choices.add(value);
                choiceValues.add(link);
            }
        }

        builder.setQuestion(prompt);
        builder.setChoices(choices);
        Dialog.Result result = builder.build().show();
        List<Pair<String, String>> returnValue = new ArrayList<>();
        for (int res : result.intResults) {
            returnValue.add(new Pair<String,String>(choices.get(res-1), choiceValues.get(res - 1)));
        }
        return returnValue;
    }

    private Document get(String baseUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl).openConnection();
        conn.setInstanceFollowRedirects(false);
        addRequestHeaders(conn);
        conn = send(baseUrl, conn);
        Document doc = Jsoup.parse(conn.getInputStream(), "UTF-8", baseUrl);
        conn.disconnect();
        return doc;
    }

    private JSONArray getJsonArray(String baseUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl).openConnection();
        conn.setInstanceFollowRedirects(false);
        addRequestHeaders(conn);
        conn = send(baseUrl, conn);
        JSONArray doc = SyncHelper.parseJsonArray(conn.getInputStream());
        conn.disconnect();
        return doc;
    }

    @Override
    public Status connect() {
        Exception ex = null;
        HttpURLConnection conn = null;
        formValues.clear();

        Status s = Status.NEED_AUTH;
        s.authMethod = AuthMethod.USER_PASS;
        if (username == null || password == null) {
            return s;
        }

        cookies.clear();

        try {
            /**
             * connect to START_URL to get cookies/formValues
             */
            conn = (HttpURLConnection) new URL(START_URL).openConnection();
            conn.setInstanceFollowRedirects(false);
            addRequestHeaders(conn);
            {
                conn = send(START_URL, conn);
            }
            conn.disconnect();

            /**
             * connect to BASE_URL to get cookies/formValues
             */
            conn = (HttpURLConnection) new URL(BASE_URL).openConnection();
            conn.setInstanceFollowRedirects(false);
            addRequestHeaders(conn);
            {
                conn = send(BASE_URL, conn);
                getFormValues(conn);
                authToken = formValues.get("authenticity_token");
            }
            conn.disconnect();

            if (authToken == null) {
                return Status.ERROR;
            }

            /**
             * Then login using a post
             */
            FormValues kv = new FormValues();
            kv.put("user[email]", username);
            kv.put("user[password]", password);
            kv.put("authenticity_token", authToken);
            kv.put("utf8", "&#x2713;");
            kv.put("user[remember_me]", "1");
            kv.put("commit", "Logga in");

            conn = (HttpURLConnection) new URL(LOGIN_URL).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setDoOutput(true);
            conn.setRequestMethod(RequestMethod.POST.name());
            addRequestHeaders(conn);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            conn.setRequestProperty("Host", "accounts.myclub.se");
            conn.setRequestProperty("Referer", "https://member.myclub.se");

            {
                OutputStream wr = new BufferedOutputStream(
                        conn.getOutputStream());
                kv.write(wr);
                wr.flush();
                wr.close();
                send(LOGIN_URL, conn);
            }

            conn = (HttpURLConnection) new URL(BASE_URL).openConnection();
            conn.setInstanceFollowRedirects(false);
            addRequestHeaders(conn);
            {
                conn = send(START_URL, conn);
                String html = SyncHelper.readInputStream(conn.getInputStream());
                conn.disconnect();
                Document doc = Jsoup.parse(html);
                if (doc.select("a[href*=/user/logout]").size() == 0) {
                    return Status.NEED_AUTH;
                }
            }

            return Status.OK;
        } catch (MalformedURLException e) {
            ex = e;
        } catch (Exception e) {
            ex = e;
        }

        if (conn != null)
            conn.disconnect();

        s.ex = ex;
        if (ex != null) {
            ex.printStackTrace();
        }
        return s;
    }

    HttpURLConnection send(String startUrl, HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                String newUrl = conn.getHeaderField("Location");
                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                addRequestHeaders(conn);
                return send(newUrl, conn);
            }
        }
        return conn;
    }

    public void loadPlayers(Repository repo) throws IOException {
        System.out.println("Load player list");
        String key = config.getProperty("Spelarinfo");
        JSONArray players = getJsonArray(PLAYER_URL + config.getProperty("teamno") + "/members/json");
        for (int i = 0; i < players.length(); i++) {
            JSONObject o = players.getJSONObject(i);
            Player p = new Player(o.getString("first_name"), o.getString("last_name"));
            p.type = o.getBoolean("is_leader") ? Player.Type.LEADER : Player.Type.PLAYER;
            p = repo.add(p);
            String targetstr = o.optString(key);
            TargetLevel level = TargetLevel.parseJson(repo, targetstr);
            if (level != null) {
                repo.addTarget(p, level, Calendar.getInstance().getTime());
            }
        }
    }

    public void loadActivities(Repository repo) throws IOException, ParseException {
        System.out.println("Load activities list");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -1);
        Date today = (Date) c.getTime().clone();
        c.add(Calendar.DAY_OF_YEAR, 8);
        Date limit = c.getTime();

        Document doc = get(BASE_URL + config.getProperty("period"));
        Element table = doc.select("table[id=grid_activities_table]").first();
//  0  <td>Tr?ning</td>
//  1  <td><span class="hidden">20150930</span>30/09</td>
//  2  <td class="visible-lg">Ons</td>
//  3  <td>17:00&nbsp;-&nbsp;18:00</td>
//  4  <td class="visible-lg visible-md">Beckombergahallen kl. 16:45</td>
//  5  <td class="visible-lg">Tr?ning</td>
//  6  <td class="visible-lg" title="Annan">Annan</td>
//  7  <td class="visible-lg visible-md"><a class="tool-tip" title="Ledare/Deltagare" href="/activities/team/view_parts/746587/">2/15</a></td>
//  8  <td class="visible-lg visible-md"><a class="tool-tip" title="Kallad (Ja/Nej/Kanske)" href="/activities/team/view_invited/746587/">46 (11/16/1)</a></td>
//  9  <td class="visible-lg visible-md">Godk?nd</td>
//  10  <td class="button_cell"><a href="/activities/team/edit/746587/" class="btn btn-default btn-xs">V?lj</a></td>
//  11  <td class="button_cell"><a class="btn btn-xs btn-danger" href=#onclick="$.fn.do_confirm('/activities/team/delete/746587/');"><i class="fa fa-trash"></i></a></td>
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        for (Element row : table.select("tr")) {
            Elements columns = row.select("td");
            if (columns.size() == 0)
                continue;
            if (columns.size() < 6)
                continue;
            String type = columns.get(5).text();
            Activity activity = null;
            String desc = columns.get(0).text();
            String date = columns.get(1).text().substring(0, 8);
            String id = columns.get(7).select("a[href]").first().attr("href").replace("/activities/team/view_parts/", "").replace("/", "");
            if (type.equals("Match")) {
                activity = repo.add(new Activity(id, formatter.parse(date), desc, Activity.Type.GAME));
            } else if (type.matches("Tr.*ning")) {
                activity = repo.add(new Activity(id, formatter.parse(date), desc, Activity.Type.TRAINING));
            } else if (type.matches("Cup")) {
                activity = repo.add(new Activity(id, formatter.parse(date), desc, Activity.Type.CUP));
            } else if (type.matches("F.*rfr.*gan")) {
                activity = repo.add(new Activity(id, formatter.parse(date), desc, Activity.Type.REQUEST));
            }
            if (activity != null) {
                activity.time = columns.get(3).text().replace('\u00a0', ' ');
            }

            if (activity != null && (activity.type == Activity.Type.GAME || activity.type == Activity.Type.CUP) && activity.level == null) {
                System.err.println("Loading level for " + activity.toString());
                loadActivityLevel(repo, activity);
            }
            if (activity != null && activity.synced == false && activity.date.before(limit)) {
                System.err.println("Loading invitations/participants for " + activity.toString());
                loadInvitations(repo, activity);
                loadParticipants(repo, activity);
                if (activity.date.before(today)) {
                    String status = columns.get(9).text();
                    if (status.matches(".*[Gg]odk.*nd") || status.matches(".*Delvis godk.*")) {
                        System.out.println(activity + " is complete.");
                        activity.synced = true;
                    }
                }
            }
        }
    }

    private void loadActivityLevel(Repository repo, Activity activity) throws IOException {
        Document doc = get(getDescriptionUrl(activity));
        String description = doc.select("textarea[id=id_description]").first().text();
        Set<Level> match = new HashSet<>();
        for (Level l : repo.getLevels()) {
            if (description.contains(l.name)) {
                match.add(l);
            }
        }
        if (match.size() == 1) {
            activity.level = match.iterator().next();
        } else {
            // Could not match level, need to ask user!
            activity.description = description;
        }
    }

    private void loadParticipants(Repository repo, Activity activity) throws IOException {
        activity.participants.clear();
        Document doc = get(getParticipantsUrl(activity));
        String tables[] = new String[]{"participants_table", "leaders_table"};
        Player.Type types[] = new Player.Type[]{Player.Type.PLAYER, Player.Type.LEADER};
        for (int i = 0; i < tables.length; i++) {
            String key = tables[i];
            Element table = doc.select("table[id=" + key + "]").first().select("tbody").first();
            for (Element row : table.select("tr")) {
                Elements columns = row.select("td");
                if (columns.size() < 6)
                    continue;

// <tr>
// <td>Albin</td>
// <td>Str?mgren</td>
// <td>20061102xxxx</td>
// <td>Godk?nd</td>
// <td></td>
// <td></td>
// </tr>
                Player p = new Player(columns.get(0).text(), columns.get(1).text());
                p.type = types[i];
                if (repo.getPlayer(p.first_name, p.last_name) == null) {
                    // Player that participate but not's in player list is guests
                    p.guest = true;
                }
                repo.addParticipant(activity, p);
            }
        }
    }

    public void loadInvitations(Repository repo, Activity activity) throws IOException, ParseException {
        activity.invitations.clear();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:ss");
        Document doc = get(getInvitationsUrl(activity));
        Element table = doc.select("table[id=invited_table]").first().select("tbody").first();
        for (Element row : table.select("tr")) {
            Elements columns = row.select("td");
            if (columns.size() < 6)
                continue;
//          <tr>
//       0     <td class=" sorting_1">Alfred</td>
//       1     <td class=" ">Wettergren</td>
//       2     <td class="hidden-xs ">Deltagare</td>
//       3     <td class="visible-lg visible-md "><p class="btn btn-xs tool-tip pop-over" title="" data-content="mail.address@somewhere.org" data-toggle="popover" href="#" data-original-title=""></p></td>
//       4     <td class="hidden-xs ">1</td>
//       5     <td class="visible-lg ">2015-10-08 12:25</td>
//       6     NEXT MAIL <td class="visible-lg "></td>
//       7     <td class=" ">
//            <div class="btn-group" data-toggle="buttons-radio">
//            <button class="respond btn active btn-success btn-xs" data-response="yes" data-id="1648554" type="button">Ja</button>
//            <button class="respond btn btn-default btn-xs" btn-default="" data-response="maybe" data-id="1648554" type="button">Kanske</button>
//            <button class="respond btn btn-default btn-xs" data-response="no" data-id="1648554" type="button">Nej</button>
//            </div>
//            </td>
//       8     <td class="visible-lg visible-md ">2015-10-08 14:38</td>
//       9     <td class="button_cell "></td>
//          </tr>

            // If no invitation is sent...skip player
            if (columns.get(5).text().isEmpty())
                continue;

            int response_col = 7;
            int response_date_col = 8;
            if (columns.size() == 9) {

            } else if (columns.size() == 8) {
                // Activity completed
                response_col = 6;
                response_date_col = 7;
            }
            Player p = new Player(columns.get(0).text(), columns.get(1).text());
            p.type = Player.Type.parse(columns.get(2).text());
            if (repo.getPlayer(p.first_name, p.last_name) == null) {
                continue;
            }
            Activity.Invitation invitation = new Activity.Invitation();
            invitation.player = repo.add(p);
            if (!columns.get(5).text().isEmpty())
                invitation.invitation_date = formatter.parse(columns.get(5).text());
            if (!columns.get(response_date_col).text().isEmpty()) {
                invitation.response_date = formatter.parse(columns.get(response_date_col).text());
                Element col = columns.get(response_col);
                Element comment = col.select("p").first();
                if (comment != null) {
                    invitation.response_comment = comment.attr("data-content");
                }
                for (Element e : col.select("button")) {
                    if (e.attr("class").contains("active")) {
                        invitation.response = Activity.Response.parse(e.attr("data-response"));
                    }
                }
                if (invitation.response == null)
                    throw new ParseException("Unable to find response in: " + col.toString(), 0);
            }
            repo.addInvitation(activity, invitation);
        }
    }

    private String getDescriptionUrl(Activity activity) {
        return DESCRIPTION_URL + activity.id + "/";
    }

    private String getParticipantsUrl(Activity activity) {
        return PARTICIPANTS_URL + activity.id + "/";
    }

    private String getInvitationsUrl(Activity activity) {
        return INVITATIONS_URL + activity.id + "/";
    }

    @Override
    public void logout() {
        authToken = null;
        super.logout();
    }
}
