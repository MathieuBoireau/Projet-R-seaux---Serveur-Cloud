import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
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

/**
 * Classe GerantDeClient
 * S'occupe d'interragir entre le client et le serveur
 * @author Mathieu BOIREAU, Thibault FOUCHET, Sébastien PRUNIER
 * @version 2019-12-06
 */

public class GerantDeClient implements Runnable{
	private PrintWriter    out;
	private OutputStream outData;
	private BufferedReader in;
	private InputStream inData;
	private ServeurCloud serv;
	private String login;
	
	public GerantDeClient(Socket socket, ServeurCloud serv) {
		try{
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			outData = socket.getOutputStream();
			inData = socket.getInputStream();
			this.serv = serv;
			login = null;
		}catch(Exception e){ e.printStackTrace(); }
	}
	
	public void run(){
		String ligne = null;
		boolean estValide = false;
		try{
			do{
				ligne = in.readLine();
				
				String log;
				String pwd;
				out.println("true");
				log = in.readLine();
				pwd = in.readLine();
				if(log == null || pwd == null)
					continue;
				if(ligne.equals("C"))
					estValide = serv.creerCompte(log,pwd);
				else if(ligne.equals("S"))
					estValide = serv.connexion(log,pwd);
				
				out.println(String.valueOf(estValide));
				if(estValide){
					System.out.println("Connexion de " + log);
					login = log;
				}
				
			}while(!estValide);
			choixClient();
		}catch(Exception e){ e.printStackTrace(); }
	}
	
	public void choixClient(){
		String choix = null;
		do{
			try{
				choix = in.readLine(); //Evite une insertion d'un espace par erreur lors de la lecture du InputStream
				choix = choix.trim();
				switch(choix.charAt(0)){
					case 'C' :
						consulterFichiersServeur();
						break;
					case 'T' :
						telechargement();
						break;
					case 'U' :
						upload();
						break;
					case 'P' :
						partager();
						break;
					case 'S' :
						supprimer();
						break;
					case 'M' :
						changePass();
						break;
					case 'V' :
						consulterPartage();
						break;
					case 'A' :
						supprimerPartage();
						break;
				}
				if(choix.equals("D"))
					break;
			}catch(Exception e){ choix = null; }
		}while(choix!=null && !choix.equals("D"));
		deconnection();
	}
	
	/**
	 * Permet de partager un fichier à un autre client
	 */
	private void partager(){
		out.println("true"); //A chaque fois qu'on commence un méthode, on envoi "true" au client afin d'indiquer que le serveur est prêt
		try{
			String log = in.readLine(); //Login de la cible du partage
			String path = in.readLine(); //Nom du fichier
			if(serv.getFichier(login, path) == null){
				out.println("false");
				return;
			}
			out.println((serv.ajouterPartage(login, log, path)? "true" : "false"));
		}catch(Exception e) {e.printStackTrace();}
	}

	private void supprimerPartage(){
		consulterPartage();
		String fichier = null;
		String dest = null;
		try{
			fichier = in.readLine();
			dest = in.readLine();
		}catch(Exception e){}
		if(fichier == null || dest == null){
			deconnection(); //Si l'un des fichier est null c'est qu'il y a une erreur de connexion
		}
		if(serv.supprimerPartage(login, dest, fichier))
			out.println("Suppression réussie");
		else
			out.println("Echec de la suppression, fichier inexistant, client inexistant ou vous n'avez pas les droits");
	}
	
	public void consulterFichiersServeur(){
		out.println("true");
		for(String name : serv.getFichiers(login)){
			if(name.substring(0, name.indexOf("/")).equals(login))
				out.println(name.substring(name.indexOf("/") + 1)); //Enleve le log du client du nom du fichier s'il en est le propriétaire
			else
				out.println(name);
		}
		out.println("terminé");
	}

	public void consulterPartage(){
		out.println("true");
		HashMap<String, ArrayList<String>> tmp = serv.getPartages(login);
		for(String s : tmp.keySet()){
			out.println(s + " :");
			for(String log : tmp.get(s)){
				out.println("\t"+log);
			}
		}
		out.println("terminé");
	}

	/**
	 * Supprime un fichier accessible
	 */
	public void supprimer(){
		out.println("true");

		try{
			String fileName = in.readLine();
			out.println((serv.supprimerFichier(login, fileName))?"true":"false");
		}catch(Exception e){ e.printStackTrace(); }
	}

	public void changePass(){
		out.println("true");
		try{
			String mdp = in.readLine();
			out.println(serv.connexion(login, mdp));
			if(!serv.connexion(login, mdp))
				return;
			String nvMDP = in.readLine();
			out.println((this.serv.chmtMDP(login, nvMDP))?"true" : "false");
		}
		catch(Exception e){e.printStackTrace(); out.println("false");}
	}
	
	private void telechargement(){
		out.println("true");
		String name;
		try{
			name = in.readLine();
		}catch(Exception e) {name = null;}
		File f = serv.getFichier(login, name);
		if(f == null || !f.exists()){ //Vérifie que le fichier existe
			out.println("false");
			return;
		}
		out.println("true");
		out.println(f.length());
		byte[] mybytearray = new byte[(int) f.length()];
		try{
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
			bis.read(mybytearray, 0, mybytearray.length);
			System.out.println("Envoi du fichier " + name + " à " + login + "(" + mybytearray.length + " octets)");
			outData.write(mybytearray, 0, mybytearray.length);
			outData.flush();
			outData.write(0); //Permet d'éviter que le client se mette en attente à la réception du fichier
			outData.flush();
			System.out.println("Envoyé.");
			bis.close();
		}catch(Exception e) {e.printStackTrace();}
	}
	
	private void upload(){
		out.println("true");
		String name;
		int taille;
		try{
			String sExists = in.readLine();
			if(sExists.equals("Introuvable"))
				return;
			taille = Integer.parseInt(sExists);
			name = in.readLine();
		}catch(Exception e){
			taille = -1;
			name = null;
		}
		
		byte[] mybytearray = new byte[taille];
		File f = new File(serv.getRepertoire(login)+name);
		try{
			f.createNewFile();
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
			
			int bytesRead = inData.read(mybytearray, 0, mybytearray.length);
			int current = bytesRead;
			while(!in.ready()){}
			do {
				bytesRead = inData.read(mybytearray, current, (mybytearray.length - current));
				if (bytesRead >= 0)
				current += bytesRead;
			} while (bytesRead > 0);
			
			bos.write(mybytearray, 0, current);
			bos.flush();
			System.out.println("Fichier " + name + " reçu de la part de " + login + " (" + current + " octets)");
			bos.close();
			serv.ajouterFichier(login, name);
			System.out.println("le fichier est ajouté");
		}catch(Exception e){ e.printStackTrace(); deconnection();}
	}
	
	public void deconnection(){
		out.println("true");
		try{
			System.out.println("Déconnexion de " + login);
			in.close();
			out.close();
		}catch(Exception e) {}
		serv.deconnecter(this);
	}
}
