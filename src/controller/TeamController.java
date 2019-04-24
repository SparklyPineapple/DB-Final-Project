package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import view.TeamView;
import bo.Team;
import bo.TeamSeason;
import dataaccesslayer.HibernateUtil;

public class TeamController extends BaseController {

    @Override
    public void init(String query) {
        System.out.println("building dynamic html for team");
        view = new TeamView();
        process(query);
    }
    
    @Override
    protected void performAction() {
        String action = keyVals.get("action");
        System.out.println("teamcontroller performing action: " + action);
        if (action.equalsIgnoreCase(ACT_SEARCHFORM)) {
            processSearchForm();
        } else if (action.equalsIgnoreCase(ACT_SEARCH)) {
            processSearch();
        } else if (action.equalsIgnoreCase(ACT_DETAIL)) {
            processDetails();
        } else if (action.equalsIgnoreCase(ACT_ROSTER)){//TODO: tentatively done, untested
            processRoster();
        }
    }

    protected void processSearchForm() {
        view.buildSearchForm();
    }
    
    protected final void processSearch() {
        String name = keyVals.get("name");
        if (name == null) {
            return;
        }
        String v = keyVals.get("exact");
        boolean exact = (v != null && v.equalsIgnoreCase("on"));
        List<Team> bos = HibernateUtil.retrieveTeamsByName(name, exact);
        view.printSearchResultsMessage(name, exact);
        buildSearchResultsTableTeam(bos);
        view.buildLinkToSearch();
    }

    protected final void processDetails() {
        String id = keyVals.get("id");
        if (id == null) {
            return;
        }
        Team t= (Team) HibernateUtil.retrieveTeamById(Integer.valueOf(id));
        if (t== null) return;
        buildSearchResultsTableTeamDetail(t);
        view.buildLinkToSearch();
    }

    protected final void processRoster(){//TODO
        String tid = keyVals.get("tid");
        String yid = keyVals.get("yid");
        if (tid == null|| yid == null) {
            return;
        }
        Team t= (Team) HibernateUtil.retrieveTeamById(Integer.valueOf(id));
         if (t== null) return;
        TeamSeason ts = t.getTeamSeason(Integer.valueOf(year));
        if(ts== null) return;
        String teamName = t.getName();
        if(teamName == null) teamName = "";
         buildSearchResultsTableTeamRoster(ts teamName);

    }


    private void buildSearchResultsTableTeam(List<Team> bos) {//TODO: tentatively done
        // need a row for the table headers
        String[][] table = new String[bos.size() + 1][5];
        table[0][0] = "ID";
        table[0][1] = "Name";
        table[0][2] = "League";
        table[0][3] = "Year Founded";
        table[0][4] = "Most Recent Year";
        for (int i = 0; i < bos.size(); i++) {
            Team t = bos.get(i);
            String tid = t.getId().toString();
            table[i + 1][0] = view.encodeLink(new String[]{"id"}, new String[]{tid}, tid, ACT_DETAIL, SSP_TEAM);
            table[i + 1][1] = t.getName();
            table[i + 1][2] = t.getLeague();
            table[i + 1][3] = t.getYearFounded().toString();
            table[i + 1][4] = t.getYearLast().toString();
        }
        view.buildTable(table);
    }
    
    private void buildSearchResultsTableTeamDetail(Team t) {//TODO: tentatively done
    	Set<TeamSeason> seasons = t.getSeasons();
    	List<TeamSeason> list = new ArrayList<TeamSeason>(seasons);
    	Collections.sort(list, TeamSeason.TeamSeasonsComparator);
    	// build 2 tables.  first the team details, then the season details
        // need a row for the table headers
        String[][] teamTable = new String[2][4];
        teamTable[0][0] = "Name";
        teamTable[0][1] = "League";
        teamTable[0][2] = "Year Founded";
        teamTable[0][3] = "Most Recent Year";
        //Fill in basic table info
        teamTable[1][0] = t.getName();
        teamTable[1][1] = t.getLeague();
        teamTable[1][2] = t.getYearFounded().toString();
        teamTable[1][3] = t.getYearLast().toString();    
        view.buildTable(teamTable);

        // now for seasons
        String[][] seasonTable = new String[seasons.size()+1][7];
        seasonTable[0][0] = "Year";
        seasonTable[0][1] = "Games Played";
        seasonTable[0][2] = "Roster";
        seasonTable[0][3] = "Wins";
        seasonTable[0][4] = "Losses";
        seasonTable[0][5] = "Rank";
        seasonTable[0][6] = "Attendance";
        int i = 0;
        for (TeamSeason ts: list) {
        	i++;
            String tid = ts.getTeam().toString();
            String yid = ts.getYear().toString();
        	seasonTable[i][0] = ts.getYear().toString();
        	seasonTable[i][1] = ts.getGamesPlayed().toString();
        	seasonTable[i][2] = view.encodeLink(new String[]{"tid", "yid"}, new String[]{tid,yid}, "Roster", ACT_ROSTER, SSP_TEAM);//TODO tentatively done
        	seasonTable[i][3] = ts.getWins().toString();
        	seasonTable[i][4] = ts.getLosses().toString();
        	seasonTable[i][5] = ts.getRank().toString();
        	seasonTable[i][6] = ts.getTotalAttendance().toString();
        }
        view.buildTable(seasonTable);
    }

    private void buildSearchResultsTableTeamRoster(TeamSeason ts, String tName){//TODO tentatively done
        // build 2 tables.  first the team details, then the season details
        // need a row for the table headers
        String[][] teamTable = new String[2][4];
        teamTable[0][0] = "Name";
        teamTable[0][1] = "League";
        teamTable[0][2] = "Year";
        teamTable[0][3] = "Player Payroll";
        //Fill in basic table info
        teamTable[1][0] = tName;
        teamTable[1][1] = ts.getLeague();
        teamTable[1][2] = ts.getYear();
        teamTable[1][3] = "Player Payroll";  
        view.buildTable(teamTable);

        //now for Roster
        Set<Player> players = ts.getPlayers();
        String[][] playerTable = new String[players.size()+1][3];
        playerTable[0][0] = "Name";
        playerTable[0][1] = "Games Played";
        playerTable[0][2] = "Salary";
        int i = 0;
        PlayerSeason ps;
        for (Player p: list) {
        	i++;
            String pid = p.getId().toString();
            ps = p.getPlayerSeason(ts.getYear());
            if(ps == null){
                playerTable[i][0] = "playerSeason was null";
                playerTable[i][1] = "playerSeason was null";
                playerTable[i][2] = "playerSeason was null";
            }else{
                playerTable[i][0] = view.encodeLink(new String[]{"id"}, new String[]{pid}, p.getName();, ACT_DETAIL, SSP_PLAYER);
                playerTable[i][1] = ps.getGamesPlayed().toString();
                playerTable[i][2] = ps.getSalary().toString();
            }
        	
        }

        view.buildTable(playerTable);
    }

}
