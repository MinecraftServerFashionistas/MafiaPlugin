package me.aplicable.sckm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

public class SQLManager {

	@SuppressWarnings("unused")
	private SCKillManager killManager;

	private Connection connection;
	private String host;
	private String username;
	private String password;
	private String database;

	public SQLManager(SCKillManager killManager){
		this.killManager=killManager;

		FileConfiguration config = killManager.getConfig();
		this.host=config.getString("host");
		this.username=config.getString("username");
		this.password=config.getString("password");
		this.database=config.getString("database");
		establishConnection();
		prepareDatabase();
	}

	private void establishConnection(){
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			this.connection=DriverManager.getConnection("jdbc:mysql://"+this.host+":3306/"+database,this.username,this.password);
		}catch(SQLException e){
			Logger.getLogger("Minecraft.SCKillManager").severe(e.getMessage());
		}catch(ClassNotFoundException e){
			Logger.getLogger("Minecraft.SCKillManager").severe(e.getMessage());
		}catch(IllegalAccessException e){
			Logger.getLogger("Minecraft.SCKillManager").severe(e.getMessage());
		}catch(InstantiationException e){
			Logger.getLogger("Minecraft.SCKillManager").severe(e.getMessage());
		}
	}

	private void prepareDatabase(){
		try{
			Statement query = connection.createStatement();
			query.executeUpdate("CREATE TABLE IF NOT EXISTS KILLSTREAK (NAME VARCHAR(40),STREAK INT)");
		}catch(SQLException e){
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
			Logger.getLogger("Minecraft.SCKillManager").severe(e.getMessage());
			Logger.getLogger("Minecraft.SCKillManager").severe("Error in Database prep.");
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
		}
	}
	
	public boolean playerPresent(String playername){
		try{
			Statement query = connection.createStatement();
			ResultSet set = query.executeQuery("SELECT * FROM KILLSTREAK WHERE NAME='"+playername+"'");
			return set.first();
		}catch(SQLException e){
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
			Logger.getLogger("Minecraft.SCKillManager").severe(e.getMessage());
			Logger.getLogger("Minecraft.SCKillManager").severe("Error in finding Player.");
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
		}
		return false;
	}

	public void preparePlayer(String playername){
		try{
			Statement query = connection.createStatement();
			query.executeUpdate("INSERT INTO KILLSTREAK VALUES ('"+playername+"',0)");
		}catch(SQLException e){
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
			Logger.getLogger("Minecraft.SCKillManager").severe(e.getMessage());
			Logger.getLogger("Minecraft.SCKillManager").severe("Error in Player prep.");
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
		}
	}

	public void incrementKillstreak(String playername){
		try{
			Statement query = connection.createStatement();
			int killstreak = fetchKillstreak(playername);
			query.executeUpdate("UPDATE KILLSTREAK SET STREAK="+(killstreak+1)+" WHERE NAME='"+playername+"'");
		}catch(SQLException e){
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
			Logger.getLogger("Minecraft.SCKillManager").severe(e.getMessage());
			Logger.getLogger("Minecraft.SCKillManager").severe("Error in Updating Streak");
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
		}
	}

	public void resetKillstreak(String playername){
		try{
			Statement query = connection.createStatement();
			query.executeUpdate("UPDATE KILLSTREAK SET STREAK=0 WHERE NAME='"+playername+"'");
		}catch(SQLException e){
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
			Logger.getLogger("Minecraft.SCKillManager").severe(e.getMessage());
			Logger.getLogger("Minecraft.SCKillManager").severe("Error in Resetting Streak");
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
		}
	}

	public int fetchKillstreak(String playername){
		try{
			Statement query = connection.createStatement();
			ResultSet set = query.executeQuery("SELECT STREAK FROM KILLSTREAK WHERE NAME='"+playername+"'");
			set.next();
			int killstreak = set.getInt(1);
			return killstreak;
		}catch(SQLException e){
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
			Logger.getLogger("Minecraft.SCKillManager").severe(e.getMessage());
			Logger.getLogger("Minecraft.SCKillManager").severe("Error in Retrieving Streak");
			Logger.getLogger("Minecraft.SCKillManager").severe("~~~~~~~~~~~~~~~~~~~~~~~");
		}
		return -1;
	}
}
