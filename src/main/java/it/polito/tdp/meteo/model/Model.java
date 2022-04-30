package it.polito.tdp.meteo.model;

import java.text.DateFormat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.tdp.meteo.DAO.MeteoDAO;

public class Model {
	
	private final static int COST = 100;
	private final static int NUMERO_GIORNI_CITTA_CONSECUTIVI_MIN = 3;
	private final static int NUMERO_GIORNI_CITTA_MAX = 6;
	private final static int NUMERO_GIORNI_TOTALI = 15;

	private MeteoDAO md;
	private Map<String,Citta> citta;
	private List<List<String>> sequenze;
	
	public Model() {
		md = new MeteoDAO();
		citta = new HashMap<String,Citta>();
		sequenze = new ArrayList<List<String>>();
		
		for(Rilevamento r : md.getAllRilevamenti()) {
			
			if(!citta.containsKey(r.getLocalita())) {
				Citta c = new Citta(r.getLocalita());
				c.getRilevamenti().add(r);
				citta.put(r.getLocalita(), c);
				
			} else {
				citta.get(r.getLocalita()).getRilevamenti().add(r);
			}
			
		}
		
	}

	// of course you can change the String output with what you think works best
	public String getUmiditaMedia(int mese) {
		
		String output = "";
		
		for(String s : citta.keySet()) {
			double sommaTot = 0;
			
			for(Rilevamento r : md.getAllRilevamentiLocalitaMese(mese, s)) {
				sommaTot += r.getUmidita();
			}
			
			output += s + " " + sommaTot/md.getAllRilevamentiLocalitaMese(mese, s).size() + "\n";
		}
			
		
		
		return output;
	}
	
	// of course you can change the String output with what you think works best
	public List<String> trovaSequenza(int mese) {
		
		List<String> parziale = new ArrayList<String>();
		
		trovaSequenzaRicorsiva(0, parziale);
		
		return sequenzaOttima(mese);
	}
	
	private void trovaSequenzaRicorsiva(int livello, List<String> parziale) {
		
		if(livello==NUMERO_GIORNI_TOTALI) { //Condizione terminale --> sono passati 15 gg
			sequenze.add(new ArrayList<String>(parziale));
			return;
		}
		
		for(int j=0; j<3; j++) {
			
			if(j==0) {
				//Inseriamo Torino
				if(controlloInserimento(parziale,"Torino")) {
					parziale.add("Torino");
					trovaSequenzaRicorsiva(livello+1, parziale);
					parziale.remove(parziale.size()-1);//Backtracking
				}
				
			} else if(j==1) {
				//Inseriamo Genova
				if(controlloInserimento(parziale,"Genova")) {
					parziale.add("Genova");
					trovaSequenzaRicorsiva(livello+1, parziale);
					parziale.remove(parziale.size()-1);//Backtracking
				}
				
			} else {
				//Inseriamo Milano
				if(controlloInserimento(parziale,"Milano")) {
					parziale.add("Milano");
					trovaSequenzaRicorsiva(livello+1, parziale);
					parziale.remove(parziale.size()-1);//Backtracking
				}
			}
		}
	}

	
	/*
	 * Procedimento logico:
	 * 
	 * Troviamo nel metodo ricorsivo tutte le possibili sequenze,
	 * filtrando opportunamente per ridurre la complessita da 15! 
	 * a qualcosa di meno.
	 * Quindi calcoliamo il costo di ognuna con e selezioniamo il costo minore
	 */
	
	
	private boolean controlloInserimento(List<String> parziale, String cittaCandidata) {
		//Ritorna vero se posso inserire la citta in parziale, falso altrimenti
		
		//Primo controllo: vediamo se la citta compare piu di 6 volte
		int count = 0;
		
		for(String s : parziale) {
			if(s.equals(cittaCandidata)) {
				count++;
			}
		}
		
		if(count>=NUMERO_GIORNI_CITTA_MAX) {
			return false;
		}
		
		//Secondo controllo: vediamo se la citta interrompe una sequenza di 1 o 2 citta uguali
		if(parziale.size()==0) {
			return true; //Se parziale è vuoto posso inserire qualsiasi citta
		}
		
		if(parziale.size()==1 || parziale.size()==2) {
			//Se parziale contiene una o due citta devo inserire la stessa citta
			//che è registrata in ultima posizione
			if(cittaCandidata.equals(parziale.get(parziale.size()-1))) {
				return true;
			}
			return false;
		}
		
		if(parziale.get(parziale.size()-1).equals(parziale.get(parziale.size()-2))
				&& parziale.get(parziale.size()-2).equals(parziale.get(parziale.size()-3))) {
			//Se le ultime tre citta inserite sono uguali allora posso inserire la citta
			return true;
		}
		
		if(parziale.get(parziale.size()-1).equals(parziale.get(parziale.size()-2))
				&& cittaCandidata.equals(parziale.get(parziale.size()-1))) {
			//Se le ultime due citta inserite sono uguali tra loro e anche uguali alla citta candidata
			//allora posso inserire la citta
			return true;
		}
		
		if(cittaCandidata.equals(parziale.get(parziale.size()-1))) {
			//Se l'ultima citta inserita è uguale alla citta candidata allora posso inserire la citta
			return true;
		}
		
		//Se arrivo qua allora sto interrompendo una sequenza di 1 o 2 e non posso stare nella citta
		return false;
	}
	
	private List<String> sequenzaOttima(int mese) {
		List<String> ottima = null;
		double costoOttimo = 0;
		int count = 0;
		
		for(List<String> lista : sequenze) {
			
			double costo = 0;
			
			for(int i=0; i<NUMERO_GIORNI_TOTALI; i++) {
				//Prendo tutti i rilevamenti in ordine di data del mese nella citta alla posizione i
				//e sommo l'umidita dei primi 15gg
				List<Rilevamento> rilevamenti = md.getAllRilevamentiLocalitaMese(mese, lista.get(i));
				costo += rilevamenti.get(i).getUmidita();	
			}
			
			for(int i=3; i<NUMERO_GIORNI_TOTALI; i++) {
				//Conto quante volte il tecnico si è spostato
				if(!lista.get(i).equals(lista.get(i-1))) {
					costo += COST;
				}
			}
			
			if(costo<costoOttimo || count==0) {
				costoOttimo=costo;
				ottima = new ArrayList<String>(lista);
				count++;
			}
			
		}
		
		return ottima;
	}
}
