import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * Classe ClientCloud
 * Se connecte au serveur par le socket
 * @author Mathieu BOIREAU, Thibault FOUCHET, Sébastien PRUNIER
 * @version 2019-12-06
 */

public class ClientCloud {
	
	private Socket serv;
	private PrintWriter out;
	private OutputStream outData;
	private BufferedReader in;
	private InputStream inData;
	private Scanner scan;
	
	public ClientCloud(String pc, int port){
		
		try{
			serv = new Socket(pc, port);
			out = new PrintWriter(serv.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(serv.getInputStream()));
			outData = serv.getOutputStream();
			inData = serv.getInputStream();
			System.out.println("Connection établie");
			scan = new Scanner(System.in);
			setConnexion();
		}catch(Exception e){System.out.println("Connexion impossible");}
	}
	
	public void setConnexion(){
		String choix;
		String ligne = null;
		do{
			System.out.println("(C)réer un compte ou (S)e connecter");
			choix = scan.next().toUpperCase();
			if(verifConnexion(choix)){
				out.println(choix);
				try{
					ligne = in.readLine();
				}catch(Exception e){e.printStackTrace();}
				if(ligne != null && ligne.equals("true")){
					if(choix.equals("S"))
						connexion();
					if(choix.equals("C"))
						signUp();
				}
				else
				System.out.println("Erreur coté serveur");
			}
			else
				System.out.println("Mauvais choix");
		}while(ligne == null || !ligne.equals("true"));
		choix();
	}
	
	private void connexion(){
		System.out.println("Inserer votre login");
		out.println(scan.next());
		System.out.println("Inserer votre mot de passe");
		out.println(scan.next());
		String rp = null;
		try{
			rp = in.readLine();
		}
		catch(Exception e){e.printStackTrace();}
		if(rp != null && rp.equals("true"))
			System.out.println("Connexion réussie");
		else{
			System.out.println("Connexion échouée");
			setConnexion();
		}
	}
	
	private void signUp(){
		System.out.println("Inserer votre login");
		out.println(scan.next());
		System.out.println("Inserer votre mot de passe");
		out.println(scan.next());
		String rp = null;
		try {
			rp = in.readLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (rp != null && rp.equals("true"))
			System.out.println("Connexion réussie");
		else {
			System.out.println("Logs incorrects");
			setConnexion();
		}
	}
	
	private void choix(){
		String choix, ligne;
		do {
			ligne = null;
			System.out.println("\n(C)onsulter les fichiers stockés");
			System.out.println("(T)élécharger un fichier");
			System.out.println("(U)ploader un fichier");
			System.out.println("(P)artager un fichier");
			System.out.println("Consulter (V)os partages");
			System.out.println("(S)upprimer un fichier");
			System.out.println("Supprimer un p(A)rtage");
			System.out.println("Changer de (M)ot de passe");
			System.out.println("(D)éconnecter");
			choix = scan.next().toUpperCase();
			if(!verifChoix(choix)){
				System.out.println("Mauvais choix");
				continue;
			}
			out.println(choix);
			try {
				ligne = in.readLine();
				if(ligne != null)
					ligne = ligne.trim(); //Evite une erreur lors de la lecture du InputStream
				if (ligne != null && ligne.equals("true")) {
					switch(choix){
						case "C" : 
							consulter();
							break;
						case "T" : 
							telecharger();
							break;
						case "U" : 
							upload();
							break;
						case "P" : 
							partager();
							break;
						case "S" :
							supprimer();
							break;
						case "M" : 
							changePass();
							break;
						case "V" : 
							consulterPartage();
							break;
						case "A" :
							supprimerPartage();
							break;
					}
				}
				else{
					System.out.println("Une erreur est survenue...");
					System.exit(1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} while (choix != null && !choix.equals("D"));
		deconnecter();
	}
	
	private boolean verifConnexion(String s){
		return (s != null && (s.equals("C") || s.equals("S")));
	}
	
	private boolean verifChoix(String s){
		return (s != null && (verifConnexion(s) || s.equals("T") || s.equals("U") || s.equals("M") || s.equals("D") || s.equals("P") || s.equals("V") || s.equals("A")));
	}
	
	/**
	 * Consulter les fichiers accessibles
	 */
	private void consulter(){
		try {
			System.out.println("\nFichiers de sur le serveur : \n");
			Thread.sleep(50);
			String ligne = "";
			while(!(ligne = in.readLine()).equals("terminé"))
				System.out.println(ligne);
			System.out.println("\nAppuyez pour continuer...");
			System.in.read(new byte[2]);
		} catch (Exception e) {}
	}

	/**
	 * Permet de consulter les fichiers que l'on possèdent et à qui on les a partagés
	 */
	private void consulterPartage() {
		try {
			System.out.println("\nFichiers partagés : \n");
			Thread.sleep(50);
			while(!in.ready()){}
			String ligne = "";
			while (!(ligne = in.readLine()).equals("terminé"))
				System.out.println(ligne);
			System.out.println("\nAppuyez pour continuer...");
			System.in.read(new byte[2]);
		} catch (Exception e) {
		}
	}
	
	/**
	 * Permet de supprimer un fichier du serveur
	 * Si on est propriétaire, supprime le fichier pour tout le monde
	 * Si on ne l'est pas, supprime notre accès
	 */
	private void supprimer(){
		System.out.println("Indiquez le nom du fichier");
		String nom = scan.next();
		out.println(nom);
		String res = "";
		try{
			res = in.readLine();
		}catch(Exception e) {e.printStackTrace();}
		if(res == null || res.equals("false"))
			System.out.println("Échec de la suppression du fichier "+nom);
		else
			System.out.println(nom+" a été supprimé");
	}
	
	private void partager(){
		System.out.println("Indiquez le login du compte"); //Nom du client cible pour le partage
		out.println(scan.next());
		System.out.println("Indiquez le nom du fichier"); //On ne peut indiquer qu'un fichier dont on est propriétaire
		out.println(scan.next());
		String res = null;
		try{
			res = in.readLine();
		}catch(Exception e) {e.printStackTrace();}
		if(res == null || res.equals("false")){
			System.out.println("Erreur lors du partage");
		}
		else
			System.out.println("Partage réussie");
	}

	/**
	 * Permet de supprimer un partage accordé à quelqu'un
	 */
	private void supprimerPartage(){
		consulterPartage();
		System.out.println("Indiquez le nom du fichier");
		out.println(scan.next());
		System.out.println("Indiquez le nom du client à supprimer");
		out.println(scan.next());
		String ligne = null;
		try{
			ligne = in.readLine();
		}catch(Exception e){System.out.println("Erreur serveur"); deconnecter();}
		if(ligne == null)
			System.out.println("Erreur serveur");
		else
			System.out.println(ligne);
		return;
	}
	
	/**
	 * Permet de téléchargé un fichier stocké sur le serveur
	 * Le fichier doit nous être accessible (Propriétaire ou par partage)
	 * @throws Exception
	 */
	private void telecharger() throws Exception{
		System.out.println("Indiquez le nom du fichier");
		String nom = scan.next();
		out.println(nom);
		String ligne = null;
		ligne = in.readLine();
		
		if(ligne != null && ligne.equals("true")){
			int taille = Integer.parseInt(in.readLine());
			byte[] mybytearray = new byte[taille];
			if(nom.indexOf("/") != -1)
				nom = nom.substring(nom.indexOf("/") + 1);
			File f = new File(System.getProperty("user.home") + "/Téléchargements/" +  nom); //Envoi chaque fichier téléchargé dans "Téléchargement"
			f.createNewFile();
			while(!in.ready()){} //Attends que le serveur envoi le fichier
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f)); //Buffer sur le fichier créé (vide)
			int bytesRead = inData.read(mybytearray, 0, mybytearray.length); //Tableau servant à enregistrer tous les octets du fichier
			int current = bytesRead;
			do {
				bytesRead = inData.read(mybytearray, current, (mybytearray.length - current));
				if (bytesRead >= 0)
				current += bytesRead;
			} while (bytesRead > 0); //Enregistre le octets du fichier jusqu'à la fin
			
			bos.write(mybytearray, 0, current);
			bos.flush(); //Ecriture des octets dans le fichier
			System.out.println("Fichier " + nom + " reçu (" + current + " octets)");
			bos.close();
		}
		else{
			System.out.println("Le fichier n'existe pas");
		}
	}	

	/**
	 * Permet d'envoyer un fichier stocké sur le pc
	 */
	private void upload(){
		System.out.println("Indiquez le nom du fichier");
		String path = System.getProperty("user.home")+"/"+scan.next();
		File file = new File(path);
		if(!file.exists()){
			System.out.println("Fichier introuvable");
			out.println("Introuvable"); //Indication au serveur que le fichier n'a pas été trouvé (Annulation de l'envoi)
			return;
		}
		
		out.println(file.length()); //Envoi de la taille du fichier
		out.println(file.getName()); //Envoi du nom du fichier
		
		byte[] mybytearray = new byte[(int) file.length()];
		try{
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
			bis.read(mybytearray, 0, mybytearray.length);
			System.out.println("Envoi de " + file.getName() + "(" + mybytearray.length + " octets)");
			Thread.sleep(500);
			outData.write(mybytearray, 0, mybytearray.length);
			outData.flush();
			outData.write(0); //Correction de bug : évite que le serveur se mette en "standby" jusqu'à ce que le client intéragisse de nouveau
			outData.flush();
			System.out.println("Fichier envoyé.");
			bis.close();
		}catch(Exception e) {e.printStackTrace();}
	}
	
	private void changePass(){
		System.out.println("Entrez le mot de passe actuel"); //Vérifie que c'eset le bon utilisateur changeant le mot de passe
		out.println(scan.next());
		try{
			if(!in.readLine().equals("true")){
				System.out.println("Mauvais mot de passe");
				return;
			}
		}catch(Exception e){}
		
		System.out.println("Entrez un nouveau mot de passe");
		out.println(scan.next());
		String res = null;
		try{
			res = in.readLine();
		}catch(Exception e) {e.printStackTrace();}
		if(res == null || res.equals("false"))
			System.out.println("Échec du changement de mdp");
		else
			System.out.println("Changement réussie");
	}
	
	public void deconnecter(){
		System.out.println("\nDéconnexion");
		try{
			in.close();
			out.close();
			serv.close();
		}catch(Exception e){}
		System.exit(0);
	}
	
	public static void main(String[] args) {
		new ClientCloud(args[0], Integer.parseInt(args[1]));
	}
}
