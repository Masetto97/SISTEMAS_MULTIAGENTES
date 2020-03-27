package com.scraping.servidor;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.Subscription;
import jade.proto.SubscriptionResponder.SubscriptionManager;

import com.scraping.domain.Provincia;
import com.scraping.domain.Tiempo;
import com.scraping.persistencia.Scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class AgenteMeteosat extends Agent{
	
	//Se crea una tabla indexada donde guardaremos las suscripciones realizadas.
    private Set<Subscription> suscripciones = new HashSet<Subscription>();
    private List<Provincia> l_provincias;
    private Scraper scraping;
 
    protected void setup() {
        System.out.println(this.getLocalName() + ": Esperando suscripciones...");
        
        scraping = new Scraper("https://www.meteosat.com/");
        cargarProvincias();
        MessageTemplate template = SubscriptionResponder.createMessageTemplate(ACLMessage.SUBSCRIBE);
 
        this.addBehaviour(new EnviarSemanal(this, (long) 10000));
        SubscriptionManager gestor = new SubscriptionManager() {
 
            public boolean register(Subscription suscripcion) {
                suscripciones.add(suscripcion);
                return true;
            }
            public boolean deregister(Subscription suscripcion) {
                suscripciones.remove(suscripcion);
                return true;
            }
        };
        this.addBehaviour(new HacerSuscripcion(this, template, gestor));
    }
    private boolean compruebaMensaje(Object p_solicitada) {
    	boolean isContain = false;
    	try {
    		//System.out.println(AgenteMeteosat.this.getLocalName()+":\n\tComprobando ciudad: "+p_solicitada.toString());
    		isContain = l_provincias.contains(p_solicitada);
    	}catch(Exception e) {
    		System.out.println(AgenteMeteosat.this.getLocalName()+":\n\tError-->"+e.getMessage());
    	}
    	return isContain;
    }
    private void cargarProvincias() {
    	l_provincias = new ArrayList<>();
    	// Compruebo si el protocolo http es 200
        if (scraping.getStatusConnectionCode() == 200) {
			Document document = scraping.getHtmlDocument();// Obtengo el HTML de la web en un objeto Document
			Elements l_tablas = document.select("div.clearfix > ul.left");// Busco todas las entradas que estan dentro de: "div.clearfix > ul.left"
            Elements li =l_tablas.select("a[href]");//lista provincias
    		for(Element e : li) {
    			//El substring(14) se usa para eliminar de la subcadena "El tiempo en " y quedarnos solo con el nombre de la provincia
    			l_provincias.add(new Provincia(e.attr("href"),e.attr("title").substring(14)));
    		}
        }else
            System.out.println("El Status Code no es OK es: "+scraping.getStatusConnectionCode());
    }
    private Tiempo cargarTiempoProvincia(Provincia p) {
    	scraping.setUrlSubPage(p.getHref());
		// Compruebo si el protocolo http es 200
    	Tiempo t_provincia;
        if (scraping.getStatusConnectionCode() == 200) {
			Document document = scraping.getHtmlDocument();// Obtengo el HTML de la web en un objeto Document
			//Elements l_tablas = document.select("div.prevision-local");// Busco todas las entradas que estan dentro de: "div.prevision-local"
			Elements filas = document.select("tbody > tr");// Busco todas las entradas que estan dentro de: "tbody > tr"
			Calendar c=Calendar.getInstance();
			Element tiempo_prov=null;
			int i=0,hora = c.get(Calendar.HOUR_OF_DAY),minuto = c.get(Calendar.MINUTE),
					temperatura,v_viento,precipitacion;
			String clima, d_viento;
			//System.out.println(hora+":"+minuto); 
			if(minuto>49)hora++;//se considera que para el minuto 50 interesa más la información de la siguiete hora
			while (i<filas.size()) {
				tiempo_prov = filas.get(i);
				i++;
				if (tiempo_prov.selectFirst("th:contains("+hora+" h)")!=null) {
					i=filas.size();
				}
			}
			clima = tiempo_prov.child(2).child(0).attr("alt");
			temperatura = Integer.parseInt(tiempo_prov.child(3).ownText().substring(0, 1));
			d_viento = tiempo_prov.child(4).ownText();
			v_viento = Integer.parseInt(tiempo_prov.child(5).ownText().substring(0, 1));
			precipitacion = Integer.parseInt(tiempo_prov.child(6).ownText().substring(0, 1));
			t_provincia = new Tiempo(hora, precipitacion, temperatura, v_viento, clima, d_viento);
		}else {
            System.out.println("El Status Code no es OK es: "+scraping.getStatusConnectionCode());
            t_provincia = null;
		}
        return t_provincia;
	}
    private class EnviarSemanal extends TickerBehaviour {
        public EnviarSemanal(Agent agente, long tiempo) {
            super(agente, tiempo);
        }
 
        public void onTick() {
            //Se crea y rellena el mensaje con la información que desea enviar.
            ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
            Provincia p_solicitada;
            //Se envía un mensaje a cada suscriptor
            for (Subscription suscripcion:AgenteMeteosat.this.suscripciones) {
            	try {
					p_solicitada = l_provincias.get(l_provincias.lastIndexOf((Object)suscripcion.getMessage().getContentObject()));
					mensaje.setContentObject(cargarTiempoProvincia(p_solicitada));
					suscripcion.notify(mensaje);
				} catch (UnreadableException e) {
					System.out.println(AgenteMeteosat.this.getLocalName()+":\n\tERROR--->"+e.getMessage());
				}catch (IOException e) {
					System.out.println(AgenteMeteosat.this.getLocalName()+":\n\tERROR--->"+e.getMessage());
				}
            }
        }
    }
 
    private class HacerSuscripcion extends SubscriptionResponder {
        private Subscription suscripcion;
 
        public HacerSuscripcion(Agent agente, MessageTemplate plantilla, SubscriptionManager gestor) {
            super(agente, plantilla, gestor);
        }
 
        //Método que maneja la suscripcion
 
        protected ACLMessage handleSubscription(ACLMessage propuesta)
                throws NotUnderstoodException {
            System.out.printf("%s: \n\tSUSCRIBE recibido de %s.\n",
            		AgenteMeteosat.this.getLocalName(), propuesta.getSender().getLocalName());
            try {
				System.out.printf("%s: \n\tLa propuesta es: %s.\n",
						AgenteMeteosat.this.getLocalName(), propuesta.getContentObject());
			} catch (UnreadableException e1) {
				System.out.println(AgenteMeteosat.this.getLocalName()+"\n\tERROR--->"+e1.getMessage());
			}
            
          //De primeras suponemos que se rechaza la peticion
		    ACLMessage confirmacion = propuesta.createReply();
		    confirmacion.setPerformative(ACLMessage.REFUSE);
            
            //Comprueba los datos de la propuesta
            try {
				if (AgenteMeteosat.this.compruebaMensaje(propuesta.getContentObject())) {
 
				    //Crea la suscripcion
				    this.suscripcion = this.createSubscription(propuesta);
 
				    try {
				        //El SubscriptionManager registra la suscripcion
				        this.mySubscriptionManager.register(suscripcion);
				        //Acepta la propuesta y la envía
					    confirmacion.setPerformative(ACLMessage.AGREE);
				    } catch (Exception e) {
				        System.out.println(AgenteMeteosat.this.getLocalName() + ": Error en el registro de la suscripción.");
				    }
				}
			} catch (UnreadableException e) {
				System.out.println(AgenteMeteosat.this.getLocalName()+"\n\tERROR-->"+e.getMessage());
			}
            return confirmacion;
        }
 
        //Maneja la cancelación de la suscripcion
 
        protected ACLMessage handleCancel(ACLMessage cancelacion) {
            System.out.printf("%s: CANCEL recibido de %s.\n",
            		AgenteMeteosat.this.getLocalName(), cancelacion.getSender().getLocalName());
            try {
                //El SubscriptionManager elimina del registro la suscripcion
                this.mySubscriptionManager.deregister(this.suscripcion);
            } catch (Exception e) {
                System.out.println(AgenteMeteosat.this.getLocalName() + ": Error en el desregistro de la suscripción.");
            }
            //Acepta la cancelación y responde
            ACLMessage cancela = cancelacion.createReply();
            cancela.setPerformative(ACLMessage.INFORM);
            return cancela;
        }
    }
}
