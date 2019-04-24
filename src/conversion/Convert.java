package conversion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import bo.BattingStats;
import bo.CatchingStats;
import bo.FieldingStats;
import bo.PitchingStats;
import bo.Player;
import bo.PlayerSeason;
import bo.Team;
import bo.TeamSeason;
import dataaccesslayer.HibernateUtil;

public class Convert {

	static Connection conn;
	static final String MYSQL_CONN_URL = "jdbc:mysql://163.11.239.17:3306/mlb?"
			+ "verifyServerCertificate=false&useSSL=true&" // PPD
			+ "user=seth&password=seth";

	public static void main(String[] args) {
		try {
			long startTime = System.currentTimeMillis();
			conn = DriverManager.getConnection(MYSQL_CONN_URL);
			convertAll();

			long endTime = System.currentTimeMillis();
			long elapsed = (endTime - startTime) / (1000 * 60);
			System.out.println("Elapsed time in mins: " + elapsed);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (!conn.isClosed())
					conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		HibernateUtil.stopConnectionProvider(); // PPD
		HibernateUtil.getSessionFactory().close();
	}

	public static void convertAll() {
		System.out.println("convert All");

		ArrayList<Team> teamList = new ArrayList<Team>();
		ArrayList<TeamSeason> teamSeasonList = new ArrayList<TeamSeason>();
		
		convertTeams(teamList, teamSeasonList);

		teamSeasonList = convertPlayers(teamSeasonList);

		// LOOP and persist all teams in teamList
		for (Team t : teamList) {
			System.out.println("in loop");
			HibernateUtil.persistTeam(t);
		}

	}
	public static void makeTeamSeasonsForThisTeam(Team t, String tID_str, ArrayList<TeamSeason> TeamSeasonList) {
		try {
			PreparedStatement ps = conn.prepareStatement("CALL getRelevantTeamData( ? );"); //TODO stored Procedure change
			ps.setString(1, tID_str);
			ResultSet rs = ps.executeQuery();
			TeamSeason ts;
						
			
			String gamesPlayed_str, wins_str, losses_str, rank_str, totalAttendance_str, yearID_str;
			totalAttendance_str="";
			Integer yearID;
			while(rs.next()) {
				
				//Make a new TeamSeason based on year
				yearID_str = rs.getString("yearID");
				if(yearID_str != null) {
					yearID = Integer.parseInt(yearID_str);
				}else {
					yearID = 0;
				}
				ts = new TeamSeason(t, yearID);
				
				//retrieve remaining attributes adjusting as necessary and then initialize
				gamesPlayed_str = rs.getString("G");
				wins_str = rs.getString("W");
				losses_str = rs.getString("L");
				rank_str = rs.getString("Rank");
				totalAttendance_str = rs.getString("attendance");
				
				if(gamesPlayed_str == null || gamesPlayed_str.isEmpty()) {
					ts.setGamesPlayed(0);
				}else {
					ts.setGamesPlayed(Integer.parseInt(gamesPlayed_str));
				}
				
				if(wins_str == null || wins_str.isEmpty()) {
					ts.setWins(0);
				}else {
					ts.setWins(Integer.parseInt(wins_str));
				}
				
				if(losses_str == null || losses_str.isEmpty()) {
					ts.setLosses(0);
				}else {
					ts.setLosses(Integer.parseInt(losses_str));
				}
				
				if(rank_str == null || rank_str.isEmpty()) {
					ts.setRank(0);
				}else {
					ts.setRank(Integer.parseInt(rank_str));
				}
				
				if(totalAttendance_str == null || totalAttendance_str.isEmpty()) {
					ts.setTotalAttendance(0);
				}else {
					ts.setTotalAttendance(Integer.parseInt(totalAttendance_str));	
				}
				
				// add to arrayList
				TeamSeasonList.add(ts);
				
				
			}
			
			rs.close();
			ps.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static Team makeTeam(String teamIDStr) {
		try {
			PreparedStatement ps = conn.prepareStatement("CALL getRelevantTeamData( ? );");
			ps.setString(1, teamIDStr);
			ResultSet rs = ps.executeQuery();
			Team t = new Team();
			
			//string team ID -> integer teamID
			String interimSTR = "";
			char c;
			int asciiC;
			for(int i = 0; i< teamIDStr.length(); i++) {
				c = teamIDStr.charAt(i);
				asciiC = c;
				interimSTR= interimSTR+asciiC;
			}
			t.setId(Integer.parseInt(interimSTR));
			
			//initialize with erroneous data to insure something is set
			t.setYearFounded(999999);
			t.setYearLast(-1);
			String teamName, league, yearIDStr;
			Integer yearID;
			while(rs.next()) {//all the rest of the attributes are time dependent
				yearIDStr = rs.getString("yearID");
				yearID = Integer.parseInt(yearIDStr);
				if(yearID>t.getYearLast()) { //currently the most recent data
					t.setYearLast(yearID);
					teamName = rs.getString("name");
					league = rs.getString("lgID");
					t.setName(teamName);
					t.setLeague(league);
				}
				if(yearID<t.getYearFounded()) {
					t.setYearFounded(yearID);
				}
			}
			
			rs.close();
			ps.close();
			return t;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void convertTeams(ArrayList<Team> teamList, ArrayList<TeamSeason> teamSeasonList) {
		// Get a list of all team ID's
		try {
			PreparedStatement ps = conn.prepareStatement("CALL getTeamIDs();");
			ResultSet rs = ps.executeQuery();
			int teamCount = 0;
			String teamIDStr;
			Team t;
			while (rs.next()) {//for each team, make a team object and all TeamSeasons Objects for that 
				teamCount++;
				if (teamCount % 10 == 0) System.out.println("num players: " + teamCount);
				
				teamIDStr = rs.getString("teamID");
				t = makeTeam(teamIDStr);
				teamList.add(t);
				makeTeamSeasonsForThisTeam(t, teamIDStr, teamSeasonList);

			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	public static ArrayList<TeamSeason> convertPlayers(ArrayList<TeamSeason> teamSeasonList) {
		try {
			PreparedStatement ps = conn.prepareStatement("select " + "playerID, " + "nameFirst, " + "nameLast, "
					+ "nameGiven, " + "birthDay, " + "birthMonth, " + "birthYear, " + "deathDay, " + "deathMonth, "
					+ "deathYear, " + "bats, " + "throws, " + "birthCity, " + "birthState, " + "birthCountry, "
					+ "debut, " + "finalGame " +
					 "from Master");
					// for debugging comment previous line, uncomment next line
					//"from Master where playerID = 'bondsba01' or playerID = 'youklke01';");
			ResultSet rs = ps.executeQuery();
			int count = 0; // for progress feedback only

			while (rs.next()) {
				count++;
				// this just gives us some progress feedback
				if (count % 1000 == 0)
					System.out.println("num players: " + count);

				String pid = rs.getString("playerID");
				String firstName = rs.getString("nameFirst");
				String lastName = rs.getString("nameLast");
				// this check is for data scrubbing
				// don't want to bring anybody over that doesn't have a pid, firstname and
				// lastname
				if (pid == null || pid.isEmpty() || firstName == null || firstName.isEmpty() || lastName == null
						|| lastName.isEmpty())
					continue;
				Player p = new Player();
				p.setName(firstName + " " + lastName);
				p.setGivenName(rs.getString("nameGiven"));

				java.util.Date birthDay = convertIntsToDate(rs.getInt("birthYear"), rs.getInt("birthMonth"),
						rs.getInt("birthDay"));
				if (birthDay != null)
					p.setBirthDay(birthDay);
				java.util.Date deathDay = convertIntsToDate(rs.getInt("deathYear"), rs.getInt("deathMonth"),
						rs.getInt("deathDay"));
				if (deathDay != null)
					p.setDeathDay(deathDay);

				// need to do some data scrubbing for bats and throws columns
				String hand = rs.getString("bats");
				if (hand != null) {
					if (hand.equalsIgnoreCase("B")) {
						hand = "S";
					} else if (hand.equalsIgnoreCase(""))
						hand = null;
				}
				p.setBattingHand(hand);

				// Clean up throwing hand
				hand = rs.getString("throws");
				if (hand.equalsIgnoreCase("")) {
					hand = null;
				}
				p.setThrowingHand(hand);

				p.setBirthCity(rs.getString("birthCity"));
				p.setBirthState(rs.getString("birthState"));
				p.setBirthCountry(rs.getString("birthCountry"));

				// Clean up debut and final game data.
				try {
					java.util.Date firstGame = rs.getDate("debut");
					if (firstGame != null)
						p.setFirstGame(firstGame);
				} catch (SQLException e) {
					// Ignore conversion error - remains null;
					System.out.println(pid + ": debut invalid format");
				}
				try {
					java.util.Date lastGame = rs.getDate("finalGame");
					if (lastGame != null)
						p.setLastGame(lastGame);
				} catch (SQLException e) {
					// Ignore conversion error - remains null
					System.out.println(pid + ": finalGame invalid format");
				}

				addPositions(p, pid);
				addSeasons(p, pid);
				
				
				//attach players to teamSeason and teamSeason to players where they correspond
				makeTeamSeasonAssociationsWithPlayer(pid, p, teamSeasonList);
				
				// we can now persist player, and the seasons and stats will cascade
				HibernateUtil.persistPlayer(p); // persist indivdual player

			} //while loop ends. out of player tuples
			rs.close();
			ps.close();
		}catch(

	Exception e)
	{
		e.printStackTrace();
	}

	return teamSeasonList;
	}
	
	private static TeamSeason findTimeSeason(ArrayList<TeamSeason> teamSeasonList, Integer yearID, String teamID_str) {
		int teamID = teamStr2Int(teamID_str);
		for (TeamSeason ts: teamSeasonList) {
		if (ts.getYear() == yearID && ts.getTeam().getId() == teamID) { 
				return ts;
			}
		}
		return null;
	}
	
	private static void makeTeamSeasonAssociationsWithPlayer(String pid, Player p, ArrayList<TeamSeason> teamSeasonList) {
		try {
			//get all team season data associated with this player
			PreparedStatement ps = conn
					.prepareStatement("CALL getTeamSeasonsOfPlayer(?);");
			ps.setString(1, pid);
			ResultSet rs = ps.executeQuery();
			String yearID_s, teamID_s;
			int yearID;
			TeamSeason ts;
			while(rs.next()) {
				yearID_s = rs.getString("yearID");
				teamID_s = rs.getString("teamID");
				yearID = Integer.parseInt(yearID_s);
				ts = findTimeSeason(teamSeasonList, yearID, teamID_s);
				if(ts != null && p!= null) {
					p.addTeamSeason(ts);
					ts.addPlayers(p);
				}
			}
			
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

	
	private static int teamStr2Int(String teamIDStr) {
		String interimSTR = "";
		char c;
		int asciiC;
		for(int i = 0; i< teamIDStr.length(); i++) {
			c = teamIDStr.charAt(i);
			asciiC = c;
			interimSTR= interimSTR+asciiC;
		}
		return Integer.parseInt(interimSTR);
	}

	private static java.util.Date convertIntsToDate(int year, int month, int day) {
		Calendar c = new GregorianCalendar();
		java.util.Date d = null;
		// if year is 0, then date wasn't populated in MySQL database
		if (year != 0) {
			c.set(year, month - 1, day);
			d = c.getTime();
		}
		return d;
	}

	public static void addPositions(Player p, String pid) {
		Set<String> positions = new HashSet<String>();
		try {
			PreparedStatement ps = conn
					.prepareStatement("select " + "distinct pos " + "from Fielding " + "where playerID = ?;");
			ps.setString(1, pid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String pos = rs.getString("pos");
				positions.add(pos);
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		p.setPositions(positions);
	}

	public static void addSeasons(Player p, String pid) {
		try {
			PreparedStatement ps = conn
					.prepareStatement("select " + "yearID, " + "teamID, " + "lgId, " + "sum(G) as gamesPlayed "
							+ "from Batting " + "where playerID = ? " + "group by yearID, teamID, lgID;");
			ps.setString(1, pid);
			ResultSet rs = ps.executeQuery();
			PlayerSeason s = null;
			while (rs.next()) {
				int yid = rs.getInt("yearID");
				s = p.getPlayerSeason(yid);
				// it is possible to see more than one of these per player if he switched teams
				// set all of these attrs the first time we see this playerseason
				if (s == null) {
					s = new PlayerSeason(p, yid);
					p.addSeason(s);
					s.setGamesPlayed(rs.getInt("gamesPlayed"));
					double salary = getSalary(pid, yid);
					s.setSalary(salary);
					BattingStats batting = getBatting(s, pid, yid);
					s.setBattingStats(batting);
					FieldingStats fielding = getFielding(s, pid, yid);
					s.setFieldingStats(fielding);
					PitchingStats pitching = getPitching(s, pid, yid);
					s.setPitchingStats(pitching);
					CatchingStats catching = getCatching(s, pid, yid);
					s.setCatchingStats(catching);
					// set this the consecutive time(s) so it is the total games played regardless
					// of team
				} else {
					s.setGamesPlayed(rs.getInt("gamesPlayed") + s.getGamesPlayed());
				}
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static double getSalary(String pid, Integer yid) {
		double salary = 0;
		try {
			PreparedStatement ps = conn.prepareStatement("select " + "sum(salary) as salary " + "from Salaries "
					+ "where playerID = ? " + "and yearID = ? ;");
			ps.setString(1, pid);
			ps.setInt(2, yid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				salary = rs.getDouble("salary");
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return salary;
	}

	public static BattingStats getBatting(PlayerSeason psi, String pid, Integer yid) {
		BattingStats s = new BattingStats();
		try {
			PreparedStatement ps = conn.prepareStatement("select " + "" + "sum(AB) as atBats, " + "sum(H) as hits, "
					+ "sum(2B) as doubles, " + "sum(3B) as triples, " + "sum(HR) as homeRuns, "
					+ "sum(RBI) as runsBattedIn, " + "sum(SO) as strikeouts, " + "sum(BB) as walks, "
					+ "sum(HBP) as hitByPitch, " + "sum(IBB) as intentionalWalks, " + "sum(SB) as steals, "
					+ "sum(CS) as stealsAttempted " + "from Batting " + "where playerID = ? " + "and yearID = ? ;");
			ps.setString(1, pid);
			ps.setInt(2, yid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				s.setId(psi);
				s.setAtBats(rs.getInt("atBats"));
				s.setHits(rs.getInt("hits"));
				s.setDoubles(rs.getInt("doubles"));
				s.setTriples(rs.getInt("triples"));
				s.setHomeRuns(rs.getInt("homeRuns"));
				s.setRunsBattedIn(rs.getInt("runsBattedIn"));
				s.setStrikeouts(rs.getInt("strikeouts"));
				s.setWalks(rs.getInt("walks"));
				s.setHitByPitch(rs.getInt("hitByPitch"));
				s.setIntentionalWalks(rs.getInt("intentionalWalks"));
				s.setSteals(rs.getInt("steals"));
				s.setStealsAttempted(rs.getInt("stealsAttempted"));
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}

	public static FieldingStats getFielding(PlayerSeason psi, String pid, Integer yid) {
		FieldingStats s = new FieldingStats();
		try {
			PreparedStatement ps = conn.prepareStatement("select " + "sum(E) as errors, " + "sum(PO) as putOuts "
					+ "from Fielding " + "where playerID = ? " + "and yearID = ? ;");
			ps.setString(1, pid);
			ps.setInt(2, yid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				s.setId(psi);
				s.setErrors(rs.getInt("errors"));
				s.setPutOuts(rs.getInt("putOuts"));
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}

	public static PitchingStats getPitching(PlayerSeason psi, String pid, Integer yid) {
		PitchingStats s = new PitchingStats();
		try {
			PreparedStatement ps = conn.prepareStatement("select " + "sum(IPOuts) as outsPitched, "
					+ "sum(ER) as earnedRunsAllowed, " + "sum(HR) as homeRunsAllowed, " + "sum(SO) as strikeouts, "
					+ "sum(BB) as walks, " + "sum(W) as wins, " + "sum(L) as losses, " + "sum(WP) as wildPitches, "
					+ "sum(BFP) as battersFaced, " + "sum(HBP) as hitBatters, " + "sum(SV) as saves " + "from Pitching "
					+ "where playerID = ? " + "and yearID = ? ;");
			ps.setString(1, pid);
			ps.setInt(2, yid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				s.setId(psi);
				s.setOutsPitched(rs.getInt("outsPitched"));
				s.setEarnedRunsAllowed(rs.getInt("earnedRunsAllowed"));
				s.setHomeRunsAllowed(rs.getInt("homeRunsAllowed"));
				s.setStrikeouts(rs.getInt("strikeouts"));
				s.setWalks(rs.getInt("walks"));
				s.setWins(rs.getInt("wins"));
				s.setLosses(rs.getInt("losses"));
				s.setWildPitches(rs.getInt("wildPitches"));
				s.setBattersFaced(rs.getInt("battersFaced"));
				s.setHitBatters(rs.getInt("hitBatters"));
				s.setSaves(rs.getInt("saves"));
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}

	public static CatchingStats getCatching(PlayerSeason psi, String pid, Integer yid) {
		CatchingStats s = new CatchingStats();
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("select " + "sum(PB) as passedBalls, " + "sum(WP) as wildPitches, "
					+ "sum(SB) as stealsAllowed, " + "sum(CS) as stealsCaught " + "from Fielding "
					+ "where playerID = ? " + "and yearID = ? ;");
			ps.setString(1, pid);
			ps.setInt(2, yid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				s.setId(psi);
				s.setPassedBalls(rs.getInt("passedBalls"));
				s.setWildPitches(rs.getInt("wildPitches"));
				s.setStealsAllowed(rs.getInt("stealsAllowed"));
				s.setStealsCaught(rs.getInt("stealsCaught"));
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			ps.toString();
			e.printStackTrace();
		}
		return s;
	}

}
