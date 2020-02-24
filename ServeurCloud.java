import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Classe ServeurCloud
 * Créer les GerantDeClient et interragit avec la base de donnée
 * @author Mathieu BOIREAU, Thibault FOUCHET, Sébastien PRUNIER
 * @version 2019-12-06
 */

public class ServeurCloud {
	private ServerSocket ss;
	private AccesBase db;
	private HashMap<GerantDeClient, Thread> hashThread;

	public ServeurCloud(/*String log, String pass*/) {
		try {
			db = new AccesBase(/*log, pass*/);
			ss = new ServerSocket(8000);
			hashThread = new HashMap<>();
			while (true) { //A chaque nouvelle connexion, un Thread est créé
				Socket s = ss.accept();
				GerantDeClient gdc = new GerantDeClient(s, this);
				Thread thread = new Thread(gdc);
				hashThread.put(gdc, thread);
				thread.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<String> getFichiers(String log){
		return db.getFichiers(log);
	}

	public HashMap<String, ArrayList<String>> getPartages(String log){
		return db.getPartages(log);
	}

	public boolean supprimerPartage(String log, String dest, String fichier){
		return db.supprimerPartage(log, dest, fichier);
	}

	/**
	 * Vérifie si le client possède ce fichier ou si ce fichier lui est partagé
	 * @param log
	 * @param name
	 * @return le fichier demandé s'il y a accès, null sinon
	 */
	public File getFichier(String log, String name){
		String s;
		if(name == null || (s = db.getFichier(log, name)) == null){
			return null;
		}
		return new File("./stocks/" + s);
	}

	/**
	 * Permet de créer un compte
	 * Connecte si la création est réussi
	 * La création peut échouer si un compte avec ce login existe déjà, ou si le login possède un '/'
	 * @param log
	 * @param pwd
	 * @return
	 */
	public boolean creerCompte(String log, String pwd) {
		if(db.creerCompte(log, pwd)){
			File f = new File("./stocks/" + log);
			f.mkdir();
			f = new File("./stocks/" + log + "/gitoui.data");
			//On créé un dossier pour les fichiers du nouveau client
			//On ajoute tout de suite un fichier vide, inaccessible car non présent dans la base de donnée
			//Ce fichier ne sert qu'à Github, pour qu'il soit envoyé
			try{
				f.createNewFile();
			}catch(Exception e){}
			return true;
		}
		
		return false;
	}

	public boolean connexion(String log, String pwd) {
		try {
			return db.verifLog(log, pwd);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Ajoute le fichier uploadé à la base de donnée
	 * @param login
	 * @param name
	 */
	public void ajouterFichier(String login, String name){
		db.ajouterPartage(login, login + "/" + name, "owner");
	}

	public boolean chmtMDP(String log, String pwd){
		if(pwd == null) return false;
		try{
			db.changePasswd(log, pwd);
			System.out.println("Changement du mot de passe de " + log);
			return true;
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Renvoie le chemin d'accès aux fichiers du client
	 * @param log
	 * @return
	 */
	public String getRepertoire(String log){
		return "stocks/" + log + "/";
	}

	/**
	 * Ajoute un partage du fichier path au client dest uniquement si le client log est propriétaire du fichier
	 * @param log
	 * @param dest
	 * @param path
	 * @return
	 */
	public boolean ajouterPartage(String log, String dest, String path){
		if(!db.verifOwner(log, log + "/" + path))
			return false;
		return db.ajouterPartage(dest, log + "/" +  path, "shared");
	}



	/**
	 * Permet de supprimer un fichier
	 * Si le fichier est partagé, supprime le partage
	 * Si le client possède le fichier, supprime le fichier pour lui et tous les autres clients partagés
	 * @param login
	 * @param fileName
	 * @return
	 */
	public boolean supprimerFichier(String login, String fileName){
		boolean bOk = db.supprimerFichier(login, fileName);
		if(!bOk)
			return false;
		File f = new File("stocks/" + login + "/" + fileName);
		if(!f.exists())
			return false;
		f.delete();
		System.out.println("Suppression du fichier " + fileName + " de " + login);
		return true;
	}

	/**
	 * Interrompt un thread est le supprime de la liste
	 * @param g
	 */
	public void deconnecter(GerantDeClient g){
		if(!hashThread.containsKey(g))
			return;
		hashThread.get(g).interrupt();
		hashThread.remove(g);
	}

	public static void main(String[] args) {
		/*if(args.length != 2){
			System.out.println("Usage : java ServeurCloud loginPSQL passwdPSQL");
			System.exit(1);
		}*/
		new ServeurCloud(/*args[0], args[1]*/);
	}
}