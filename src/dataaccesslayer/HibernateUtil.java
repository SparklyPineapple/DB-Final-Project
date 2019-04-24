package dataaccesslayer;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.Stoppable;

import bo.Player;
import bo.Team;


public class HibernateUtil {

	private static final SessionFactory sessionFactoryP, sessionFactoryT;

	static {
		try {
			System.out.println("player configuration");
			
			Configuration cfg = new Configuration()
				.addAnnotatedClass(bo.Player.class)
				.addAnnotatedClass(bo.PlayerSeason.class)
				.addAnnotatedClass(bo.BattingStats.class)
				.addAnnotatedClass(bo.CatchingStats.class)
				.addAnnotatedClass(bo.FieldingStats.class)
				.addAnnotatedClass(bo.PitchingStats.class)
				.addAnnotatedClass(bo.TeamSeason.class)
				.addAnnotatedClass(bo.Team.class)
				.configure();
			StandardServiceRegistryBuilder builderP = new StandardServiceRegistryBuilder().
			applySettings(cfg.getProperties());
			sessionFactoryP = cfg.buildSessionFactory(builderP.build());
			
		} catch (Throwable ex) {
			System.err.println("Initial SessionFactoryPlayer creation failed." + ex);
			ex.printStackTrace();
			throw new ExceptionInInitializerError(ex);
			
		}
	}

	
	static {
		try {	
			Configuration cfgT = new Configuration()
					.addAnnotatedClass(bo.Team.class)
					.addAnnotatedClass(bo.TeamSeason.class)
					.configure();
				StandardServiceRegistryBuilder builderT = new StandardServiceRegistryBuilder().
				applySettings(cfgT.getProperties());
			sessionFactoryT = cfgT.buildSessionFactory(builderT.build());
			
		} catch (Throwable ex) {
			System.err.println("Initial SessionFactoryTeam creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}

	}
	
	
	public static SessionFactory getSessionFactory() {
		return sessionFactoryP;
	}
	
	public static SessionFactory getSessionFactoryT() {
		return sessionFactoryT;
	}
  
  public static void stopConnectionProvider() {
    final SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) sessionFactoryP;
    ConnectionProvider connectionProvider = sessionFactoryImplementor.getConnectionProvider();
    if (Stoppable.class.isInstance(connectionProvider)) {
        ((Stoppable) connectionProvider).stop();
    }        
}
	
	public static Player retrievePlayerById(Integer id) {
        Player p=null;
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.getTransaction();
		try {
			tx.begin();
			org.hibernate.Query query;
			query = session.createQuery("from bo.Player where id = :id ");
		    query.setParameter("id", id);
		    if (query.list().size()>0) {
		    	p = (Player) query.list().get(0);
		    	Hibernate.initialize(p.getSeasons());
		    }
			tx.commit();
		} catch (Exception e) {
			tx.rollback();
			e.printStackTrace();
		} finally {
			if (session.isOpen()) session.close();
		}
		return p;
	}
	
	
	@SuppressWarnings("unchecked")
	public static List<Player> retrievePlayersByName(String nameQuery, Boolean exactMatch) {
        List<Player> list=null;
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.getTransaction();
		try {
			tx.begin();
			org.hibernate.Query query;
			if (exactMatch) {
				query = session.createQuery("from bo.Player where name = :name ");
			} else {
				query = session.createQuery("from bo.Player where name like '%' + :name + '%' ");
			}
		    query.setParameter("name", nameQuery);
		    list = query.list();
			tx.commit();
		} catch (Exception e) {
			tx.rollback();
			e.printStackTrace();
		} finally {
			if (session.isOpen()) session.close();
		}
		return list;
	}

	//TODO 
	public static Player retrieveTeamById(Integer id){
		
	}

	//TODO public static List<Team> retrieveTeamByName(String nameQuery, Boolean exactMatch)
	
	public static boolean persistPlayer(Player p) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.getTransaction();
		try {
			tx.begin();
			session.save(p);
			tx.commit();
		} catch (Exception e) {
			tx.rollback();
			e.printStackTrace();
			return false;
		} finally {
			if (session.isOpen()) session.close();
		}
		return true;
	}
	
	public static boolean persistTeam(Team t) {
		Session session = HibernateUtil.getSessionFactoryT().openSession();
		Transaction tx = session.getTransaction();
		try {
			tx.begin();
			session.save(t);
			tx.commit();
		} catch (Exception e) {
			tx.rollback();
			e.printStackTrace();
			return false;
		} finally {
			if (session.isOpen()) session.close();
		}
		return true;
	}
		
}