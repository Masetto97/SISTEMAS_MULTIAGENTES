package com.scraping.persistencia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

public class Scraper {

	public String url_inicio;
	public String url_subpage;
	public Scraper (String url) {
		this.url_inicio = url;
		this.url_subpage="";
	}
	///////////////
	////Get-Set////
	///////////////
	public String getUrlIncio() {
		return url_inicio;
	}
	public String getUrlSubPage() {
		return url_subpage;
	}
	public void setUrlInicio(String url) {
		this.url_inicio=url;
	}
	public void setUrlSubPage(String url) {
		this.url_subpage=url;
	}
	////////////////////////
	////Metodos publicos////
	////////////////////////
	public List<Node> cleanList(List<Node> nodo){
    	List<Node> lista=new ArrayList<>();
    	for(Node aux : nodo)
    		if(!aux.toString().equals(" "))
    			lista.add(aux);
    	return lista;
    }
	/**
	 * Con este método se comprueba el Status code de la respuesta que se recibe al hacer la petición
	 * EJM:
	 * 		200 OK					300 Multiple Choices
	 * 		301 Moved Permanently	305 Use Proxy
	 * 		400 Bad Request			403 Forbidden
	 * 		404 Not Found			500 Internal Server Error
	 * 		502 Bad Gateway			503 Service Unavailable
	 * @return Status Code
	 */
	public int getStatusConnectionCode() {
			
	    Response response = null;
	    try {
	    	response = Jsoup.connect(url_inicio+url_subpage).userAgent("Mozilla/5.0").timeout(100000).ignoreHttpErrors(true).execute();
	    } catch (IOException ex) {
	    	System.out.println("Excepción al obtener el Status Code: " + ex.getMessage());
	    }
	    return response.statusCode();
	}
	/**
     * Este método devuelve un objeto de la clase Document con el contenido del
     * HTML de la web que me permitirá parsearlo con los métodos de la libreria JSoup
     * @return Documento con el HTML
     */
    public Document getHtmlDocument() {

        Document doc = null;

        try {
            doc = Jsoup.connect(url_inicio+url_subpage).userAgent("Mozilla/5.0").timeout(100000).get();
        } catch (IOException ex) {
            System.out.println("Excepción al obtener el HTML de la página" + ex.getMessage());
        }

        return doc;

    }
}
