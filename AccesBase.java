import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Classe AccesBase
 * S'occupe d'interragir avec la base de donnée
 * @author Mathieu BOIREAU, Thibault FOUCHET, Sébastien PRUNIER
 * @version 2019-12-06
 */

public class AccesBase{
	private Connection conn;
	private Statement st;
	private PreparedStatement pStat;
	private PreparedStatement pShare;
	private PreparedStatement pDelete;

	public AccesBase(/*String log, String pass*/){
		try{
			Class.forName("org.postgresql.Driver");
		}
		catch (ClassNotFoundException e){
			e.printStackTrace();
		}

		try{
			conn = DriverManager.getConnection("jdbc:postgresql://woody/ps180437", "ps180437", "qsdnbvc");
			System.out.println("Connexion établie");
			st = conn.createStatement();
			pStat = conn.prepareStatement("select verifPass( ?, ?)");
			pShare = conn.prepareStatement("INSERT INTO share_projet values(?, ?, ?)");
			pDelete = conn.prepareStatement("DELETE FROM share_projet where login LIKE ? and path LIKE ?");
		}
		catch (SQLException e){
			e.printStackTrace();
		}
	}

	/**
	 * Permet de vérifier que les logs entrés sont corrects
	 * @param log
	 * @param pass
	 * @return
	 * @throws SQLException
	 */
	public boolean verifLog(String log, String pass) throws SQLException{
		pStat.setString(1, log);
		pStat.setString(2, pass);
		ResultSet rs = pStat.executeQuery();
		if(rs.next())
			return rs.getBoolean("verifpass");
		return false;
	}

	/**
	 * Vérifie qu'un utilisateur est propriétaire du fichier
	 * @param log
	 * @param path
	 * @return
	 */
	public boolean verifOwner(String log, String path){
		try{
			ResultSet rs = st.executeQuery("select login from share_projet where path LIKE '%"+path+"' and type LIKE 'owner'");
			if(rs.next())
				return rs.getString("login").equals(log);
		}catch(Exception e) {e.printStackTrace();}
		return false;
	}

	/**
	 * Renvoie le lien du fichier demandé
	 * @return Le lien du fichier si l'utilisateur y a accès (propriétaire ou partage)
	 * null sinon
	 */
	public String getFichier(String log, String name){
		try{
			ResultSet rs = st.executeQuery("select path from share_projet where login LIKE '"+ log +"' and path LIKE '%"+ name +"'");
			if (rs.next())
				return rs.getString("path");
		}catch(Exception e){e.printStackTrace();}
		return null;
	}

	public boolean ajouterPartage(String log, String path, String type) {
		try {
			pShare.setString(1, log);
			pShare.setString(2, path);
			pShare.setString(3, type);
			pShare.executeUpdate();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Renvoie tous les fichiers possédez par l'utilisateur avec le nom des clients partagés
	 * @param log
	 * @return
	 */
	public HashMap<String, ArrayList<String>> getPartages(String log){
		HashMap<String, ArrayList<String>> retour = new HashMap<>();
		for(String s : getFichiers(log)){
			if(!verifOwner(log, s)){
				continue;
			}
			retour.put(s.substring(s.indexOf("/")+1), new ArrayList<String>());
			//Enlève la partie "répertoire" du nom du fichier
			try{
				ResultSet rs = st.executeQuery("select login from share_projet where path LIKE '%"+s+"' and type like 'shared'");
				while(rs.next()){
					retour.get(s.substring(s.indexOf("/") + 1)).add(rs.getString("login"));
				}
			}catch(Exception e){e.printStackTrace();}
		}
		return retour;
	}

	public void changePasswd(String log, String pass) throws SQLException{
		st.executeUpdate("UPDATE login_projet SET passwd = '" + pass + "' where login LIKE '" + log + "'");
	}

	public boolean creerCompte(String log, String pass){
		try{
			st.executeUpdate("INSERT INTO login_projet values('" + log + "', '" + pass + "')");
			return true;
		}catch(Exception e){return false;}
	}

	public boolean supprimerFichier(String log, String fileName){
		try{
			pDelete.setString(1, log);
			pDelete.setString(2, "%"+fileName);
			pDelete.executeUpdate();
			return true;
		}catch(Exception e) {e.printStackTrace();}
		return false;
	}

	/**
	 * @return tous les fichiers accessibles à l'utilisateur
	 */
	public ArrayList<String> getFichiers(String log){
		ArrayList<String> tmp = new ArrayList<>();
		try{
			ResultSet rs = st.executeQuery("select * from share_projet where login like '"+log+"'");
			while(rs.next()){
				tmp.add(rs.getString("path"));
			}
		}catch(Exception e) {e.printStackTrace();}
		return tmp;
	}

	public boolean supprimerPartage(String log, String dest, String fichier){
		if(!verifOwner(log, fichier))
			return false;
		try{
			return st.executeUpdate("DELETE FROM share_projet where path LIKE '%"+fichier+"' and login LIKE '"+dest+"'") == 1; //retourne 1 si la suppression à réussie, 0 sinon
		}catch(Exception e){return false;}
	}
}
