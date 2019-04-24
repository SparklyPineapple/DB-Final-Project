package bo;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@SuppressWarnings("serial")
@Entity(name = "teamseason")
public class TeamSeason implements Serializable {
	
	@EmbeddedId
	TeamSeasonId id;
	
	@Embeddable
	static class TeamSeasonId implements Serializable {
		@ManyToOne
		@JoinColumn(name = "teamid", referencedColumnName = "teamid", insertable = false, updatable = false)
		Team team;
		@Column(name="year")
		Integer teamYear;
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof TeamSeasonId)){
				return false;
			}
			TeamSeasonId other = (TeamSeasonId)obj;
			// in order for two different object of this type to be equal,
			// they must be for the same year and for the same player
			return (this.team==other.team &&
					this.team==other.team);
		}
		
		
			@Override
			public int hashCode() {
				Integer hash = 0;
				if (this.team != null) hash += this.team.hashCode();
				if (this.team != null) hash += this.team.hashCode();
				return hash;
			}
		}
		
		@Column
		Integer gamesPlayed;
		@Column
		Integer wins;
		@Column
		Integer losses;
		@Column
		Integer rank;
		@Column
		Integer totalAttendance;
			
		
		
		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(name = "teamseasonplayer", 
		   joinColumns={
		     @JoinColumn(name="teamId", insertable = false, updatable = false), 
		     @JoinColumn(name="year",  insertable = false, updatable = false)}, 
		   inverseJoinColumns={
		     @JoinColumn(name="playerId", insertable = false, updatable = false)})
		Set<Player> players = new HashSet<Player>();
		
		
		// utility function
		public Player getPlayers(Player p) {
			for (Player pl : players) {
				if (pl.getId().equals(p.getId())) return pl;
			}
			return null;
		}
		
		public void addPlayers(Player p) {
			players.add(p);
		}

		public Set<Player> getPlayers() {
			return players;
		}
		
		public void setPlayers(Set<Player> players) {
			this.players = players;
		}	

		
		// Hibernate needs a default constructor
		public TeamSeason() {}

		public TeamSeason(Team t, Integer year) {
			TeamSeasonId tsi = new TeamSeasonId();
			tsi.team = t;
			tsi.teamYear = year;
			this.id = tsi;
		}
	
		public void setYear(Integer year) {
			this.id.teamYear = year;
		}

		public Integer getYear() {
			return this.id.teamYear;
		}

		public Team getTeam() {
			return this.id.team;
		}

		public void setTeam(Team team) {
			this.id.team = team;
		}

		public TeamSeasonId getId() {
			return this.id;
		}

		public Integer getGamesPlayed() {
			return gamesPlayed;
		}

		public void setGamesPlayed(Integer gamesPlayed) {
			this.gamesPlayed = gamesPlayed;
		}

		public int getWins() {
			return wins;
		}

		public void setWins(Integer wins) {
			this.wins = wins;
		}

		public int getLosses() {
			return losses;
		}

		public void setLosses(Integer losses) {
			this.losses = losses;
		}		
		
		public int getRank() {
			return rank;
		}

		public void setRank(Integer rank) {
			this.rank = rank;
		}
		
		public int getTotalAttendance() {
			return totalAttendance;
		}

		public void setTotalAttendance(Integer totalAttendance) {
			this.totalAttendance = totalAttendance;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof TeamSeason)){
				return false;
			}
			TeamSeason other = (TeamSeason)obj;
			return other.getId().equals(this.getId());
		}
		 
		@Override
		public int hashCode() {
			return this.getId().hashCode();
		}

		public static Comparator<TeamSeason> TeamSeasonsComparator = new Comparator<TeamSeason>() {

			public int compare(TeamSeason ts1, TeamSeason ts2) {
				Integer year1 = ts1.getYear();
				Integer year2 = ts2.getYear();
				return year1.compareTo(year2);
			}

		};
		
}
